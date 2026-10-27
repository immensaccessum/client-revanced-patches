package app.revanced.patches.fourpda

import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.writer.builder.DexBuilder
import com.android.tools.smali.smali.smaliFlexLexer
import com.android.tools.smali.smali.smaliParser
import com.android.tools.smali.smali.smaliTreeWalker
import org.antlr.runtime.CommonTokenStream
import org.antlr.runtime.TokenSource
import org.antlr.runtime.tree.CommonTreeNodeStream
import java.io.StringReader

private const val ARTICLE_PAGE = "Lru/fourpda/client/Q0;"
private const val ARTICLE_MODEL = "Lru/fourpda/client/Q0\$m;"
private const val ARTICLE_MENU = "Lru/fourpda/client/Q0\$d;"
private const val ARTICLE_BB = "Lru/fourpda/client/ArticleBb;"
private const val MENU_ID = 4

private fun Instruction.calledMethod(): MethodReference? =
    (this as? ReferenceInstruction)?.reference as? MethodReference

private fun Instruction.constString(): String? {
    if (opcode != Opcode.CONST_STRING && opcode != Opcode.CONST_STRING_JUMBO) {
        return null
    }
    return ((this as? ReferenceInstruction)?.reference as? StringReference)?.string
}

private fun assemble(smali: String): ClassDef {
    val reader = StringReader(smali)
    val lexer = smaliFlexLexer(reader, 15)
    val tokens = CommonTokenStream(lexer as TokenSource)
    val parser = smaliParser(tokens)
    val fileTree = parser.smali_file()
    if (lexer.numberOfSyntaxErrors > 0 || parser.numberOfSyntaxErrors > 0) {
        error("ошибка сборки ArticleBb")
    }
    val walker = smaliTreeWalker(
        CommonTreeNodeStream(fileTree.tree).apply { tokenStream = tokens },
    )
    walker.setDexBuilder(DexBuilder(Opcodes.getDefault()))
    return walker.smali_file()
}

private const val ARTICLE_BB_SMALI = """
.class public Lru/fourpda/client/ArticleBb;
.super Ljava/lang/Object;

.field private static map:Ljava/util/WeakHashMap;

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static save(Lru/fourpda/client/Q0${'$'}m;Ljava/lang/String;)V
    .registers 3
    if-nez p1, :put
    return-void
    :put
    sget-object v0, Lru/fourpda/client/ArticleBb;->map:Ljava/util/WeakHashMap;
    if-nez v0, :ready
    new-instance v0, Ljava/util/WeakHashMap;
    invoke-direct {v0}, Ljava/util/WeakHashMap;-><init>()V
    sput-object v0, Lru/fourpda/client/ArticleBb;->map:Ljava/util/WeakHashMap;
    :ready
    invoke-virtual {v0, p0, p1}, Ljava/util/WeakHashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    return-void
.end method

.method public static copy(Lru/fourpda/client/Q0${'$'}m;Lru/fourpda/client/MainActivity;)V
    .registers 4
    if-eqz p1, :abort
    if-eqz p0, :missing
    sget-object v0, Lru/fourpda/client/ArticleBb;->map:Ljava/util/WeakHashMap;
    if-eqz v0, :missing
    invoke-virtual {v0, p0}, Ljava/util/WeakHashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object v0
    if-eqz v0, :missing
    check-cast v0, Ljava/lang/String;
    const-string v1, "\u041a\u043e\u0434 \u0441\u043a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u043d"
    invoke-static {p1, v0, v1}, Lru/fourpda/client/s1;->a(Landroid/app/Activity;Ljava/lang/String;Ljava/lang/String;)V
    return-void
    :missing
    const-string v0, "\u041a\u043e\u0434 \u0441\u0442\u0430\u0442\u044c\u0438 \u0435\u0449\u0451 \u043d\u0435 \u0437\u0430\u0433\u0440\u0443\u0436\u0435\u043d"
    const/4 v1, 0x0
    invoke-static {p1, v0, v1}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object v0
    invoke-virtual {v0}, Landroid/widget/Toast;->show()V
    :abort
    return-void
.end method
"""

val copyArticleCodePatch = bytecodePatch(
    name = "Скопировать код статьи",
    description = "В меню статьи пункт «Скопировать код» кладёт BB-код в буфер обмена.",
) {
    compatibleWith("ru.fourpda.client")

    apply {
        val page = classDefs[ARTICLE_PAGE] ?: error("в apk нет страницы статьи")
        val menuMethod = proxy(page).mutableClass.methods.singleOrNull { method ->
            method.implementation?.instructions?.any { it.constString() == "Открыть в браузере" } == true
        } ?: error("не найдено меню статьи")

        val browser = menuMethod.implementation!!.instructions.indexOfFirst { it.constString() == "Открыть в браузере" }
        val addItem = menuMethod.implementation!!.instructions.getOrNull(browser + 1)
        if (addItem?.calledMethod()?.name != "a") {
            error("пункт меню статьи стоит не там, где ожидалось")
        }
        menuMethod.addInstructions(
            browser + 2,
            """
            const/4 v1, 0x$MENU_ID
            const-string v2, "Скопировать код"
            invoke-virtual {v0, v6, v6, v1, v2}, Lru/fourpda/client/B1;->a(IIILjava/lang/String;)V
            """.trimIndent(),
        )

        val menuClass = classDefs[ARTICLE_MENU] ?: error("в apk нет обработчика меню статьи")
        val click = proxy(menuClass).mutableClass.methods.single { it.name == "a" && it.parameterTypes.size == 3 }
        val originalStart = click.implementation!!.instructions.first()
        click.addInstructionsWithLabels(
            0,
            """
            const/4 p1, 0x$MENU_ID
            if-ne p3, p1, :not_copy
            iget-object p1, p0, $ARTICLE_MENU->a:$ARTICLE_PAGE
            iget-object p2, p1, $ARTICLE_PAGE->G:$ARTICLE_MODEL
            iget-object p1, p1, Lru/fourpda/client/w0;->g:Lru/fourpda/client/MainActivity;
            invoke-static {p2, p1}, $ARTICLE_BB->copy($ARTICLE_MODEL;Lru/fourpda/client/MainActivity;)V
            return-void
            """.trimIndent(),
            ExternalLabel("not_copy", originalStart),
        )

        val model = classDefs[ARTICLE_MODEL] ?: error("в apk нет модели статьи")
        val parse = proxy(model).mutableClass.methods.singleOrNull { method ->
            method.name == "a" && method.parameterTypes.size == 2
        } ?: error("не найден разбор статьи")
        val loaded = parse.implementation!!.instructions.withIndex().firstOrNull { (index, instruction) ->
            instruction.opcode == Opcode.MOVE_RESULT_OBJECT &&
                parse.implementation!!.instructions[index - 1].calledMethod()?.name == "n"
        } ?: error("не найдено поле с текстом статьи")
        parse.addInstructions(
            loaded.index + 1,
            "invoke-static {v0, v1}, $ARTICLE_BB->save($ARTICLE_MODEL;Ljava/lang/String;)V",
        )

        if (classDefs[ARTICLE_BB] == null) {
            classDefs += assemble(ARTICLE_BB_SMALI)
        }
    }
}
