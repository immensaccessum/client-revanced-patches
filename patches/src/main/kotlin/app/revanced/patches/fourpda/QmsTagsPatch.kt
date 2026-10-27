package app.revanced.patches.fourpda

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.writer.builder.DexBuilder
import com.android.tools.smali.smali.smaliFlexLexer
import com.android.tools.smali.smali.smaliParser
import com.android.tools.smali.smali.smaliTreeWalker
import org.antlr.runtime.CommonTokenStream
import org.antlr.runtime.TokenSource
import org.antlr.runtime.tree.CommonTreeNodeStream
import java.io.StringReader

private const val QMS_PAGE = "Lru/fourpda/client/M0;"
private const val TAG_BAR = "Lru/fourpda/client/s;"
private const val QMS_TAGS = "Lru/fourpda/client/QmsTags;"

private fun Instruction.calledMethod(): MethodReference? =
    (this as? ReferenceInstruction)?.reference as? MethodReference

private fun assemble(smali: String): ClassDef {
    val reader = StringReader(smali)
    val lexer = smaliFlexLexer(reader, 15)
    val tokens = CommonTokenStream(lexer as TokenSource)
    val parser = smaliParser(tokens)
    val fileTree = parser.smali_file()
    if (lexer.numberOfSyntaxErrors > 0 || parser.numberOfSyntaxErrors > 0) {
        error("ошибка сборки QmsTags")
    }
    val walker = smaliTreeWalker(
        CommonTreeNodeStream(fileTree.tree).apply { tokenStream = tokens },
    )
    walker.setDexBuilder(DexBuilder(Opcodes.getDefault()))
    return walker.smali_file()
}

