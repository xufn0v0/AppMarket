plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.composeMultiplatform) apply false
}

// 版本自动递增：./gradlew bumpVersion [-Plevel=patch|minor|major]
val versionConfigFile = layout.projectDirectory.file("buildSrc/src/main/kotlin/ProjectConfig.kt")

tasks.register<BumpVersionTask>("bumpVersion") {
    level.set(providers.gradleProperty("level").orElse(BumpLevel.PATCH.name.lowercase()))
    configFile.set(versionConfigFile)
}

// 便捷任务：./gradlew bumpPatch | bumpMinor | bumpMajor
listOf(BumpLevel.PATCH, BumpLevel.MINOR, BumpLevel.MAJOR).forEach { bumpLevel ->
    tasks.register<BumpVersionTask>("bump${bumpLevel.name.lowercase().replaceFirstChar { it.uppercase() }}") {
        level.set(bumpLevel.name.lowercase())
        configFile.set(versionConfigFile)
    }
}
