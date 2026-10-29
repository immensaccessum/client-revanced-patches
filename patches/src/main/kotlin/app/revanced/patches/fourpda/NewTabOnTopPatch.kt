package app.revanced.patches.fourpda

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.removeInstructions
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val TAB = "Lru/fourpda/client/l1;"
private const val LAYOUT = "Lru/fourpda/client/MainLayout;"
private const val RES_IDS = "Lru/fourpda/client/e1;"

private fun com.android.tools.smali.dexlib2.iface.instruction.Instruction.calledMethod(): MethodReference? =
    (this as? ReferenceInstruction)?.reference as? MethodReference

private fun com.android.tools.smali.dexlib2.iface.instruction.Instruction.field(): FieldReference? =
    (this as? ReferenceInstruction)?.reference as? FieldReference

val newTabOnTopPatch = bytecodePatch(
    name = "Новая вкладка сверху",
    description = "В левой шторке новая вкладка появляется сразу под «Открытые вкладки», а не в конце списка.",
) {
    compatibleWith("ru.fourpda.client"("1.9.44_pre7"))

    apply {
        val tab = classDefs[TAB] ?: error("в apk нет вкладки")
        val setup = proxy(tab).mutableClass.methods.firstOrNull { method ->
            method.name == "<init>" && method.implementation?.instructions?.any { instruction ->
                instruction.field()?.let { it.definingClass == LAYOUT && it.name == "h" } == true
            } == true
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
        val call = instructions[insert] as FiveRegisterInstruction
        val drawerRegister = call.registerC
        val rowRegister = call.registerD
        val indexRegister = 4
        if (
            drawerRegister > 15 || rowRegister > 15 ||
            drawerRegister == indexRegister || rowRegister == indexRegister
        ) {
            error("строка вкладки попала в широкий регистр")
        }
        setup.removeInstructions(insert, 1)
        setup.addInstructions(
            insert,
            """
            sget v$indexRegister, $RES_IDS->p3:I
            invoke-virtual {v$drawerRegister, v$indexRegister}, Landroid/view/ViewGroup;->findViewById(I)Landroid/view/View;
            move-result-object v$indexRegister
            if-eqz v$indexRegister, :cond_no_tickets
            const/16 v$indexRegister, 0x8
            goto :goto_insert
            :cond_no_tickets
            const/4 v$indexRegister, 0x7
            :goto_insert
            invoke-virtual {v$drawerRegister, v$rowRegister, v$indexRegister}, Landroid/view/ViewGroup;->addView(Landroid/view/View;I)V
            """.trimIndent(),
        )
    }
}
