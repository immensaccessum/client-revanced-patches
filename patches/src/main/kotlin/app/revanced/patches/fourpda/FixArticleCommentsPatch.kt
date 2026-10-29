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
private const val Q0_N_CLASS = "Lru/fourpda/client/Q0\$n;"
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
        // Поиск строго ПОСЛЕ l13Index (начиная с l13Index + 2), чтобы не совпасть с самим l13Index
        var rowFetchIndex = -1
        for (i in (l13Index + 2) until minOf(l13Index + 50, instructions.size)) {
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

        require(instructions[rowFetchIndex + 1].opcode == Opcode.MOVE_RESULT_OBJECT) { "Неверная инструкция rowFetchIndex+1" }
        require(instructions[rowFetchIndex + 2].opcode == Opcode.INVOKE_VIRTUAL) { "Неверная инструкция rowFetchIndex+2" }
        require(instructions[rowFetchIndex + 3].opcode == Opcode.MOVE_RESULT_OBJECT) { "Неверная инструкция rowFetchIndex+3" }
        require(instructions[rowFetchIndex + 4].opcode == Opcode.INVOKE_VIRTUAL) { "Неверная инструкция rowFetchIndex+4" }
        require(instructions[rowFetchIndex + 5].opcode == Opcode.MOVE_RESULT) { "Неверная инструкция rowFetchIndex+5" }
        require(instructions[rowFetchIndex + 6].opcode == Opcode.NEW_INSTANCE) { "Неверная инструкция rowFetchIndex+6" }

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

        // 5. Защита от NullPointerException при чтении кармы (инструкция iput v15, v6, Q0$n->k:I)
        val karmaWriteIndex = instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.IPUT &&
                instruction.field()?.definingClass == Q0_N_CLASS &&
                instruction.field()?.name == "k"
        }
        if (karmaWriteIndex > rowFetchIndex) {
            require(instructions[karmaWriteIndex - 1].opcode == Opcode.MOVE_RESULT) { "Неверная инструкция перед karmaWrite" }
            require(instructions[karmaWriteIndex - 2].opcode == Opcode.INVOKE_VIRTUAL) { "Неверный invoke intValue перед karmaWrite" }

            parseMethod.replaceInstruction(karmaWriteIndex - 1, "nop")
            parseMethod.replaceInstruction(
                karmaWriteIndex - 2,
                """
                if-eqz v15, :cond_karma_null
                invoke-virtual {v15}, Ljava/lang/Integer;->intValue()I
                move-result v15
                goto :cond_karma_done
                :cond_karma_null
                const/4 v15, 0x0
                :cond_karma_done
                """.trimIndent(),
            )
        }

        // 6. Проверка строки комментария v14 на null и минимальный размер d() >= 6.
        // Заменяем в нисходящем порядке (rowFetchIndex + 4, + 3, + 2), чтобы не сдвигать смещения:
        //
        // rowFetchIndex + 4: invoke-virtual {v15}, Ljava/lang/Integer;->intValue()I
        parseMethod.replaceInstruction(
            rowFetchIndex + 4,
            """
            invoke-virtual {v14, v7}, Lru/fourpda/client/y;->m(I)Ljava/lang/Integer;
            move-result-object v15
            invoke-virtual {v15}, Ljava/lang/Integer;->intValue()I
            """.trimIndent(),
        )

        // rowFetchIndex + 3: move-result-object v15 -> заменяем на условный переход IF_EQZ к loopNext
        parseMethod.replaceInstruction(
            rowFetchIndex + 3,
            BuilderInstruction21t(Opcode.IF_EQZ, 15, loopNextBranch.target),
        )

        // rowFetchIndex + 2: invoke-virtual {v14, v7}, Lru/fourpda/client/y;->m(I)Ljava/lang/Integer; -> проверка v14
        parseMethod.replaceInstruction(
            rowFetchIndex + 2,
            """
            if-eqz v14, :cond_row_null
            invoke-virtual {v14}, Lru/fourpda/client/y;->d()I
            move-result v15
            const/4 v16, 0x6
            if-ge v15, v16, :cond_row_ok
            :cond_row_null
            const/4 v15, 0x0
            goto :cond_row_done
            :cond_row_ok
            const/4 v15, 0x1
            :cond_row_done
            """.trimIndent(),
        )
    }
}
