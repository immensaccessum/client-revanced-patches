package app.revanced.patches.fourpda

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element
import org.w3c.dom.Node

private const val QMS_PAGE = "Lru/fourpda/client/M0;"
private const val TAG_BAR = "Lru/fourpda/client/s;"

private fun Instruction.calledMethod(): MethodReference? =
    (this as? ReferenceInstruction)?.reference as? MethodReference

private fun org.w3c.dom.Document.tagButton(id: String, drawable: String): Element =
    createElement("Button").apply {
        setAttribute("android:id", "@id/$id")
        setAttribute("android:background", "@drawable/$drawable")
        setAttribute("android:layout_width", "42.0dip")
        setAttribute("android:layout_height", "fill_parent")
    }

private fun org.w3c.dom.Document.findButton(id: String): Element {
    val buttons = getElementsByTagName("Button")
    for (index in 0 until buttons.length) {
        val button = buttons.item(index) as Element
        val value = button.getAttribute("android:id")
        if (value == "@id/$id" || value == "@+id/$id") return button
    }
    error("в панели QMS нет кнопки $id")
}

private fun Node.insertAfter(vararg nodes: Node) {
    var anchor = this
    val parent = parentNode
    for (node in nodes) {
        parent.insertBefore(node, anchor.nextSibling)
        anchor = node
    }
}

private val qmsTagDialogsPatch = bytecodePatch {
    compatibleWith("ru.fourpda.client")

    apply {
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
        val flag = (instructions[init] as FiveRegisterInstruction).registerG
        if (flag > 15) error("флаг панели тегов QMS в регистре v$flag")
        setup.addInstructions(init, "const/4 v$flag, 0x1")
    }
}

val qmsTagsPatch = resourcePatch(
    name = "Теги QMS",
    description = "В панели тегов QMS: размер, цвет, ссылка, списки, спойлер, выравнивание, индекс и оффтоп. Скрытие и метки модераторов не добавляются.",
) {
    dependsOn(qmsTagDialogsPatch)
    compatibleWith("ru.fourpda.client")

    apply {
        document("res/layout/qmsreplyform.xml").use { layout ->
            if (layout.getElementsByTagName("Button").let { buttons ->
                    (0 until buttons.length).any { index ->
                        (buttons.item(index) as Element).getAttribute("android:id").endsWith("editor_Size")
                    }
                }
            ) {
                return@use
            }
            layout.findButton("editor_Strike").insertAfter(
                layout.tagButton("editor_Size", "tag_size"),
                layout.tagButton("editor_Color", "tag_color"),
                layout.tagButton("editor_Link", "tag_link"),
                layout.tagButton("editor_List", "tag_list"),
                layout.tagButton("editor_ListNumder", "tag_list_number"),
            )
            layout.findButton("editor_Quote").insertAfter(
                layout.tagButton("editor_Spoiler", "tag_spoiler"),
                layout.tagButton("editor_SpoilerNamed", "tag_spoiler_named"),
            )
            layout.findButton("editor_Code").insertAfter(
                layout.tagButton("editor_Left", "tag_left"),
                layout.tagButton("editor_Center", "tag_center"),
                layout.tagButton("editor_Right", "tag_right"),
                layout.tagButton("editor_Sub", "tag_sub"),
                layout.tagButton("editor_Sup", "tag_sup"),
                layout.tagButton("editor_Offtop", "tag_offtop"),
            )
        }
    }
}
