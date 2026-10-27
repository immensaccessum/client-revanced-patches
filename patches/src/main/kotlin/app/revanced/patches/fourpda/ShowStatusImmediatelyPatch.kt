package app.revanced.patches.fourpda

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.removeInstruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.extensions.toInstructions
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderInstruction
import com.android.tools.smali.dexlib2.builder.BuilderOffsetInstruction
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val MAIN_LAYOUT = "Lru/fourpda/client/MainLayout;"
private const val RUNNABLE = "Ljava/lang/Runnable;"
private const val STATUS_FIELD = "Lru/fourpda/client/MainLayout;->b:Landroid/widget/TextView;"

/** Обычная загрузка страницы ждёт 3 секунды, прежде чем показать строку. */
private const val DELAY_3_SECONDS = 3000L

/** Сколько строка ещё видна после того, как загрузка уже кончилась. */
private const val HOLD_MS = 1000

private const val HOLD_TAG = 2

private fun MethodReference.isPostDelayed() = name == "postDelayed"

private fun Method.calls(name: String) = implementation?.instructions?.any { instruction ->
    ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.name == name
} == true

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
            method.calls("postDelayed") && method.calls("setText") && method.calls("length")
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

        if (mutableClass.methods.any { it.name == "run" && it.parameterTypes.isEmpty() }) {
            return@apply
        }

        val instructions = statusMethod.implementation!!.instructions
        val setTextIndex = instructions.withIndex().first { (index, instruction) ->
            val called = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            val next = (instructions.getOrNull(index + 1) as? ReferenceInstruction)?.reference as? MethodReference
            called?.name == "setText" &&
                next?.name == "length" &&
                next.definingClass == "Landroid/text/SpannableStringBuilder;"
        }.index

        val branchIndex = instructions.withIndex().first { (index, instruction) ->
            if (instruction.opcode != Opcode.IF_NEZ) {
                return@first false
            }
            val target = (instruction as BuilderOffsetInstruction).target.location.instruction
            val targetIndex = instructions.indexOf(target)
            targetIndex > index && instructions.subList(index, targetIndex).any { candidate ->
                ((candidate as? ReferenceInstruction)?.reference as? MethodReference)?.isPostDelayed() == true
            }
        }.index
        val branch = instructions[branchIndex] as BuilderOffsetInstruction
        val tagRegister = (branch as OneRegisterInstruction).registerA
        val showSetup = instructions[branchIndex + 1] as BuilderInstruction
        val hideDone = branch.target.location.instruction as BuilderInstruction

        statusMethod.removeInstruction(branchIndex)
        statusMethod.addInstructionsWithLabels(
            branchIndex,
            """
                if-eqz v$tagRegister, :show_setup
                iget-object v$tagRegister, p0, $STATUS_FIELD
                invoke-static {v6}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
                move-result-object v2
                invoke-virtual {v$tagRegister, v2}, Landroid/view/View;->setTag(Ljava/lang/Object;)V
                goto/16 :done
            """,
            ExternalLabel("show_setup", showSetup),
            ExternalLabel("done", hideDone),
        )

        statusMethod.addInstructionsWithLabels(
            setTextIndex,
            """
                invoke-virtual {v0}, Landroid/text/SpannableStringBuilder;->length()I
                move-result v3
                if-lez v3, :write_text
                iget-object v2, p0, $STATUS_FIELD
                const/4 v3, 0x$HOLD_TAG
                invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
                move-result-object v3
                invoke-virtual {v2, v3}, Landroid/view/View;->setTag(Ljava/lang/Object;)V
                const-wide/16 v7, 0x${HOLD_MS.toString(16)}
                invoke-virtual {p0, p0, v7, v8}, Landroid/view/View;->postDelayed(Ljava/lang/Runnable;J)Z
                return-void
            """,
            ExternalLabel("write_text", instructions[setTextIndex] as BuilderInstruction),
        )

        val runnable = statusMethod.toMutable().also { method ->
            method.setName("run")
            method.setAccessFlags(AccessFlags.PUBLIC.value)
            method.setReturnType("V")
            (method.parameterTypes as MutableList<CharSequence>).clear()
            (method.parameters as MutableList<*>).clear()
            method.setImplementation(MutableMethodImplementation(4))
        }
        val body = """
            iget-object v0, p0, $STATUS_FIELD
            invoke-virtual {v0}, Landroid/view/View;->getTag()Ljava/lang/Object;
            move-result-object v1
            const/4 v2, 0x$HOLD_TAG
            invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
            move-result-object v2
            invoke-virtual {v2, v1}, Ljava/lang/Integer;->equals(Ljava/lang/Object;)Z
            move-result v1
            if-eqz v1, :keep
            const-string v1, ""
            invoke-virtual {v0, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
            invoke-virtual {v0}, Landroid/view/View;->clearAnimation()V
            const/4 v1, 0x0
            invoke-virtual {v0, v1}, Landroid/view/View;->setTag(Ljava/lang/Object;)V
            const/4 v1, 0x4
            invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
            :keep
            return-void
        """.toInstructions(runnable)
        (runnable.implementation as MutableMethodImplementation).addInstructions(body)
        (mutableClass.methods as MutableSet<Method>).add(runnable)
        val interfaces = mutableClass.interfaces as MutableList<String>
        if (RUNNABLE !in interfaces) {
            interfaces.add(RUNNABLE)
        }
    }
}
