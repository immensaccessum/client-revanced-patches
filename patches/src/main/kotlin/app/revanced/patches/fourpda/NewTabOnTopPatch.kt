package app.revanced.patches.fourpda

import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val TAB = "Lru/fourpda/client/l1;"
private const val LAYOUT = "Lru/fourpda/client/MainLayout;"

private fun com.android.tools.smali.dexlib2.iface.instruction.Instruction.calledMethod(): MethodReference? =
    (this as? ReferenceInstruction)?.reference as? MethodReference

private fun com.android.tools.smali.dexlib2.iface.instruction.Instruction.field(): FieldReference? =
    (this as? ReferenceInstruction)?.reference as? FieldReference

val newTabOnTopPatch = bytecodePatch(
    name = "Новая вкладка сверху",
    description = "В левой шторке новая вкладка появляется в начале списка.",
) {
    compatibleWith("ru.fourpda.client"("1.9.44_pre7"))

    apply {
        val tab = classDefs[TAB] ?: error("в apk нет вкладки")
        val setup = proxy(tab).mutableClass.methods.firstOrNull { method ->
            method.implementation?.instructions?.any { instruction ->
                instruction.field()?.let { it.definingClass == LAYOUT && it.name == "h" } == true &&
                    instruction.calledMethod() == null
            } == true && method.name == "<init>"
        } ?: error("не найден конструктор вкладки")
        val instructions = setup.implementation!!.instructions
        val insert = instructions.indices.firstOrNull { index ->
            if (index < 2) return@firstOrNull false
            val called = instructions[index].calledMethod() ?: return@firstOrNull false
            val drawer = instructions[index - 2].field()
            val row = instructions[index - 1].field()
            called.name == "addView" && called.parameterTypes.size == 1 &&
                drawer?.definingClass == LAYOUT && drawer.name == "h" &&
                row?.definingClass == TAB && row.name == "k"
        } ?: error("не найдена вставка строки шторки")
        val drawer = instructions[insert - 2].field()
        val row = instructions[insert - 1].field()
        if (drawer?.definingClass != LAYOUT || drawer.name != "h" || row?.definingClass != TAB || row.name != "k") {
            error("строка шторки стоит не на списке вкладок")
        }
        val call = instructions[insert] as FiveRegisterInstruction
        val index = instructions.take(insert).indexOfLast { instruction ->
            val called = instruction.calledMethod() ?: return@indexOfLast false
            called.name == "addView" && called.parameterTypes.size == 2
        }
        if (index < 0) error("не найден ноль для верха списка")
        val zero = (instructions[index] as FiveRegisterInstruction).registerE
        if (zero > 15 || call.registerC > 15 || call.registerD > 15) {
            error("список вкладок попал в широкий регистр")
        }
        setup.replaceInstruction(
            insert,
            "invoke-virtual {v${call.registerC}, v${call.registerD}, v$zero}, " +
                "Landroid/view/ViewGroup;->addView(Landroid/view/View;I)V",
        )
    }
}
