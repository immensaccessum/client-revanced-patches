package app.revanced.patches.fourpda

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.writer.builder.DexBuilder
import com.android.tools.smali.smali.smaliFlexLexer
import com.android.tools.smali.smali.smaliParser
import com.android.tools.smali.smali.smaliTreeWalker
import org.antlr.runtime.CommonTokenStream
import org.antlr.runtime.TokenSource
import org.antlr.runtime.tree.CommonTreeNodeStream
import java.io.StringReader

private const val Q0_CLASS = "Lru/fourpda/client/Q0;"
private const val Q0_S_CLASS = "Lru/fourpda/client/Q0\$s;"
private const val Y_CLASS = "Lru/fourpda/client/y;"
private const val ARTICLE_FIX_HELPER = "Lru/fourpda/client/ArticleFixHelper;"

private fun Instruction.calledMethod(): MethodReference? =
    (this as? ReferenceInstruction)?.reference as? MethodReference

private fun Instruction.field(): FieldReference? =
    (this as? ReferenceInstruction)?.reference as? FieldReference

private fun assemble(smali: String): ClassDef {
    val reader = StringReader(smali)
    val lexer = smaliFlexLexer(reader, 15)
    val tokens = CommonTokenStream(lexer as TokenSource)
    val parser = smaliParser(tokens)
    val fileTree = parser.smali_file()
    if (lexer.numberOfSyntaxErrors > 0 || parser.numberOfSyntaxErrors > 0) {
        error("ошибка сборки ArticleFixHelper")
    }
    val walker = smaliTreeWalker(
        CommonTreeNodeStream(fileTree.tree).apply { tokenStream = tokens },
    )
    walker.setDexBuilder(DexBuilder(Opcodes.getDefault()))
    return walker.smali_file()
}

private const val ARTICLE_FIX_HELPER_SMALI = """
.class public Lru/fourpda/client/ArticleFixHelper;
.super Ljava/lang/Object;

.method public constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static filterComments(Lru/fourpda/client/y;)Lru/fourpda/client/y;
    .locals 5
    if-eqz p0, :cond_done
    new-instance v0, Lru/fourpda/client/y;
    invoke-direct {v0}, Lru/fourpda/client/y;-><init>()V
    invoke-virtual {p0}, Lru/fourpda/client/y;->d()I
    move-result v1
    const/4 v2, 0x0
    :goto_loop
    if-ge v2, v1, :cond_return
    invoke-virtual {p0, v2}, Lru/fourpda/client/y;->l(I)Lru/fourpda/client/y;
    move-result-object v3
    if-eqz v3, :cond_skip
    invoke-virtual {v3}, Lru/fourpda/client/y;->d()I
    move-result v4
    const/4 v3, 0x6
    if-ge v4, v3, :cond_add
    goto :cond_skip
    :cond_add
    invoke-virtual {p0, v2}, Lru/fourpda/client/y;->f(I)Ljava/lang/Object;
    move-result-object v3
    invoke-virtual {v0, v3}, Lru/fourpda/client/y;->b(Ljava/lang/Object;)Lru/fourpda/client/y;
    :cond_skip
    add-int/lit8 v2, v2, 0x1
    goto :goto_loop
    :cond_return
    return-object v0
    :cond_done
    return-object p0
.end method

.method public static fixPollFlags(I)I
    .locals 1
    and-int/lit8 v0, p0, 0x1
    if-nez v0, :cond_can_vote
    or-int/lit8 p0, p0, 0x4
    :cond_can_vote
    return p0
.end method
"""

val fixArticleCommentsPatch = bytecodePatch(
    name = "Ремонт комментариев и опросов в статьях",
    description = "Исправляет отображение комментариев в статьях (предотвращает обрыв списка при удаленных комментариях и восстанавливает древовидность ответов) и включает отображение результатов закрытых опросов",
) {
    compatibleWith("ru.fourpda.client"("1.9.44_pre7"))

    apply {
        if (classDefs[ARTICLE_FIX_HELPER] == null) {
            classDefs += assemble(ARTICLE_FIX_HELPER_SMALI)
        }

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
            parseMethod.addInstructions(
                pollFlagsWriteIndex,
                """
                invoke-static {v13}, $ARTICLE_FIX_HELPER->fixPollFlags(I)I
                move-result v13
                """.trimIndent(),
            )
        }

        // 3. Фильтрация массива комментариев v0 от удаленных комментариев-заглушек [0].
        // Выполняется сразу при получении v0 = k.l(13) ДО цикла комментариев через addInstructions.
        // Отфильтрованный v0 содержит только валидные строки комментариев (длиной >= 6),
        // поэтому оригинальный цикл парсера никогда не падает с NullPointerException
        // и без сбоев восстанавливает все комментарии и их дерево вложенности.
        parseMethod.addInstructions(
            l13Index + 2,
            """
            invoke-static {v0}, $ARTICLE_FIX_HELPER->filterComments(Lru/fourpda/client/y;)Lru/fourpda/client/y;
            move-result-object v0
            """.trimIndent(),
        )
    }
}
