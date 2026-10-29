package app.revanced.patches.fourpda

import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21t
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val Q0_CLASS = "Lru/fourpda/client/Q0;"
private const val Q0_S_CLASS = "Lru/fourpda/client/Q0\$s;"
private const val Y_CLASS = "Lru/fourpda/client/y;"

private fun Instruction.calledMethod(): MethodReference? =
    (this as? ReferenceInstruction)?.reference as? MethodReference

private fun Instruction.field(): FieldReference? =
    (this as? ReferenceInstruction)?.reference as? FieldReference

val fixArticleCommentsPatch = bytecodePatch(
    name = "Ремонт комментариев и опросов в статьях",
    description = "Исправляет отображение комментариев в статьях (предотвращает обрыв списка при удаленных комментариях и восстанавливает древовидность ответов) и включает отображение результатов закрытых опросов",
) {
    compatibleWith("ru.fourpda.client"("1.9.44_pre7"))

    apply {
        val q0Class = classDefs[Q0_CLASS] ?: error("в apk нет Q0")
        val parseMethod = proxy(q0Class).mutableClass.methods.firstOrNull { method ->
            method.name == "H" && method.returnType == "Z"
        } ?: error("не найден метод Q0.H")

        val instructions = parseMethod.implementation!!.instructions

        // 1. Находим вызов sa[13] -> this.k.l(13)
        var l13Index = -1
        for (i in 0 until instructions.size - 2) {
            val inst = instructions[i]
            if (inst.opcode == Opcode.INVOKE_VIRTUAL) {
                val methodRef = inst.calledMethod()
                if (methodRef?.name == "l" && methodRef.definingClass == Y_CLASS) {
                    val prev = instructions.getOrNull(i - 1)
                    val lit = (prev as? NarrowLiteralInstruction)?.narrowLiteral
                    if (lit == 13 || lit == 0xd) {
                        l13Index = i
                        break
                    }
                }
            }
        }
        if (l13Index < 0) {
            error("Не удалось найти вызов k.l(13) в Q0.H()")
        }

        // 2. Находим переход loopNext (if-nez, ведущий к концу итерации цикла комментариев: add-int/lit8 v13, v13, 1)
        var loopNextBranch: BuilderInstruction21t? = null
        for (i in l13Index until minOf(l13Index + 250, instructions.size)) {
            val inst = instructions[i]
            if (inst.opcode == Opcode.IF_NEZ) {
                val branch = inst as? BuilderInstruction21t ?: continue
                val target = branch.target.location.instruction
                if (target?.opcode == Opcode.ADD_INT_LIT8) {
                    loopNextBranch = branch
                    break
                }
            }
        }
        if (loopNextBranch == null) {
            error("Не удалось найти переход к следующей итерации комментариев в Q0.H()")
        }

        // 3. Находим строку комментария: invoke-virtual {v0, v13}, y->l(I) -> move-result-object v14
        var rowFetchIndex = -1
        for (i in l13Index until minOf(l13Index + 50, instructions.size)) {
            val inst = instructions[i]
            if (inst.opcode == Opcode.INVOKE_VIRTUAL && inst.calledMethod()?.name == "l" && inst.calledMethod()?.definingClass == Y_CLASS) {
                val next = instructions.getOrNull(i + 1)
                if (next?.opcode == Opcode.MOVE_RESULT_OBJECT) {
                    rowFetchIndex = i
                    break
                }
            }
        }
        if (rowFetchIndex < 0) {
            error("Не удалось найти выборку строки комментария v14 в Q0.H()")
        }

        // 4. Починка опросов: если опрос закрыт ((poll.c & 1) == 0), выставляем бит 4 (poll.c |= 4),
        // чтобы показывать результаты опроса вместо заблокированных радиокнопок.
        val pollFlagsWriteIndex = instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.IPUT &&
                instruction.field()?.definingClass == Q0_S_CLASS &&
                instruction.field()?.name == "c"
        }
        if (pollFlagsWriteIndex >= 0) {
            parseMethod.replaceInstruction(
                pollFlagsWriteIndex,
                """
                and-int/lit8 v14, v13, 0x1
                if-nez v14, :cond_can_vote
                or-int/lit8 v13, v13, 0x4
                :cond_can_vote
                iput v13, v12, $Q0_S_CLASS->c:I
                """.trimIndent(),
            )
        }

        // 5. Защита от NullPointerException при парсинге комментариев.
        // Выполняем замены от наибольшего индекса к наименьшему (в нисходящем порядке),
        // чтобы смещения инструкций не сдвигались.

        // Offset +61: аватар move-result-object v14
        parseMethod.replaceInstruction(
            rowFetchIndex + 61,
            """
            move-result-object v14
            if-nez v14, :cond_avatar_ok
            const-string v14, ""
            :cond_avatar_ok
            """.trimIndent(),
        )

        // Offset +57: move-result v15 (карма)
        parseMethod.replaceInstruction(rowFetchIndex + 57, "nop")

        // Offset +56: invoke-virtual {v15}, Integer->intValue() (карма)
        parseMethod.replaceInstruction(
            rowFetchIndex + 56,
            """
            if-nez v15, :cond_karma_null
            const/4 v15, 0x0
            goto :cond_karma_done
            :cond_karma_null
            invoke-virtual {v15}, Ljava/lang/Integer;->intValue()I
            move-result v15
            :cond_karma_done
            """.trimIndent(),
        )

        // Offset +42: текст move-result-object v15
        parseMethod.replaceInstruction(
            rowFetchIndex + 42,
            """
            move-result-object v15
            if-nez v15, :cond_text_ok
            const-string v15, ""
            :cond_text_ok
            """.trimIndent(),
        )

        // Offset +36: move-result v15 (таймштамп)
        parseMethod.replaceInstruction(rowFetchIndex + 36, "nop")

        // Offset +35: invoke-virtual {v15}, Integer->intValue() (таймштамп)
        parseMethod.replaceInstruction(
            rowFetchIndex + 35,
            """
            if-nez v15, :cond_ts_null
            const/4 v15, 0x0
            goto :cond_ts_done
            :cond_ts_null
            invoke-virtual {v15}, Ljava/lang/Integer;->intValue()I
            move-result v15
            :cond_ts_done
            """.trimIndent(),
        )

        // Offset +29: автор move-result-object v15
        parseMethod.replaceInstruction(
            rowFetchIndex + 29,
            """
            move-result-object v15
            if-nez v15, :cond_author_ok
            const-string v15, ""
            :cond_author_ok
            """.trimIndent(),
        )

        // Offset +23: move-result v7 (parentId)
        parseMethod.replaceInstruction(rowFetchIndex + 23, "nop")

        // Offset +22: invoke-virtual {v7}, Integer->intValue() (parentId)
        parseMethod.replaceInstruction(
            rowFetchIndex + 22,
            """
            if-nez v7, :cond_parent_null
            const/4 v7, 0x0
            goto :cond_parent_done
            :cond_parent_null
            invoke-virtual {v7}, Ljava/lang/Integer;->intValue()I
            move-result v7
            :cond_parent_done
            """.trimIndent(),
        )

        // Offset +17: move-result v7 (userId)
        parseMethod.replaceInstruction(rowFetchIndex + 17, "nop")

        // Offset +16: invoke-virtual {v7}, Integer->intValue() (userId)
        parseMethod.replaceInstruction(
            rowFetchIndex + 16,
            """
            if-nez v7, :cond_user_null
            const/4 v7, 0x0
            goto :cond_user_done
            :cond_user_null
            invoke-virtual {v7}, Ljava/lang/Integer;->intValue()I
            move-result v7
            :cond_user_done
            """.trimIndent(),
        )

        // Offset +11: move-result v7 (ID)
        parseMethod.replaceInstruction(rowFetchIndex + 11, "nop")

        // Offset +10: invoke-virtual/range {v16 .. v16}, Integer->intValue() (ID)
        parseMethod.replaceInstruction(
            rowFetchIndex + 10,
            """
            if-nez v16, :cond_id_null
            const/4 v7, 0x0
            goto :cond_id_done
            :cond_id_null
            invoke-virtual/range {v16 .. v16}, Ljava/lang/Integer;->intValue()I
            move-result v7
            :cond_id_done
            """.trimIndent(),
        )

        // Offset +5: move-result v15 (флаги)
        parseMethod.replaceInstruction(rowFetchIndex + 5, "nop")

        // Offset +4: чтение флагов с проверкой null
        parseMethod.replaceInstruction(
            rowFetchIndex + 4,
            """
            invoke-virtual {v14, v7}, Lru/fourpda/client/y;->m(I)Ljava/lang/Integer;
            move-result-object v15
            if-nez v15, :cond_flags_null
            const/4 v15, 0x0
            goto :cond_flags_done
            :cond_flags_null
            invoke-virtual {v15}, Ljava/lang/Integer;->intValue()I
            move-result v15
            :cond_flags_done
            """.trimIndent(),
        )

        // Offset +3: если строка битая/удаленная ([0]), переходим к следующей итерации (loopNext)
        parseMethod.replaceInstruction(
            rowFetchIndex + 3,
            BuilderInstruction21t(Opcode.IF_EQZ, 15, loopNextBranch.target),
        )

        // Offset +2: проверка строки комментария на null и длину d() >= 6
        parseMethod.replaceInstruction(
            rowFetchIndex + 2,
            """
            if-nez v14, :cond_invalid_row
            invoke-virtual {v14}, Lru/fourpda/client/y;->d()I
            move-result v15
            const/4 v16, 0x6
            if-ge v15, v16, :cond_row_ok
            :cond_invalid_row
            const/4 v15, 0x0
            goto :cond_after_row_check
            :cond_row_ok
            const/4 v15, 0x1
            :cond_after_row_check
            """.trimIndent(),
        )
    }
}
