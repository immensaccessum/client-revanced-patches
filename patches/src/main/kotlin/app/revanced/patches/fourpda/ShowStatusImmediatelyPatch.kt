package app.revanced.patches.fourpda

import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val MAIN_LAYOUT = "Lru/fourpda/client/MainLayout;"

/** `postDelayed(..., z ? 3000L : 100L)` — обычная загрузка страницы ждёт 3 секунды. */
private const val DELAY_3_SECONDS = 3000L

/**
 * Нижняя строка статуса («Загрузка темы…») рисуется сразу.
 * Текст по-прежнему стирается, когда ответ уже пришёл.
 */
val showStatusImmediatelyPatch = bytecodePatch(
    name = "Show status immediately",
    description = "Показывает нижнюю строку статуса сразу, без паузы в 3 секунды.",
) {
    compatibleWith("ru.fourpda.client")

    apply {
        val mainLayout = classDefs[MAIN_LAYOUT]
            ?: error("в apk нет $MAIN_LAYOUT")

        val mutableMethod = proxy(mainLayout).mutableClass.methods.singleOrNull { method ->
            val instructions = method.implementation?.instructions ?: return@singleOrNull false
            val waitsThreeSeconds = instructions.any { instruction ->
                instruction.opcode == Opcode.CONST_WIDE_16 &&
                    (instruction as? WideLiteralInstruction)?.wideLiteral == DELAY_3_SECONDS
            }
            val postsDelayed = instructions.any { instruction ->
                val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                reference?.name == "postDelayed"
            }
            waitsThreeSeconds && postsDelayed
        } ?: error("в $MAIN_LAYOUT нет const 3000 перед postDelayed")

        val instructions = mutableMethod.implementation!!.instructions
        val index = instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.CONST_WIDE_16 &&
                (instruction as? WideLiteralInstruction)?.wideLiteral == DELAY_3_SECONDS
        }
        check(index >= 0)

        val register = (instructions[index] as OneRegisterInstruction).registerA
        mutableMethod.replaceInstruction(index, "const-wide/16 v$register, 0x0")
    }
}
