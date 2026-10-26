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
        description = "Патчи официального клиента 4PDA. В репозитории только патч, без apk клиента."
        source = "https://github.com/immensaccessum/4pda-revanced-patches"
        author = "immensaccessum"
        license = "MIT"
    }
}
