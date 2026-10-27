package app.revanced.patches.fourpda

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val CAP = 500
private const val STEP = 10

private fun com.android.tools.smali.dexlib2.iface.instruction.Instruction.intLiteral(): Int? =
    (this as? WideLiteralInstruction)?.wideLiteral?.toInt()

private fun com.android.tools.smali.dexlib2.iface.instruction.Instruction.calledMethod(): MethodReference? =
    (this as? ReferenceInstruction)?.reference as? MethodReference

private val clickClasses = listOf(
    "Lru/fourpda/client/I0\$w\$a;",
    "Lru/fourpda/client/I0\$w\$b;",
    "Lru/fourpda/client/I0\$E\$a;",
    "Lru/fourpda/client/I0\$E\$b;",
    "Lru/fourpda/client/U0\$b;",
    "Lru/fourpda/client/U0\$c;",
)

private val capMethods = listOf(
    "Lru/fourpda/client/I0;" to "f0",
    "Lru/fourpda/client/I0;" to "h0",
    "Lru/fourpda/client/U0;" to "e0",
)

val pageSizePatch = bytecodePatch(
    name = "Страница по 500",
    description = "В настройках «Количество постов» и «Количество тем», и в меню тикетов «Тикетов на странице»: потолок 500, шаг плюса и минуса 10.",
) {
    compatibleWith("ru.fourpda.client"("1.9.44_pre7"))

    apply {
        for (type in clickClasses) {
            val typeDef = classDefs[type] ?: error("в apk нет $type")
            val click = proxy(typeDef).mutableClass.methods.single { it.name == "onClick" }
            val instructions = click.implementation!!.instructions
            val step = instructions.indexOfFirst { instruction ->
                instruction.opcode == Opcode.CONST_4 && instruction.intLiteral() == 1
            }
            if (step < 0) error("не найден шаг в $type")
            val next = instructions[step + 1].opcode
            if (next != Opcode.ADD_INT_2ADDR && next != Opcode.SUB_INT_2ADDR) {
                error("шаг в $type стоит не перед сложением")
            }
            val register = (instructions[step] as OneRegisterInstruction).registerA
            click.replaceInstruction(step, "const/16 v$register, 0x${STEP.toString(16)}")
            val refresh = click.implementation!!.instructions.indexOfFirst { instruction ->
                instruction.calledMethod()?.name in setOf("f0", "h0", "e0")
            }
            if (refresh < 0) error("не найден пересчёт в $type")
            click.addInstructions(refresh, "const/4 v$register, 0x1")
            val cap = click.implementation!!.instructions.indexOfFirst { it.intLiteral() == 100 }
            if (cap >= 0) {
                val capRegister = (click.implementation!!.instructions[cap] as OneRegisterInstruction).registerA
                click.replaceInstruction(cap, "const/16 v$capRegister, 0x${CAP.toString(16)}")
            }
        }

        for ((type, name) in capMethods) {
            val typeDef = classDefs[type] ?: error("в apk нет $type")
            val method = proxy(typeDef).mutableClass.methods.single { it.name == name }
            val cap = method.implementation!!.instructions.indexOfFirst { it.intLiteral() == 100 }
            if (cap < 0) error("не найден потолок 100 в $type.$name")
            val register = (method.implementation!!.instructions[cap] as OneRegisterInstruction).registerA
            method.replaceInstruction(cap, "const/16 v$register, 0x${CAP.toString(16)}")
        }
    }
}
