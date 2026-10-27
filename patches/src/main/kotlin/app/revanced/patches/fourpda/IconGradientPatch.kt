package app.revanced.patches.fourpda

import app.revanced.patcher.patch.resourcePatch
import org.w3c.dom.Element

private const val ROUND_ICON = "res/mipmap-anydpi-v26/ic_launcher_round.xml"
private const val BLUE = "#ff3861a8"

val iconGradientPatch = resourcePatch(
    name = "Градиент иконки",
    description = "Синий фон круглой иконки заменён на градиент ReVanced: сверху розовый, в середине фиолетовый, снизу голубой. Белый знак остаётся.",
) {
    compatibleWith("ru.fourpda.client"("1.9.44_pre7"))

    apply {
        document(ROUND_ICON).use { icon ->
            val plates = icon.getElementsByTagName("solid")
            if (plates.length != 1) error("у круглой иконки не одна заливка фона")
            val plate = plates.item(0) as Element
            val color = plate.getAttribute("android:color").lowercase()
            if (color != BLUE) error("фон круглой иконки не синий: $color")
            val shape = plate.parentNode
            val gradient = icon.createElement("gradient").apply {
                setAttribute("android:type", "linear")
                setAttribute("android:angle", "270")
                setAttribute("android:startColor", "#fff04e98")
                setAttribute("android:centerColor", "#ff5f65d4")
                setAttribute("android:endColor", "#ff4e98f0")
            }
            shape.replaceChild(gradient, plate)
        }
    }
}
