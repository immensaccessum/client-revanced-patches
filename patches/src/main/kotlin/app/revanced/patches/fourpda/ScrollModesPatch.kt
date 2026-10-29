package app.revanced.patches.fourpda

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21t
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22t
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
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

private const val SCROLL_MODES = "Lru/fourpda/client/ScrollModes;"
private const val LIST = "Lru/fourpda/client/l1\$g;"
private const val SETTINGS = "Lru/fourpda/client/I0;"
private const val SETTINGS_CLICK = "Lru/fourpda/client/I0\$v;"

private fun Instruction.calledMethod(): MethodReference? =
    (this as? ReferenceInstruction)?.reference as? MethodReference

private fun Instruction.intLiteral(): Int? =
    (this as? WideLiteralInstruction)?.wideLiteral?.toInt()

private fun Instruction.stringConst(): String? =
    ((this as? ReferenceInstruction)?.reference as? StringReference)?.string

private fun Instruction.fieldRef(): FieldReference? =
    (this as? ReferenceInstruction)?.reference as? FieldReference

private fun assemble(smali: String): ClassDef {
    val reader = StringReader(smali)
    val lexer = smaliFlexLexer(reader, 15)
    val tokens = CommonTokenStream(lexer as TokenSource)
    val parser = smaliParser(tokens)
    val fileTree = parser.smali_file()
    if (lexer.numberOfSyntaxErrors > 0 || parser.numberOfSyntaxErrors > 0) {
        error("ошибка сборки ScrollModes")
    }
    val walker = smaliTreeWalker(
        CommonTreeNodeStream(fileTree.tree).apply { tokenStream = tokens },
    )
    walker.setDexBuilder(DexBuilder(Opcodes.getDefault()))
    return walker.smali_file()
}

