package app.revanced.patches.fourpda

import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
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

        require(instructions[l13Index + 1].opcode == Opcode.MOVE_RESULT_OBJECT) { "Неверная инструкция l13Index+1" }
        require(instructions[l13Index + 2].opcode == Opcode.NEW_INSTANCE) { "Неверная инструкция l13Index+2" }

        // 2. Починка опросов: если опрос закрыт ((poll.c & 1) == 0), выставляем бит 4 (poll.c |= 4),
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

        // 3. Защита от NullPointerException при чтении кармы (инструкция iput v15, v6, Q0$n->k:I)
        val karmaWriteIndex = instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.IPUT &&
                instruction.field()?.definingClass == Q0_N_CLASS &&
                instruction.field()?.name == "k"
        }
        if (karmaWriteIndex > l13Index) {
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

        // 4. Фильтрация массива комментариев v0 от удаленных комментариев-заглушек [0].
        // Выполняется сразу при получении v0 = k.l(13), ДО входа в цикл комментариев.
        // Отфильтрованный v0 содержит только валидные строки комментариев (длиной >= 6),
        // поэтому оригинальный цикл парсера никогда не падает с NullPointerException
        // и без сбоев восстанавливает все комментарии и их дерево вложенности.
        parseMethod.replaceInstruction(
            l13Index + 1,
            """
            move-result-object v0
            if-eqz v0, :cond_filter_done
            new-instance v11, $Y_CLASS
            invoke-direct {v11}, $Y_CLASS-><init>()V
            invoke-virtual {v0}, $Y_CLASS->d()I
            move-result v12
            const/4 v13, 0x0
            :goto_filter_loop
            if-ge v13, v12, :cond_filter_end
            invoke-virtual {v0, v13}, $Y_CLASS->l(I)$Y_CLASS
            move-result-object v14
            if-eqz v14, :cond_filter_next
            invoke-virtual {v14}, $Y_CLASS->d()I
            move-result v15
            const/4 v16, 0x6
            if-ge v15, v16, :cond_filter_add
            goto :cond_filter_next
            :cond_filter_add
            invoke-virtual {v11, v14}, $Y_CLASS->b(Ljava/lang/Object;)$Y_CLASS
            :cond_filter_next
            add-int/lit8 v13, v13, 0x1
            goto :goto_filter_loop
            :cond_filter_end
            move-object v0, v11
            :cond_filter_done
            """.trimIndent(),
        )
    }
}