private const val QMS_TAGS_SMALI = """
.class public Lru/fourpda/client/QmsTags;
.super Ljava/lang/Object;

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static make(Landroid/content/Context;IIII)Landroid/view/View;
    .registers 8
    new-instance v0, Landroid/view/View;
    invoke-direct {v0, p0}, Landroid/view/View;-><init>(Landroid/content/Context;)V
    invoke-virtual {v0, p1}, Landroid/view/View;->setId(I)V
    invoke-virtual {v0, p2}, Landroid/view/View;->setBackgroundResource(I)V
    const/4 v1, 0x1
    invoke-virtual {v0, v1}, Landroid/view/View;->setClickable(Z)V
    const/4 v1, 0x0
    invoke-virtual {v0, v1, v1, v1, v1}, Landroid/view/View;->setPadding(IIII)V
    new-instance v2, Landroid/widget/LinearLayout${'$'}LayoutParams;
    invoke-direct {v2, p3, p4}, Landroid/widget/LinearLayout${'$'}LayoutParams;-><init>(II)V
    invoke-virtual {v0, v2}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup${'$'}LayoutParams;)V
    return-object v0
.end method

.method public static insert(Landroid/widget/LinearLayout;ILandroid/view/View;)V
    .registers 6
    invoke-virtual {p0, p1}, Landroid/view/View;->findViewById(I)Landroid/view/View;
    move-result-object v0
    if-nez v0, :found
    return-void
    :found
    invoke-virtual {p0, v0}, Landroid/view/ViewGroup;->indexOfChild(Landroid/view/View;)I
    move-result v1
    const/4 v2, -0x1
    if-ne v1, v2, :index
    return-void
    :index
    add-int/lit8 v1, v1, 0x1
    invoke-virtual {p0, p2, v1}, Landroid/view/ViewGroup;->addView(Landroid/view/View;I)V
    return-void
.end method

.method public static add(Landroid/view/View;)I
    .registers 12
    sget v4, Lru/fourpda/client/e1;->m6:I
    invoke-virtual {p0, v4}, Landroid/view/View;->findViewById(I)Landroid/view/View;
    move-result-object v0
    if-nez v0, :scroll
    const/4 v2, 0x0
    return v2
    :scroll
    instance-of v2, v0, Landroid/widget/HorizontalScrollView;
    if-nez v2, :is_scroll
    const/4 v2, 0x0
    return v2
    :is_scroll
    check-cast v0, Landroid/widget/HorizontalScrollView;
    const/4 v2, 0x1
    invoke-virtual {v0, v2}, Landroid/view/View;->setHorizontalScrollBarEnabled(Z)V
    const/4 v2, 0x0
    invoke-virtual {v0, v2}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;
    move-result-object v1
    if-nez v1, :child
    const/4 v2, 0x0
    return v2
    :child
    instance-of v2, v1, Landroid/widget/LinearLayout;
    if-nez v2, :is_bar
    const/4 v2, 0x0
    return v2
    :is_bar
    check-cast v1, Landroid/widget/LinearLayout;
    sget v4, Lru/fourpda/client/e1;->G1:I
    invoke-virtual {v1, v4}, Landroid/view/View;->findViewById(I)Landroid/view/View;
    move-result-object v9
    if-eqz v9, :fresh
    const/4 v2, 0x1
    return v2
    :fresh
    sget v4, Lru/fourpda/client/e1;->p1:I
    invoke-virtual {v1, v4}, Landroid/view/View;->findViewById(I)Landroid/view/View;
    move-result-object v9
    if-nez v9, :sample
    const/4 v2, 0x0
    return v2
    :sample
    invoke-virtual {v9}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup${'$'}LayoutParams;
    move-result-object v10
    if-nez v10, :params
    const/4 v2, 0x0
    return v2
    :params
    iget v2, v10, Landroid/view/ViewGroup${'$'}LayoutParams;->width:I
    iget v3, v10, Landroid/view/ViewGroup${'$'}LayoutParams;->height:I
    invoke-virtual {v1}, Landroid/view/View;->getContext()Landroid/content/Context;
    move-result-object v7

    sget v4, Lru/fourpda/client/e1;->J1:I
    sget v5, Lru/fourpda/client/e1;->G1:I
    sget v6, Lru/fourpda/client/R${'$'}drawable;->tag_size:I
    invoke-static {v7, v5, v6, v2, v3}, Lru/fourpda/client/QmsTags;->make(Landroid/content/Context;IIII)Landroid/view/View;
    move-result-object v8
    invoke-static {v1, v4, v8}, Lru/fourpda/client/QmsTags;->insert(Landroid/widget/LinearLayout;ILandroid/view/View;)V

    sget v4, Lru/fourpda/client/e1;->G1:I
    sget v5, Lru/fourpda/client/e1;->t1:I
    sget v6, Lru/fourpda/client/R${'$'}drawable;->tag_color:I
    invoke-static {v7, v5, v6, v2, v3}, Lru/fourpda/client/QmsTags;->make(Landroid/content/Context;IIII)Landroid/view/View;
    move-result-object v8
    invoke-static {v1, v4, v8}, Lru/fourpda/client/QmsTags;->insert(Landroid/widget/LinearLayout;ILandroid/view/View;)V

    sget v4, Lru/fourpda/client/e1;->t1:I
    sget v5, Lru/fourpda/client/e1;->x1:I
    sget v6, Lru/fourpda/client/R${'$'}drawable;->tag_link:I
    invoke-static {v7, v5, v6, v2, v3}, Lru/fourpda/client/QmsTags;->make(Landroid/content/Context;IIII)Landroid/view/View;
    move-result-object v8
    invoke-static {v1, v4, v8}, Lru/fourpda/client/QmsTags;->insert(Landroid/widget/LinearLayout;ILandroid/view/View;)V

    sget v4, Lru/fourpda/client/e1;->x1:I
    sget v5, Lru/fourpda/client/e1;->y1:I
    sget v6, Lru/fourpda/client/R${'$'}drawable;->tag_list:I
    invoke-static {v7, v5, v6, v2, v3}, Lru/fourpda/client/QmsTags;->make(Landroid/content/Context;IIII)Landroid/view/View;
    move-result-object v8
    invoke-static {v1, v4, v8}, Lru/fourpda/client/QmsTags;->insert(Landroid/widget/LinearLayout;ILandroid/view/View;)V

    sget v4, Lru/fourpda/client/e1;->y1:I
    sget v5, Lru/fourpda/client/e1;->z1:I
    sget v6, Lru/fourpda/client/R${'$'}drawable;->tag_list_number:I
    invoke-static {v7, v5, v6, v2, v3}, Lru/fourpda/client/QmsTags;->make(Landroid/content/Context;IIII)Landroid/view/View;
    move-result-object v8
    invoke-static {v1, v4, v8}, Lru/fourpda/client/QmsTags;->insert(Landroid/widget/LinearLayout;ILandroid/view/View;)V

    sget v4, Lru/fourpda/client/e1;->E1:I
    sget v5, Lru/fourpda/client/e1;->H1:I
    sget v6, Lru/fourpda/client/R${'$'}drawable;->tag_spoiler:I
    invoke-static {v7, v5, v6, v2, v3}, Lru/fourpda/client/QmsTags;->make(Landroid/content/Context;IIII)Landroid/view/View;
    move-result-object v8
    invoke-static {v1, v4, v8}, Lru/fourpda/client/QmsTags;->insert(Landroid/widget/LinearLayout;ILandroid/view/View;)V

    sget v4, Lru/fourpda/client/e1;->H1:I
    sget v5, Lru/fourpda/client/e1;->I1:I
    sget v6, Lru/fourpda/client/R${'$'}drawable;->tag_spoiler_named:I
    invoke-static {v7, v5, v6, v2, v3}, Lru/fourpda/client/QmsTags;->make(Landroid/content/Context;IIII)Landroid/view/View;
    move-result-object v8
    invoke-static {v1, v4, v8}, Lru/fourpda/client/QmsTags;->insert(Landroid/widget/LinearLayout;ILandroid/view/View;)V

    sget v4, Lru/fourpda/client/e1;->s1:I
    sget v5, Lru/fourpda/client/e1;->w1:I
    sget v6, Lru/fourpda/client/R${'$'}drawable;->tag_left:I
    invoke-static {v7, v5, v6, v2, v3}, Lru/fourpda/client/QmsTags;->make(Landroid/content/Context;IIII)Landroid/view/View;
    move-result-object v8
    invoke-static {v1, v4, v8}, Lru/fourpda/client/QmsTags;->insert(Landroid/widget/LinearLayout;ILandroid/view/View;)V

    sget v4, Lru/fourpda/client/e1;->w1:I
    sget v5, Lru/fourpda/client/e1;->q1:I
    sget v6, Lru/fourpda/client/R${'$'}drawable;->tag_center:I
    invoke-static {v7, v5, v6, v2, v3}, Lru/fourpda/client/QmsTags;->make(Landroid/content/Context;IIII)Landroid/view/View;
    move-result-object v8
    invoke-static {v1, v4, v8}, Lru/fourpda/client/QmsTags;->insert(Landroid/widget/LinearLayout;ILandroid/view/View;)V

    sget v4, Lru/fourpda/client/e1;->q1:I
    sget v5, Lru/fourpda/client/e1;->F1:I
    sget v6, Lru/fourpda/client/R${'$'}drawable;->tag_right:I
    invoke-static {v7, v5, v6, v2, v3}, Lru/fourpda/client/QmsTags;->make(Landroid/content/Context;IIII)Landroid/view/View;
    move-result-object v8
    invoke-static {v1, v4, v8}, Lru/fourpda/client/QmsTags;->insert(Landroid/widget/LinearLayout;ILandroid/view/View;)V

    sget v4, Lru/fourpda/client/e1;->F1:I
    sget v5, Lru/fourpda/client/e1;->K1:I
    sget v6, Lru/fourpda/client/R${'$'}drawable;->tag_sub:I
    invoke-static {v7, v5, v6, v2, v3}, Lru/fourpda/client/QmsTags;->make(Landroid/content/Context;IIII)Landroid/view/View;
    move-result-object v8
    invoke-static {v1, v4, v8}, Lru/fourpda/client/QmsTags;->insert(Landroid/widget/LinearLayout;ILandroid/view/View;)V

    sget v4, Lru/fourpda/client/e1;->K1:I
    sget v5, Lru/fourpda/client/e1;->L1:I
    sget v6, Lru/fourpda/client/R${'$'}drawable;->tag_sup:I
    invoke-static {v7, v5, v6, v2, v3}, Lru/fourpda/client/QmsTags;->make(Landroid/content/Context;IIII)Landroid/view/View;
    move-result-object v8
    invoke-static {v1, v4, v8}, Lru/fourpda/client/QmsTags;->insert(Landroid/widget/LinearLayout;ILandroid/view/View;)V

    sget v4, Lru/fourpda/client/e1;->L1:I
    sget v5, Lru/fourpda/client/e1;->D1:I
    sget v6, Lru/fourpda/client/R${'$'}drawable;->tag_offtop:I
    invoke-static {v7, v5, v6, v2, v3}, Lru/fourpda/client/QmsTags;->make(Landroid/content/Context;IIII)Landroid/view/View;
    move-result-object v8
    invoke-static {v1, v4, v8}, Lru/fourpda/client/QmsTags;->insert(Landroid/widget/LinearLayout;ILandroid/view/View;)V
    const/4 v2, 0x1
    return v2
.end method
"""