private const val SCROLL_MODES_SMALI = """
.class public Lru/fourpda/client/ScrollModes;
.super Ljava/lang/Object;
.implements Landroid/widget/CompoundButton${'$'}OnCheckedChangeListener;

.field private static host:Lru/fourpda/client/I0${'$'}v;
.field private static listener:Lru/fourpda/client/ScrollModes;

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

# 0 — обычный (как раньше), иначе 0 если включён бит 4.
.method public static regularGate(I)I
    .registers 1
    if-eqz p0, :on
    and-int/lit8 p0, p0, 0x4
    if-nez p0, :on
    const/4 p0, 0x1
    return p0
    :on
    const/4 p0, 0x0
    return p0
.end method

.method public static mask1(I)I
    .registers 1
    and-int/lit8 p0, p0, 0x1
    return p0
.end method

.method public static mask2(I)I
    .registers 1
    and-int/lit8 p0, p0, 0x2
    return p0
.end method

.method private static appendName(Landroid/content/res/Resources;Ljava/lang/StringBuilder;I)V
    .registers 5
    const-string v1, ""
    invoke-virtual {p1}, Ljava/lang/StringBuilder;->length()I
    move-result v0
    if-eqz v0, :text
    const-string v1, ", "
    invoke-virtual {p1, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    :text
    invoke-virtual {p0, p2}, Landroid/content/res/Resources;->getString(I)Ljava/lang/String;
    move-result-object v1
    invoke-virtual {p1, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    return-void
.end method

.method public static label(Landroid/content/res/Resources;)Ljava/lang/String;
    .registers 5
    sget v0, Lru/fourpda/client/b1;->x:I
    if-nez v0, :bits
    const/4 v0, 0x4
    :bits
    new-instance v1, Ljava/lang/StringBuilder;
    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V
    and-int/lit8 v2, v0, 0x4
    if-eqz v2, :buttons
    sget v2, Lru/fourpda/client/g1;->L0:I
    invoke-static {p0, v1, v2}, Lru/fourpda/client/ScrollModes;->appendName(Landroid/content/res/Resources;Ljava/lang/StringBuilder;I)V
    :buttons
    and-int/lit8 v2, v0, 0x1
    if-eqz v2, :slider
    sget v2, Lru/fourpda/client/g1;->K0:I
    invoke-static {p0, v1, v2}, Lru/fourpda/client/ScrollModes;->appendName(Landroid/content/res/Resources;Ljava/lang/StringBuilder;I)V
    :slider
    and-int/lit8 v2, v0, 0x2
    if-eqz v2, :done
    sget v2, Lru/fourpda/client/g1;->J0:I
    invoke-static {p0, v1, v2}, Lru/fourpda/client/ScrollModes;->appendName(Landroid/content/res/Resources;Ljava/lang/StringBuilder;I)V
    :done
    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v0
    return-object v0
.end method

.method public static bind(Landroid/view/View;Lru/fourpda/client/I0${'$'}v;)V
    .registers 6
    sput-object p1, Lru/fourpda/client/ScrollModes;->host:Lru/fourpda/client/I0${'$'}v;
    sget-object v0, Lru/fourpda/client/ScrollModes;->listener:Lru/fourpda/client/ScrollModes;
    if-nez v0, :ready
    new-instance v0, Lru/fourpda/client/ScrollModes;
    invoke-direct {v0}, Lru/fourpda/client/ScrollModes;-><init>()V
    sput-object v0, Lru/fourpda/client/ScrollModes;->listener:Lru/fourpda/client/ScrollModes;
    :ready
    sget v1, Lru/fourpda/client/e1;->A3:I
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;
    move-result-object v1
    const/4 v2, 0x4
    invoke-static {v1, v2, v0}, Lru/fourpda/client/ScrollModes;->swap(Landroid/view/View;ILru/fourpda/client/ScrollModes;)V
    sget v1, Lru/fourpda/client/e1;->B3:I
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;
    move-result-object v1
    const/4 v2, 0x1
    invoke-static {v1, v2, v0}, Lru/fourpda/client/ScrollModes;->swap(Landroid/view/View;ILru/fourpda/client/ScrollModes;)V
    sget v1, Lru/fourpda/client/e1;->C3:I
    invoke-virtual {p0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;
    move-result-object v1
    const/4 v2, 0x2
    invoke-static {v1, v2, v0}, Lru/fourpda/client/ScrollModes;->swap(Landroid/view/View;ILru/fourpda/client/ScrollModes;)V
    return-void
.end method

.method private static swap(Landroid/view/View;ILru/fourpda/client/ScrollModes;)V
    .registers 12
    invoke-virtual {p0}, Landroid/view/View;->getParent()Landroid/view/ViewParent;
    move-result-object v0
    check-cast v0, Landroid/view/ViewGroup;
    invoke-virtual {v0, p0}, Landroid/view/ViewGroup;->indexOfChild(Landroid/view/View;)I
    move-result v1
    new-instance v2, Landroid/widget/CheckBox;
    invoke-virtual {p0}, Landroid/view/View;->getContext()Landroid/content/Context;
    move-result-object v3
    invoke-direct {v2, v3}, Landroid/widget/CheckBox;-><init>(Landroid/content/Context;)V
    invoke-virtual {p0}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup${'$'}LayoutParams;
    move-result-object v3
    invoke-virtual {v2, v3}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup${'$'}LayoutParams;)V
    move-object v3, p0
    check-cast v3, Landroid/widget/TextView;
    invoke-virtual {v3}, Landroid/widget/TextView;->getText()Ljava/lang/CharSequence;
    move-result-object v4
    invoke-virtual {v2, v4}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
    const/4 v4, 0x0
    invoke-virtual {v3}, Landroid/widget/TextView;->getTextSize()F
    move-result v5
    invoke-virtual {v2, v4, v5}, Landroid/widget/TextView;->setTextSize(IF)V
    invoke-virtual {v3}, Landroid/widget/TextView;->getTextColors()Landroid/content/res/ColorStateList;
    move-result-object v4
    invoke-virtual {v2, v4}, Landroid/widget/TextView;->setTextColor(Landroid/content/res/ColorStateList;)V
    invoke-virtual {v3}, Landroid/widget/TextView;->getGravity()I
    move-result v4
    invoke-virtual {v2, v4}, Landroid/widget/TextView;->setGravity(I)V
    const/4 v4, 0x1
    invoke-virtual {v2, v4}, Landroid/widget/TextView;->setSingleLine(Z)V
    const/4 v4, 0x0
    invoke-virtual {v2, v4}, Landroid/widget/TextView;->setIncludeFontPadding(Z)V
    invoke-virtual {p0}, Landroid/view/View;->getPaddingLeft()I
    move-result v4
    invoke-virtual {p0}, Landroid/view/View;->getPaddingTop()I
    move-result v5
    invoke-virtual {p0}, Landroid/view/View;->getPaddingRight()I
    move-result v6
    invoke-virtual {p0}, Landroid/view/View;->getPaddingBottom()I
    move-result v7
    invoke-virtual {v2, v4, v5, v6, v7}, Landroid/view/View;->setPadding(IIII)V
    invoke-virtual {p0}, Landroid/view/View;->getBackground()Landroid/graphics/drawable/Drawable;
    move-result-object v4
    invoke-virtual {v2, v4}, Landroid/view/View;->setBackground(Landroid/graphics/drawable/Drawable;)V
    invoke-virtual {v3}, Landroid/widget/TextView;->getCompoundDrawablePadding()I
    move-result v4
    invoke-virtual {v2, v4}, Landroid/widget/TextView;->setCompoundDrawablePadding(I)V
    invoke-virtual {p0}, Landroid/view/View;->getVisibility()I
    move-result v4
    invoke-virtual {v2, v4}, Landroid/view/View;->setVisibility(I)V
    const/4 v4, 0x0
    invoke-virtual {v2, v4}, Landroid/widget/CompoundButton;->setButtonDrawable(Landroid/graphics/drawable/Drawable;)V
    new-instance v5, Landroid/graphics/drawable/StateListDrawable;
    invoke-direct {v5}, Landroid/graphics/drawable/StateListDrawable;-><init>()V
    invoke-virtual {p0}, Landroid/view/View;->getResources()Landroid/content/res/Resources;
    move-result-object v6
    const v4, 0x10100a0
    filled-new-array {v4}, [I
    move-result-object v4
    const v7, 0x7f05006b
    invoke-virtual {v6, v7}, Landroid/content/res/Resources;->getDrawable(I)Landroid/graphics/drawable/Drawable;
    move-result-object v7
    invoke-virtual {v5, v4, v7}, Landroid/graphics/drawable/StateListDrawable;->addState([ILandroid/graphics/drawable/Drawable;)V
    const/4 v4, 0x0
    new-array v4, v4, [I
    const v7, 0x7f05006a
    invoke-virtual {v6, v7}, Landroid/content/res/Resources;->getDrawable(I)Landroid/graphics/drawable/Drawable;
    move-result-object v6
    invoke-virtual {v5, v4, v6}, Landroid/graphics/drawable/StateListDrawable;->addState([ILandroid/graphics/drawable/Drawable;)V
    const/4 v4, 0x0
    invoke-virtual {v2, v5, v4, v4, v4}, Landroid/widget/TextView;->setCompoundDrawablesWithIntrinsicBounds(Landroid/graphics/drawable/Drawable;Landroid/graphics/drawable/Drawable;Landroid/graphics/drawable/Drawable;Landroid/graphics/drawable/Drawable;)V
    sget v4, Lru/fourpda/client/b1;->x:I
    if-nez v4, :checked
    const/4 v4, 0x4
    :checked
    and-int v4, v4, p1
    if-eqz v4, :off
    const/4 v4, 0x1
    goto :mark
    :off
    const/4 v4, 0x0
    :mark
    invoke-virtual {v2, v4}, Landroid/widget/CompoundButton;->setChecked(Z)V
    invoke-static {p1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v4
    invoke-virtual {v2, v4}, Landroid/view/View;->setTag(Ljava/lang/Object;)V
    invoke-virtual {v0, p0}, Landroid/view/ViewGroup;->removeView(Landroid/view/View;)V
    invoke-virtual {v0, v2, v1}, Landroid/view/ViewGroup;->addView(Landroid/view/View;I)V
    invoke-virtual {v2, p2}, Landroid/widget/CompoundButton;->setOnCheckedChangeListener(Landroid/widget/CompoundButton${'$'}OnCheckedChangeListener;)V
    return-void
.end method

.method public onCheckedChanged(Landroid/widget/CompoundButton;Z)V
    .registers 6
    sget v0, Lru/fourpda/client/b1;->x:I
    if-nez v0, :bits
    const/4 v0, 0x4
    :bits
    invoke-virtual {p1}, Landroid/view/View;->getTag()Ljava/lang/Object;
    move-result-object v1
    check-cast v1, Ljava/lang/Integer;
    invoke-virtual {v1}, Ljava/lang/Integer;->intValue()I
    move-result v1
    if-eqz p2, :clear
    or-int/2addr v0, v1
    goto :pack
    :clear
    not-int v1, v1
    and-int/2addr v0, v1
    :pack
    const/4 v1, 0x4
    if-ne v0, v1, :store
    const/4 v0, 0x0
    :store
    sput v0, Lru/fourpda/client/b1;->x:I
    sget-object v1, Lru/fourpda/client/ScrollModes;->host:Lru/fourpda/client/I0${'$'}v;
    iget-object v1, v1, Lru/fourpda/client/I0${'$'}v;->a:Lru/fourpda/client/I0;
    iget-object v1, v1, Lru/fourpda/client/w0;->g:Lru/fourpda/client/MainActivity;
    const-string v2, "scroll_mode"
    invoke-static {v1, v2, v0}, Lru/fourpda/client/b1;->g(Landroid/content/Context;Ljava/lang/String;I)V
    return-void
.end method
"""

