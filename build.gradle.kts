// Top-level build file. Versions match the Prosaurus Android app (../Android).
// AGP 9 has built-in Kotlin support; pin the Kotlin Gradle Plugin above AGP's
// bundled default (see https://kotl.in/gradle/agp-built-in-kotlin).
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    }
}

plugins {
    id("com.android.application") version "9.3.0" apply false
    // org.jetbrains.kotlin.android is intentionally not declared -- AGP 9 has
    // built-in Kotlin support and applying it explicitly is an error.
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}
