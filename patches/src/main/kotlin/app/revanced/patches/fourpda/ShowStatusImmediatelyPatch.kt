package app.revanced.patches.fourpda

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.removeInstructions
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.extensions.toInstructions
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderInstruction
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21t
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val MAIN_LAYOUT = "Lru/fourpda/client/MainLayout;"
private const val RUNNABLE = "Ljava/lang/Runnable;"
private const val STATUS_FIELD = "Lru/fourpda/client/MainLayout;->b:Landroid/widget/TextView;"
private const val DELAY_3_SECONDS = 3000L
private const val HOLD_MS = 1000L
private const val HOLD_TAG = 2

private fun Instruction.calledMethod(): MethodReference? =
    (this as? ReferenceInstruction)?.reference as? MethodReference

private fun MutableMethod.stripBrokenHold() {
    val instructions = implementation!!.instructions
    val constIndex = instructions.indexOfFirst { instruction ->
        instruction.opcode == Opcode.CONST_WIDE_16 &&
            (instruction as? WideLiteralInstruction)?.wideLiteral == HOLD_MS
    }
    if (constIndex < 0) {
        return
    }
    val legacy = instructions
        .subList((constIndex - 8).coerceAtLeast(0), constIndex)
        .any { instruction ->
            instruction.opcode == Opcode.CONST_4 &&
                (instruction as? NarrowLiteralInstruction)?.narrowLiteral == HOLD_TAG
        }
    if (!legacy) {
        return
    }
    val start = (constIndex downTo 0).first { index ->
        instructions[index].calledMethod()?.name == "length"
    }
    val end = (constIndex until instructions.size).first { index ->
        instructions[index].opcode == Opcode.RETURN_VOID
    }
    removeInstructions(start, end - start + 1)
}

/**
 * Прошлая сборка перепутала две метки: пустой тег вёл в выход, и строка не показывалась.
 * Здесь возвращается прежний смысл перехода.
 */
private fun MutableMethod.repairSwappedShowBranch() {
    val instructions = implementation!!.instructions
    val index = instructions.indices.firstOrNull { candidate ->
        val branch = instructions[candidate]
        branch.opcode == Opcode.IF_EQZ &&
            instructions.getOrNull(candidate + 1)?.opcode == Opcode.IGET_OBJECT &&
            instructions.getOrNull(candidate + 2)?.calledMethod()?.name == "valueOf" &&
            instructions.getOrNull(candidate + 5)?.opcode == Opcode.GOTO_16
    } ?: return
    val branch = instructions[index] as BuilderInstruction21t
    replaceInstruction(
        index,
        BuilderInstruction21t(Opcode.IF_NEZ, branch.registerA, branch.target),
    )
}

val showStatusImmediatelyPatch = bytecodePatch(
    name = "Show status immediately",
    description = "Нижняя строка статуса появляется сразу и остаётся ещё секунду после загрузки.",
) {
    compatibleWith("ru.fourpda.client")

    apply {
        val mainLayout = classDefs[MAIN_LAYOUT]
            ?: error("в apk нет $MAIN_LAYOUT")
        val mutableClass = proxy(mainLayout).mutableClass
        val statusMethod = mutableClass.methods.singleOrNull { method ->
            method.implementation?.instructions?.any { it.calledMethod()?.name == "postDelayed" } == true &&
                method.implementation?.instructions?.any { it.calledMethod()?.name == "setText" } == true
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

        statusMethod.stripBrokenHold()
        statusMethod.repairSwappedShowBranch()

        val alreadyHolds = statusMethod.implementation!!.instructions.any { instruction ->
            instruction.calledMethod()?.name == "removeCallbacks"
        }
        if (!alreadyHolds) {
            val instructions = statusMethod.implementation!!.instructions
            val setTextIndex = instructions.withIndex().first { (index, instruction) ->
                val called = instruction.calledMethod()
                val next = instructions.getOrNull(index + 1)?.calledMethod()
                called?.name == "setText" &&
                    next?.name == "length" &&
                    next.definingClass == "Landroid/text/SpannableStringBuilder;"
            }.index
            statusMethod.addInstructionsWithLabels(
                setTextIndex,
                """
                    invoke-virtual {p0, p0}, Landroid/view/View;->removeCallbacks(Ljava/lang/Runnable;)Z
                    invoke-virtual {v0}, Landroid/text/SpannableStringBuilder;->length()I
                    move-result v3
                    if-lez v3, :write_text
                    const-wide/16 v7, 0x${HOLD_MS.toString(16)}
                    invoke-virtual {p0, p0, v7, v8}, Landroid/view/View;->postDelayed(Ljava/lang/Runnable;J)Z
                    return-void
                """,
                ExternalLabel("write_text", instructions[setTextIndex] as BuilderInstruction),
            )
        }

        val runMethod = mutableClass.methods.firstOrNull { method ->
            method.name == "run" && method.parameterTypes.isEmpty()
        } ?: statusMethod.toMutable().also { method ->
            method.setName("run")
            method.setAccessFlags(AccessFlags.PUBLIC.value)
            method.setReturnType("V")
            (method.parameterTypes as MutableList<CharSequence>).clear()
            (method.parameters as MutableList<*>).clear()
            (mutableClass.methods as MutableSet<Method>).add(method)
            val interfaces = mutableClass.interfaces as MutableList<String>
            if (RUNNABLE !in interfaces) {
                interfaces.add(RUNNABLE)
            }
        }
        runMethod.setImplementation(MutableMethodImplementation(3))
        val hide = """
            iget-object v0, p0, $STATUS_FIELD
            const-string v1, ""
            invoke-virtual {v0, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
            invoke-virtual {v0}, Landroid/view/View;->clearAnimation()V
            const/4 v1, 0x0
            invoke-virtual {v0, v1}, Landroid/view/View;->setTag(Ljava/lang/Object;)V
            const/4 v1, 0x4
            invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
            return-void
        """.toInstructions(runMethod)
        (runMethod.implementation as MutableMethodImplementation).addInstructions(hide)
    }
}
