package app.revanced.patches.fourpda

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.removeInstructions
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.extensions.toInstructions
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21t
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.writer.builder.DexBuilder
import com.android.tools.smali.smali.smaliFlexLexer
import com.android.tools.smali.smali.smaliParser
import com.android.tools.smali.smali.smaliTreeWalker
import org.antlr.runtime.CommonTokenStream
import org.antlr.runtime.TokenSource
import org.antlr.runtime.tree.CommonTreeNodeStream
import java.io.StringReader

private const val MAIN_LAYOUT = "Lru/fourpda/client/MainLayout;"
private const val STATUS_HOLD = "Lru/fourpda/client/StatusHold;"
private const val STATUS_LINE = "Lru/fourpda/client/StatusLine;"
private const val DELAY_3_SECONDS = 3000L
private const val HOLD_MS = 1000L

private fun Instruction.calledMethod(): MethodReference? =
    (this as? ReferenceInstruction)?.reference as? MethodReference

private fun assemble(smali: String): ClassDef {
    val reader = StringReader(smali)
    val lexer = smaliFlexLexer(reader, 15)
    val tokens = CommonTokenStream(lexer as TokenSource)
    val parser = smaliParser(tokens)
    val fileTree = parser.smali_file()
    if (lexer.numberOfSyntaxErrors > 0 || parser.numberOfSyntaxErrors > 0) {
        error("ошибка сборки StatusLine")
    }
    val walker = smaliTreeWalker(
        CommonTreeNodeStream(fileTree.tree).apply { tokenStream = tokens },
    )
    walker.setDexBuilder(DexBuilder(Opcodes.getDefault()))
    return walker.smali_file()
}

/** Удаляет ранний return, из-за которого текст статуса вообще не ставился. */
private fun MutableMethod.stripEarlyReturn() {
    while (true) {
        val instructions = implementation!!.instructions
        val constIndex = instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.CONST_WIDE_16 &&
                (instruction as? WideLiteralInstruction)?.wideLiteral == HOLD_MS &&
                instructions.getOrNull(instructions.indexOf(instruction) + 1)?.calledMethod()?.name == "postDelayed" &&
                instructions.getOrNull(instructions.indexOf(instruction) + 2)?.opcode == Opcode.RETURN_VOID
        }
        if (constIndex < 0) {
            return
        }
        val start = (constIndex downTo (constIndex - 12).coerceAtLeast(0)).firstOrNull { index ->
            val name = instructions[index].calledMethod()?.name
            name == "removeCallbacks" || name == "length"
        } ?: constIndex
        removeInstructions(start, constIndex + 3 - start)
    }
}

/** Пустой тег в сломанной сборке вёл сразу в return. Возвращаем прежний смысл. */
private fun MutableMethod.repairShowBranch() {
    val instructions = implementation!!.instructions
    val index = instructions.indexOfFirst { instruction ->
        instruction.opcode == Opcode.IF_EQZ &&
            (instruction as? BuilderOffsetInstruction)?.target?.location?.instruction?.opcode == Opcode.RETURN_VOID
    }
    if (index < 0) {
        return
    }
    val branch = instructions[index] as BuilderInstruction21t
    replaceInstruction(index, BuilderInstruction21t(Opcode.IF_NEZ, branch.registerA, branch.target))
}

/**
 * 1.0.7 заменил скрытие на nop, поэтому полоска оставалась на экране.
 * Возвращаем очистку анимации и тега, а само скрытие откладываем.
 */
private fun MutableMethod.restoreNopedHide() {
    val instructions = implementation!!.instructions
    instructions.forEachIndexed { index, instruction ->
        if (instruction.opcode != Opcode.RETURN_VOID || index < 6) {
            return@forEachIndexed
        }
        val nops = listOf(index - 5, index - 3, index - 1).all { instructions[it].opcode == Opcode.NOP }
        val loads = listOf(index - 6, index - 4, index - 2).all { instructions[it].opcode == Opcode.IGET_OBJECT }
        if (!nops || !loads) {
            return@forEachIndexed
        }
        replaceInstruction(index - 5, "invoke-virtual {v0}, Landroid/view/View;->clearAnimation()V")
        replaceInstruction(index - 3, "invoke-virtual {v0, v4}, Landroid/view/View;->setTag(Ljava/lang/Object;)V")
        replaceInstruction(index - 1, "invoke-static {v0}, $STATUS_LINE->hideLater(Landroid/widget/TextView;)V")
    }
}

/** Прямое скрытие в конце загрузки меняем на отложенное. Ветка ошибки остаётся мгновенной. */
private fun MutableMethod.delayStatusHide() {
    val instructions = implementation!!.instructions
    instructions.forEachIndexed { index, instruction ->
        if (instruction.calledMethod()?.name != "clearAnimation") {
            return@forEachIndexed
        }
        val setTag = instructions.getOrNull(index + 2)
        val setVisibility = instructions.getOrNull(index + 4) ?: return@forEachIndexed
        val after = instructions.getOrNull(index + 5)
        if (
            setTag?.calledMethod()?.name != "setTag" ||
            setVisibility.calledMethod()?.name != "setVisibility" ||
            after?.opcode != Opcode.RETURN_VOID
        ) {
            return@forEachIndexed
        }
        val view = (setVisibility as FiveRegisterInstruction).registerC
        replaceInstruction(
            index + 4,
            "invoke-static {v$view}, $STATUS_LINE->hideLater(Landroid/widget/TextView;)V",
        )
    }
}

