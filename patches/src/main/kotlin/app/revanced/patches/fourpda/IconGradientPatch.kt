package app.revanced.patches.fourpda

import app.revanced.patcher.patch.resourcePatch

private const val LAUNCHER = "res/mipmap-anydpi-v26/ic_launcher.xml"
private const val ROUND = "res/mipmap-anydpi-v26/ic_launcher_round.xml"

private const val GRADIENT =
    """<gradient android:type="linear" android:startX="54" android:startY="25" android:endX="54" android:endY="83" android:startColor="#fff04e98" android:centerColor="#ff5f65d4" android:endColor="#ff4e98f0" />"""

private const val LAUNCHER_XML =
    """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background>
        <shape android:shape="rectangle">
            <solid android:color="#ffffffff" />
        </shape>
    </background>
    <foreground>
        <vector xmlns:aapt="http://schemas.android.com/aapt" android:height="108.0dip" android:width="108.0dip" android:viewportWidth="108.0" android:viewportHeight="108.0">
            <path android:pathData="m54 25a29 29 0 1 0 29 29 29 29 0 0 0-29-29zm13.69 45.92h-11.69l-.08-6.4h-20.45v-10l15.33-17.44h16.89z">
                <aapt:attr name="android:fillColor">
                    $GRADIENT
                </aapt:attr>
            </path>
            <path android:pathData="m47.16 56.6 8.59.01v-9.56z">
                <aapt:attr name="android:fillColor">
                    $GRADIENT
                </aapt:attr>
            </path>
        </vector>
    </foreground>
    <monochrome>
        <vector android:height="108.0dip" android:width="108.0dip" android:viewportWidth="108.0" android:viewportHeight="108.0">
            <path android:fillColor="#ff000000" android:pathData="M50.36,34.75,32.92,54.53V66H56.15l.09,7.27H69.58V34.75ZM56,57H46.22L56,46.09Z" />
        </vector>
    </monochrome>
</adaptive-icon>
"""

private const val ROUND_XML =
    """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background>
        <shape android:shape="rectangle">
            <solid android:color="#ffffffff" />
        </shape>
    </background>
    <foreground>
        <vector xmlns:aapt="http://schemas.android.com/aapt" android:height="110.0dip" android:width="110.0dip" android:viewportWidth="108.0" android:viewportHeight="108.0">
            <path android:pathData="M50.36,34.75,32.92,54.53V66H56.15l.09,7.27H69.58V34.75ZM56,57H46.22L56,46.09Z">
                <aapt:attr name="android:fillColor">
                    $GRADIENT
                </aapt:attr>
            </path>
        </vector>
    </foreground>
    <monochrome>
        <vector android:height="110.0dip" android:width="110.0dip" android:viewportWidth="108.0" android:viewportHeight="108.0">
            <path android:fillColor="#ff000000" android:pathData="M50.36,34.75,32.92,54.53V66H56.15l.09,7.27H69.58V34.75ZM56,57H46.22L56,46.09Z" />
        </vector>
    </monochrome>
</adaptive-icon>
"""

val iconGradientPatch = resourcePatch(
    name = "Gradient launcher icon",
    description = "Replaces the blue circle background in the app launcher icon with a color gradient.",
    use = false,
) {
    compatibleWith("ru.fourpda.client"("1.9.44_pre7"))

    apply {
        replaceIcon(LAUNCHER, "#ff3861a8", LAUNCHER_XML)
        replaceIcon(ROUND, "#ffffffff", ROUND_XML)
    }
}

private fun app.revanced.patcher.patch.ResourcePatchContext.replaceIcon(
    path: String,
    marker: String,
    xml: String,
) {
    val file = get(path)
    val current = file.readText()
    if (!current.contains(marker)) error("в $path нет прежней заливки")
    file.writeText(xml.trim() + "\n")
}
