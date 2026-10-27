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
        error("ошибка сборки StatusHold")
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

private fun MutableMethod.nopStatusHide() {
    val instructions = implementation!!.instructions
    instructions.forEachIndexed { index, instruction ->
        if (instruction.calledMethod()?.name != "clearAnimation") {
            return@forEachIndexed
        }
        val setTag = instructions.getOrNull(index + 2)
        val setVisibility = instructions.getOrNull(index + 4)
        if (setTag?.calledMethod()?.name == "setTag" && setVisibility?.calledMethod()?.name == "setVisibility") {
            replaceInstruction(index, "nop")
            replaceInstruction(index + 2, "nop")
            replaceInstruction(index + 4, "nop")
        }
    }
}

private const val STATUS_HOLD_SMALI = """
.class public Lru/fourpda/client/StatusHold;
.super Ljava/lang/Object;
.implements Ljava/lang/Runnable;

.field private static instance:Lru/fourpda/client/StatusHold;
.field private static view:Landroid/widget/TextView;

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static apply(Lru/fourpda/client/MainLayout;Landroid/widget/TextView;Ljava/lang/CharSequence;)V
    .registers 6
    sget-object v0, Lru/fourpda/client/StatusHold;->instance:Lru/fourpda/client/StatusHold;
    if-nez v0, :ready
    new-instance v0, Lru/fourpda/client/StatusHold;
    invoke-direct {v0}, Lru/fourpda/client/StatusHold;-><init>()V
    sput-object v0, Lru/fourpda/client/StatusHold;->instance:Lru/fourpda/client/StatusHold;
    :ready
    invoke-virtual {p0, v0}, Landroid/view/View;->removeCallbacks(Ljava/lang/Runnable;)Z
    invoke-interface {p2}, Ljava/lang/CharSequence;->length()I
    move-result v1
    if-gtz v1, :empty
    invoke-virtual {p1, p2}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
    return-void
    :empty
    sput-object p1, Lru/fourpda/client/StatusHold;->view:Landroid/widget/TextView;
    const-wide/16 v1, 0x3e8
    invoke-virtual {p0, v0, v1, v2}, Landroid/view/View;->postDelayed(Ljava/lang/Runnable;J)Z
    return-void
.end method

.method public run()V
    .registers 3
    sget-object v0, Lru/fourpda/client/StatusHold;->view:Landroid/widget/TextView;
    if-eqz v0, :done
    const-string v1, ""
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
    invoke-virtual {v0}, Landroid/view/View;->clearAnimation()V
    const/4 v1, 0x0
    invoke-virtual {v0, v1}, Landroid/view/View;->setTag(Ljava/lang/Object;)V
    const/4 v1, 0x4
    invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
    const/4 v0, 0x0
    sput-object v0, Lru/fourpda/client/StatusHold;->view:Landroid/widget/TextView;
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
        statusMethod.nopStatusHide()

        val setTextIndex = statusMethod.implementation!!.instructions.indexOfFirst { instruction ->
            val called = instruction.calledMethod()
            called?.name == "setText" && called.definingClass == "Landroid/widget/TextView;"
        }
        if (setTextIndex >= 0) {
            statusMethod.replaceInstruction(
                setTextIndex,
                "invoke-static {p0, v2, v0}, $STATUS_HOLD->apply(Lru/fourpda/client/MainLayout;Landroid/widget/TextView;Ljava/lang/CharSequence;)V",
            )
        }

        mutableClass.methods.firstOrNull { it.name == "run" && it.parameterTypes.isEmpty() }?.let { runMethod ->
            runMethod.setImplementation(MutableMethodImplementation(1))
            (runMethod.implementation as MutableMethodImplementation).addInstructions(
                "return-void".toInstructions(runMethod),
            )
        }

        if (classDefs[STATUS_HOLD] == null) {
            classDefs += assemble(STATUS_HOLD_SMALI)
        }
    }
}
