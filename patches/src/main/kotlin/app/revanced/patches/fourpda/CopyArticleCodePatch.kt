package app.revanced.patches.fourpda

import app.revanced.patcher.extensions.addInstructions
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
private const val TOPIC_PAGE = "Lru/fourpda/client/V0;"
private const val TOPIC_POST = "Lru/fourpda/client/V0\$z;"
private const val TOPIC_MENU = "Lru/fourpda/client/V0\$n;"
private const val POST_MENU = "Lru/fourpda/client/V0\$i;"
private const val ARTICLE_BB = "Lru/fourpda/client/ArticleBb;"

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

.field private static posts:Ljava/util/WeakHashMap;

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

.method public static onMenu(Ljava/lang/Object;I)V
    .registers 4
    const/4 v0, 0x4
    if-ne p1, v0, :skip
    check-cast p0, Lru/fourpda/client/Q0${'$'}d;
    iget-object v0, p0, Lru/fourpda/client/Q0${'$'}d;->a:Lru/fourpda/client/Q0;
    iget-object v1, v0, Lru/fourpda/client/Q0;->G:Lru/fourpda/client/Q0${'$'}m;
    iget-object v0, v0, Lru/fourpda/client/w0;->g:Lru/fourpda/client/MainActivity;
    invoke-static {v1, v0}, Lru/fourpda/client/ArticleBb;->copy(Lru/fourpda/client/Q0${'$'}m;Lru/fourpda/client/MainActivity;)V
    :skip
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
    const-string v1, "Код скопирован"
    invoke-static {p1, v0, v1}, Lru/fourpda/client/s1;->a(Landroid/app/Activity;Ljava/lang/String;Ljava/lang/String;)V
    return-void
    :missing
    const-string v0, "Код статьи ещё не загружен"
    const/4 v1, 0x0
    invoke-static {p1, v0, v1}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object v0
    invoke-virtual {v0}, Landroid/widget/Toast;->show()V
    :abort
    return-void
.end method

.method public static savePost(Lru/fourpda/client/V0${'$'}z;Ljava/lang/String;)V
    .registers 3
    if-nez p1, :put
    return-void
    :put
    sget-object v0, Lru/fourpda/client/ArticleBb;->posts:Ljava/util/WeakHashMap;
    if-nez v0, :ready
    new-instance v0, Ljava/util/WeakHashMap;
    invoke-direct {v0}, Ljava/util/WeakHashMap;-><init>()V
    sput-object v0, Lru/fourpda/client/ArticleBb;->posts:Ljava/util/WeakHashMap;
    :ready
    invoke-virtual {v0, p0, p1}, Ljava/util/WeakHashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    return-void
.end method

.method public static onTopicMenu(Ljava/lang/Object;I)V
    .registers 4
    const/16 v0, 0x9
    if-ne p1, v0, :skip
    check-cast p0, Lru/fourpda/client/V0${'$'}n;
    iget-object v0, p0, Lru/fourpda/client/V0${'$'}n;->a:Lru/fourpda/client/V0;
    iget-object v1, v0, Lru/fourpda/client/w0;->g:Lru/fourpda/client/MainActivity;
    invoke-static {v0, v1}, Lru/fourpda/client/ArticleBb;->copyPage(Lru/fourpda/client/V0;Lru/fourpda/client/MainActivity;)V
    :skip
    return-void
.end method

.method public static onPostMenu(Ljava/lang/Object;I)V
    .registers 4
    const/16 v0, 0xa
    if-ne p1, v0, :skip
    check-cast p0, Lru/fourpda/client/V0${'$'}i;
    iget-object v0, p0, Lru/fourpda/client/V0${'$'}i;->d:Lru/fourpda/client/V0${'$'}z;
    iget-object v1, p0, Lru/fourpda/client/V0${'$'}i;->f:Lru/fourpda/client/V0;
    iget-object v1, v1, Lru/fourpda/client/w0;->g:Lru/fourpda/client/MainActivity;
    invoke-static {v0, v1}, Lru/fourpda/client/ArticleBb;->copyPost(Lru/fourpda/client/V0${'$'}z;Lru/fourpda/client/MainActivity;)V
    :skip
    return-void
.end method

.method public static copyPost(Lru/fourpda/client/V0${'$'}z;Lru/fourpda/client/MainActivity;)V
    .registers 4
    if-eqz p1, :abort
    if-eqz p0, :missing
    sget-object v0, Lru/fourpda/client/ArticleBb;->posts:Ljava/util/WeakHashMap;
    if-eqz v0, :missing
    invoke-virtual {v0, p0}, Ljava/util/WeakHashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object v0
    if-eqz v0, :missing
    check-cast v0, Ljava/lang/String;
    const-string v1, "Код поста скопирован"
    invoke-static {p1, v0, v1}, Lru/fourpda/client/s1;->a(Landroid/app/Activity;Ljava/lang/String;Ljava/lang/String;)V
    return-void
    :missing
    const-string v0, "Код поста ещё не загружен"
    const/4 v1, 0x0
    invoke-static {p1, v0, v1}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object v0
    invoke-virtual {v0}, Landroid/widget/Toast;->show()V
    :abort
    return-void