val qmsTagsPatch = bytecodePatch(
    name = "Теги QMS",
    description = "В панели тегов QMS: размер, цвет, ссылка, списки, спойлер, выравнивание, индекс и оффтоп. Панель прокручивается вбок. Скрытие и метки модераторов не добавляются.",
) {
    compatibleWith("ru.fourpda.client"("1.9.44_pre7"))

    apply {
        if (classDefs[QMS_TAGS] == null) {
            classDefs += assemble(QMS_TAGS_SMALI)
        }

        val page = classDefs[QMS_PAGE] ?: error("в apk нет страницы QMS")
        val setup = proxy(page).mutableClass.methods.firstOrNull { method ->
            method.implementation?.instructions?.any { instruction ->
                val called = instruction.calledMethod()
                called?.definingClass == TAG_BAR && called.name == "<init>"
            } == true
        } ?: error("не найден конструктор панели тегов QMS")
        val instructions = setup.implementation!!.instructions
        val init = instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.INVOKE_DIRECT &&
                instruction.calledMethod()?.definingClass == TAG_BAR
        }
        val call = instructions[init] as FiveRegisterInstruction
        if (call.registerE > 15 || call.registerG > 15) {
            error("панель тегов QMS попала в широкий регистр")
        }
        setup.addInstructions(
            init,
            """
            invoke-static {v${call.registerE}}, $QMS_TAGS->add(Landroid/view/View;)I
            move-result v${call.registerG}
            """.trimIndent(),
        )
    }
}
