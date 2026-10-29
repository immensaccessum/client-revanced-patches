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
        description = "Patches for official 4PDA client. Contains patch bundle only without client APK."
        source = "https://github.com/immensaccessum/client-revanced-patches"
        author = "immensaccessum"
        contact = "https://github.com/immensaccessum"
        website = "https://github.com/immensaccessum/client-revanced-patches"
        license = "All rights reserved"
    }
}