private const val STATUS_LINE_SMALI = """
.class public Lru/fourpda/client/StatusLine;
.super Ljava/lang/Object;
.implements Ljava/lang/Runnable;

.field private static instance:Lru/fourpda/client/StatusLine;
.field private static view:Landroid/widget/TextView;

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

# Текст пишем только если он не пустой. Пустую строку не ставим: иначе секунду висит голая полоска.
.method public static keep(Landroid/widget/TextView;Ljava/lang/CharSequence;)V
    .registers 4
    invoke-interface {p1}, Ljava/lang/CharSequence;->length()I
    move-result v0
    if-lez v0, :empty
    sget-object v0, Lru/fourpda/client/StatusLine;->instance:Lru/fourpda/client/StatusLine;
    if-eqz v0, :write
    invoke-virtual {p0, v0}, Landroid/view/View;->removeCallbacks(Ljava/lang/Runnable;)Z
    :write
    invoke-virtual {p0, p1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
    :empty
    return-void
.end method

.method public static hideLater(Landroid/widget/TextView;)V
    .registers 4
    sget-object v0, Lru/fourpda/client/StatusLine;->instance:Lru/fourpda/client/StatusLine;
    if-nez v0, :ready
    new-instance v0, Lru/fourpda/client/StatusLine;
    invoke-direct {v0}, Lru/fourpda/client/StatusLine;-><init>()V
    sput-object v0, Lru/fourpda/client/StatusLine;->instance:Lru/fourpda/client/StatusLine;
    :ready
    sput-object p0, Lru/fourpda/client/StatusLine;->view:Landroid/widget/TextView;
    const-wide/16 v1, 0x3e8
    invoke-virtual {p0, v0, v1, v2}, Landroid/view/View;->postDelayed(Ljava/lang/Runnable;J)Z
    return-void
.end method

.method public run()V
    .registers 3
    sget-object v0, Lru/fourpda/client/StatusLine;->view:Landroid/widget/TextView;
    if-eqz v0, :done
    const-string v1, ""
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
    invoke-virtual {v0}, Landroid/view/View;->clearAnimation()V
    const/4 v1, 0x0
    invoke-virtual {v0, v1}, Landroid/view/View;->setTag(Ljava/lang/Object;)V
    const/4 v1, 0x4
    invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
    const/4 v0, 0x0
    sput-object v0, Lru/fourpda/client/StatusLine;->view:Landroid/widget/TextView;
    :done
    return-void
.end method
"""

val showStatusImmediatelyPatch = bytecodePatch(
    name = "Show status immediately",
    description = "Нижняя строка статуса появляется сразу и остаётся ещё секунду после загрузки.",
) {
    compatibleWith("ru.fourpda.client")

    apply {
        val mainLayout = classDefs[MAIN_LAYOUT] ?: error("в apk нет $MAIN_LAYOUT")
        val mutableClass = proxy(mainLayout).mutableClass
        val statusMethod = mutableClass.methods.singleOrNull { method ->
            method.implementation?.instructions?.any { it.calledMethod()?.name == "postDelayed" } == true &&
                method.implementation?.instructions?.any {
                    val called = it.calledMethod()
                    called?.name == "setText" || called?.name == "apply"
                } == true
        } ?: error("не найден метод строки статуса")

        statusMethod.implementation!!.instructions.forEachIndexed { index, instruction ->
            if (
                instruction.opcode == Opcode.CONST_WIDE_16 &&
                (instruction as? WideLiteralInstruction)?.wideLiteral == DELAY_3_SECONDS
            ) {
                val register = (instruction as OneRegisterInstruction).registerA
                statusMethod.replaceInstruction(index, "const-wide/16 v$register, 0x0")
            }
        }

        statusMethod.stripEarlyReturn()
        statusMethod.repairShowBranch()
        statusMethod.restoreNopedHide()
        statusMethod.delayStatusHide()

        val textWrite = statusMethod.implementation!!.instructions.indexOfFirst { instruction ->
            val called = instruction.calledMethod() ?: return@indexOfFirst false
            (called.name == "setText" && called.definingClass == "Landroid/widget/TextView;") ||
                (called.name == "apply" && called.definingClass == STATUS_HOLD)
        }
        if (textWrite >= 0) {
            val invoke = statusMethod.implementation!!.instructions[textWrite] as FiveRegisterInstruction
            val called = statusMethod.implementation!!.instructions[textWrite].calledMethod()!!
            val view = if (called.name == "apply") invoke.registerD else invoke.registerC
            val text = if (called.name == "apply") invoke.registerE else invoke.registerD
            statusMethod.replaceInstruction(
                textWrite,
                "invoke-static {v$view, v$text}, $STATUS_LINE->keep(Landroid/widget/TextView;Ljava/lang/CharSequence;)V",
            )
        }

        mutableClass.methods.firstOrNull { it.name == "run" && it.parameterTypes.isEmpty() }?.let { runMethod ->
            runMethod.setImplementation(MutableMethodImplementation(1))
            (runMethod.implementation as MutableMethodImplementation).addInstructions(
                "return-void".toInstructions(runMethod),
            )
        }

        if (classDefs[STATUS_LINE] == null) {
            classDefs += assemble(STATUS_LINE_SMALI)
        }
    }
}