val scrollModesPatch = bytecodePatch(
    name = "Combined scroll modes",
    description = "Allows enabling multiple scroll modes simultaneously (standard scroll, scrollbar thumb, and up/down buttons) in main settings.",
    use = false,
) {
    compatibleWith("ru.fourpda.client"("1.9.44_pre7"))

    apply {
        if (classDefs[SCROLL_MODES] == null) {
            classDefs += assemble(SCROLL_MODES_SMALI)
        }

        val list = classDefs[LIST] ?: error("в apk нет списка")
        var gates = 0
        var masks = 0
        for (method in proxy(list).mutableClass.methods) {
            val implementation = method.implementation ?: continue
            val inserts = mutableListOf<Pair<Int, String>>()
            val instructions = implementation.instructions
            instructions.forEachIndexed { index, instruction ->
                val field = instruction.fieldRef() ?: return@forEachIndexed
                if (field.definingClass != "Lru/fourpda/client/b1;" || field.name != "x") return@forEachIndexed
                val mode = (instruction as OneRegisterInstruction).registerA
                val next = instructions.getOrNull(index + 1) ?: return@forEachIndexed
                if (next.opcode == Opcode.IF_NEZ) {
                    val branch = next as BuilderInstruction21t
                    if (branch.registerA != mode) error("проверка обычного режима на другом регистре")
                    inserts += index + 1 to """
                        invoke-static {v$mode}, $SCROLL_MODES->regularGate(I)I
                        move-result v$mode
                    """.trimIndent()
                    gates += 1
                    return@forEachIndexed
                }
                val literal = next.intLiteral()
                val branch = instructions.getOrNull(index + 2)
                if (branch?.opcode != Opcode.IF_NE || (literal != 1 && literal != 2)) return@forEachIndexed
                val compare = branch as BuilderInstruction22t
                val mask = (next as OneRegisterInstruction).registerA
                if (setOf(compare.registerA, compare.registerB) != setOf(mode, mask)) {
                    error("сравнение режима прокрутки не с тем регистром")
                }
                val helper = if (literal == 1) "mask1" else "mask2"
                inserts += index + 2 to """
                    invoke-static {v$mode}, $SCROLL_MODES->$helper(I)I
                    move-result v$mode
                """.trimIndent()
                masks += 1
            }
            for ((at, smali) in inserts.sortedByDescending { it.first }) {
                method.addInstructions(at, smali)
            }
        }
        if (gates != 1 || masks != 2) error("не найдены все три проверки режима прокрутки")

        val settings = classDefs[SETTINGS] ?: error("в apk нет настроек")
        val row = proxy(settings).mutableClass.methods.firstOrNull { method ->
            method.implementation?.instructions?.any { it.stringConst()?.contains("Выбран") == true } == true
        } ?: error("не найдена подпись режима прокрутки")
        val chosen = row.implementation!!.instructions.indexOfFirst { it.stringConst()?.contains("Выбран") == true }
        val chosenRegister = (row.implementation!!.instructions[chosen] as OneRegisterInstruction).registerA
        row.replaceInstruction(chosen, "const-string v$chosenRegister, \"\\nВыбрано: \"")
        val name = (chosen + 1 until row.implementation!!.instructions.size).firstOrNull { index ->
            row.implementation!!.instructions[index].calledMethod()?.name == "getString"
        } ?: error("не найдено имя выбранного режима")
        val resources = (row.implementation!!.instructions[name] as FiveRegisterInstruction).registerC
        row.replaceInstruction(
            name,
            "invoke-static {v$resources}, $SCROLL_MODES->label(Landroid/content/res/Resources;)Ljava/lang/String;",
        )

        val dialog = classDefs[SETTINGS_CLICK] ?: error("в apk нет диалога прокрутки")
        val click = proxy(dialog).mutableClass.methods.single { it.name == "onClick" }
        val listener = click.implementation!!.instructions.indexOfFirst {
            it.calledMethod()?.name == "setOnCheckedChangeListener"
        }
        if (listener < 0) error("не найден выбор режима прокрутки")
        val root = click.implementation!!.instructions
            .take(listener)
            .last { it.calledMethod()?.name == "findViewById" } as FiveRegisterInstruction
        val page = (click.implementation!!.instructions[listener - 1] as FiveRegisterInstruction).registerD
        click.replaceInstruction(
            listener,
            "invoke-static {v${root.registerC}, v$page}, $SCROLL_MODES->bind(Landroid/view/View;${SETTINGS_CLICK})V",
        )
    }
}