.end method

.method public static copyPage(Lru/fourpda/client/V0;Lru/fourpda/client/MainActivity;)V
    .registers 8
    if-nez p1, :haveAct
    return-void
    :haveAct
    if-eqz p0, :empty
    iget-object v0, p0, Lru/fourpda/client/V0;->I:Ljava/util/List;
    if-eqz v0, :empty
    invoke-interface {v0}, Ljava/util/List;->size()I
    move-result v1
    if-lez v1, :empty
    new-instance v2, Ljava/lang/StringBuilder;
    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V
    const/4 v3, 0x0
    :loop
    if-ge v3, v1, :finish
    if-lez v3, :head
    const-string v5, "\u000a\u000a"
    invoke-virtual {v2, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    :head
    invoke-interface {v0, v3}, Ljava/util/List;->get(I)Ljava/lang/Object;
    move-result-object v4
    check-cast v4, Lru/fourpda/client/V0${'$'}z;
    const-string v5, "#"
    invoke-virtual {v2, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    iget v5, v4, Lru/fourpda/client/V0${'$'}z;->a:I
    invoke-virtual {v2, v5}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    const-string v5, " "
    invoke-virtual {v2, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    iget-object v5, v4, Lru/fourpda/client/V0${'$'}z;->c:Ljava/lang/String;
    if-eqz v5, :noname
    invoke-virtual {v2, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    :noname
    const-string v5, "\u000a"
    invoke-virtual {v2, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    sget-object v5, Lru/fourpda/client/ArticleBb;->posts:Ljava/util/WeakHashMap;
    if-eqz v5, :nobody
    invoke-virtual {v5, v4}, Ljava/util/WeakHashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object v4
    if-eqz v4, :nobody
    check-cast v4, Ljava/lang/String;
    invoke-virtual {v2, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    goto :next
    :nobody
    const-string v4, "(пусто)"
    invoke-virtual {v2, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    :next
    add-int/lit8 v3, v3, 0x1
    goto :loop
    :finish
    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v0
    const-string v1, "Сообщения скопированы"
    invoke-static {p1, v0, v1}, Lru/fourpda/client/s1;->a(Landroid/app/Activity;Ljava/lang/String;Ljava/lang/String;)V
    return-void
    :empty
    const-string v0, "На странице нет сообщений"
    const/4 v1, 0x0
    invoke-static {p1, v0, v1}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object v0
    invoke-virtual {v0}, Landroid/widget/Toast;->show()V
    return-void
.end method
"""

val copyArticleCodePatch = bytecodePatch(
    name = "Скопировать код",
    description = "В меню статьи — код статьи. В меню поста — код поста. В меню темы — все сообщения открытой страницы. Берётся уже загруженный текст, без нового запроса.",
) {
    compatibleWith("ru.fourpda.client")

    apply {
        if (classDefs[ARTICLE_BB] == null) {
            classDefs += assemble(ARTICLE_BB_SMALI)
        }

        val page = classDefs[ARTICLE_PAGE] ?: error("в apk нет страницы статьи")
        val menuMethod = proxy(page).mutableClass.methods.singleOrNull { method ->
            method.implementation?.instructions?.any { it.constString() == "Открыть в браузере" } == true
        } ?: error("не найдено меню статьи")
        val instructions = menuMethod.implementation!!.instructions
        val browser = instructions.indexOfFirst { it.constString() == "Открыть в браузере" }
        val addItem = (browser + 1 until minOf(browser + 4, instructions.size)).firstOrNull { index ->
            val called = instructions[index].calledMethod()
            called?.name == "a" && called.definingClass == "Lru/fourpda/client/B1;"
        } ?: error("пункт меню статьи стоит не там, где ожидалось")
        menuMethod.addInstructions(
            addItem + 1,
            """
            const/4 v1, 0x4
            const-string v2, "Скопировать код"
            invoke-virtual {v0, v6, v6, v1, v2}, Lru/fourpda/client/B1;->a(IIILjava/lang/String;)V
            """.trimIndent(),
        )

        val menuClass = classDefs[ARTICLE_MENU] ?: error("в apk нет обработчика меню статьи")
        val click = proxy(menuClass).mutableClass.methods.first { it.name == "a" && it.parameterTypes.size == 3 }
        click.addInstructions(
            0,
            "invoke-static {p0, p3}, $ARTICLE_BB->onMenu(Ljava/lang/Object;I)V",
        )

        val model = classDefs[ARTICLE_MODEL] ?: error("в apk нет модели статьи")
        val parse = proxy(model).mutableClass.methods.first { method ->
            method.name == "a" && method.parameterTypes.size == 2
        }
        val loaded = parse.implementation!!.instructions.withIndex().firstOrNull { (index, instruction) ->
            instruction.opcode == Opcode.MOVE_RESULT_OBJECT &&
                parse.implementation!!.instructions[index - 1].calledMethod()?.name == "n"
        } ?: error("не найдено поле с текстом статьи")
        parse.addInstructions(
            loaded.index + 1,
            "invoke-static {v0, v1}, $ARTICLE_BB->save(${ARTICLE_MODEL}Ljava/lang/String;)V",
        )

        val topic = classDefs[TOPIC_PAGE] ?: error("в apk нет страницы темы")
        val topicMenu = proxy(topic).mutableClass.methods.singleOrNull { method ->
            method.implementation?.instructions?.any { it.constString() == "О теме" } == true
        } ?: error("не найдено меню темы")
        val topicInstructions = topicMenu.implementation!!.instructions
        val about = topicInstructions.indexOfFirst { it.constString() == "О теме" }
        val aboutItem = (about + 1 until minOf(about + 4, topicInstructions.size)).firstOrNull { index ->
            val called = topicInstructions[index].calledMethod()
            called?.name == "a" && called.definingClass == "Lru/fourpda/client/B1;"
        } ?: error("пункт меню темы стоит не там, где ожидалось")
        topicMenu.addInstructions(
            aboutItem + 1,
            """
            const/16 v2, 0x9
            const-string v4, "Скопировать все сообщения со страницы"
            invoke-virtual {v8, v5, v5, v2, v4}, Lru/fourpda/client/B1;->a(IIILjava/lang/String;)V
            """.trimIndent(),
        )

        val topicClick = proxy(classDefs[TOPIC_MENU] ?: error("в apk нет обработчика меню темы"))
            .mutableClass.methods.first { it.name == "a" && it.parameterTypes.size == 3 }
        topicClick.addInstructions(
            0,
            """
            move-object/from16 v0, p0
            move/from16 v1, p3
            invoke-static {v0, v1}, $ARTICLE_BB->onTopicMenu(Ljava/lang/Object;I)V
            """.trimIndent(),
        )

        val postMenu = proxy(topic).mutableClass.methods.singleOrNull { method ->
            method.implementation?.instructions?.any { it.constString() == "Упоминания поста" } == true
        } ?: error("не найдено меню поста")
        val postInstructions = postMenu.implementation!!.instructions
        val mentions = postInstructions.indexOfFirst { it.constString() == "Упоминания поста" }
        val mentionsItem = (mentions + 1 until minOf(mentions + 4, postInstructions.size)).firstOrNull { index ->
            val called = postInstructions[index].calledMethod()
            called?.name == "a" && called.definingClass == "Lru/fourpda/client/B1;"
        } ?: error("пункт меню поста стоит не там, где ожидалось")
        postMenu.addInstructions(
            mentionsItem + 1,
            """
            const/16 v0, 0xa
            const-string v2, "Скопировать код поста"
            invoke-virtual {v9, v8, v8, v0, v2}, Lru/fourpda/client/B1;->a(IIILjava/lang/String;)V
            """.trimIndent(),
        )

        val postClick = proxy(classDefs[POST_MENU] ?: error("в apk нет обработчика меню поста"))
            .mutableClass.methods.first { it.name == "a" && it.parameterTypes.size == 3 }
        postClick.addInstructions(
            0,
            """
            move-object/from16 v0, p0
            move/from16 v1, p3
            invoke-static {v0, v1}, $ARTICLE_BB->onPostMenu(Ljava/lang/Object;I)V
            """.trimIndent(),
        )

        val postParse = proxy(classDefs[TOPIC_POST] ?: error("в apk нет модели поста"))
            .mutableClass.methods.first { it.name == "b" && it.parameterTypes.size == 2 }
        val postText = postParse.implementation!!.instructions.withIndex().firstOrNull { (index, instruction) ->
            instruction.opcode == Opcode.MOVE_RESULT_OBJECT &&
                postParse.implementation!!.instructions[index - 1].calledMethod()?.name == "n"
        } ?: error("не найдено поле с текстом поста")
        postParse.addInstructions(
            postText.index + 1,
            "invoke-static {v2, v3}, $ARTICLE_BB->savePost(${TOPIC_POST}Ljava/lang/String;)V",
        )
    }
}
