package app.revanced.patches.fourpda

import app.revanced.patcher.patch.resourcePatch
import org.w3c.dom.Element

private const val COLORS = "res/values/colors.xml"

private val accents = mapOf(
    "brend" to ("#ff1976d2" to "#ff5f65d4"),
    "brend_alpha" to ("#401976d2" to "#405f65d4"),
    "status_bar" to ("#ff125799" to "#ff3c428e"),
    "button_text" to ("#ff1976d2" to "#ff5f65d4"),
    "start_btn_text" to ("#ff1976d2" to "#ff5f65d4"),
    "bb_link" to ("#ff0072bc" to "#ff5f65d4"),
    "pressed" to ("#ff7bbff7" to "#ffc9cbf2"),
    "refresh_arrow_2" to ("#ff1976d2" to "#ff5f65d4"),
    "comments_count_bg" to ("#ff3eaad6" to "#ff5f65d4"),
    "bb_selection_background" to ("#ff2eabdd" to "#ff5f65d4"),
    "skin_keyword_on_bg" to ("#ff7bbff7" to "#ffc9cbf2"),
    "search_post_title" to ("#ffbbdefb" to "#ffd4d6f5"),
    "progress_image" to ("#ff1976d2" to "#ff5f65d4"),
    "progress_qms" to ("#ff1976d2" to "#ff5f65d4"),
    "progress_done" to ("#ff0000ff" to "#ff5f65d4"),
    "zn_brend" to ("#ff0e2e49" to "#ff2c2f66"),
    "zn_brend_alpha" to ("#400e2e49" to "#402c2f66"),
    "zn_status_bar" to ("#ff0a2235" to "#ff1e2048"),
    "zn_bb_link" to ("#ff0060b0" to "#ff9aa0ef"),
    "zn_pressed" to ("#ff008cff" to "#ff5f65d4"),
    "zn_refresh_arrow_2" to ("#ff0e2e49" to "#ff2c2f66"),
    "zn_start_btn_text" to ("#ff0e2e49" to "#ff2c2f66"),
    "zn_comments_count_bg" to ("#ff2b7691" to "#ff5f65d4"),
    "zn_bb_selection_background" to ("#ff1d7a9f" to "#ff5f65d4"),
    "zn_skin_keyword_on_bg" to ("#ff008cff" to "#ff5f65d4"),
    "zn_search_post_title" to ("#ff2b455b" to "#ff3a3d72"),
    "zn_progress_done" to ("#ff0000ff" to "#ff9aa0ef"),
)

val accentThemePatch = resourcePatch(
    name = "Акцент ReVanced",
    description = "Светлая и тёмная темы: шапка, статусбар, ссылки, кнопки и нажатие с синего на фиолетовый ReVanced. Фоны, ранги и цвета тегов остаются.",
) {
    compatibleWith("ru.fourpda.client"("1.9.44_pre7"))

    apply {
        document(COLORS).use { palette ->
            val nodes = palette.getElementsByTagName("color")
            val done = mutableSetOf<String>()
            for (index in 0 until nodes.length) {
                val color = nodes.item(index) as Element
                val name = color.getAttribute("name")
                val swap = accents[name] ?: continue
                val current = color.textContent.trim().lowercase()
                if (current != swap.first) error("цвет $name сейчас $current")
                color.textContent = swap.second
                done += name
            }
            val missing = accents.keys - done
            if (missing.isNotEmpty()) error("в палитре нет $missing")
        }
    }
}
