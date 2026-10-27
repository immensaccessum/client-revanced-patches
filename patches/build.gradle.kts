@file:OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension

group = "app.revanced"

extensions.configure<KotlinJvmProjectExtension>("kotlin") {
    extensions.configure<AbiValidationExtension>("abiValidation") {
        enabled.set(false)
    }
}

patches {
    about {
        name = "4PDA patches"
        description = "Патчи официального клиента 4PDA. В репозитории только патч, без apk клиента. Использование и распространение модификаций запрещены."
        source = "https://github.com/immensaccessum/client-revanced-patches"
        author = "immensaccessum"
        contact = "https://github.com/immensaccessum"
        website = "https://github.com/immensaccessum/client-revanced-patches"
        license = "Все права защищены, передача запрещена"
    }
}
