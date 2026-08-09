// VideoHub Pro Enterprise — Root Gradle Config
// buildscript block — ensures JavaPoet 1.13.0 is on the plugin classpath
// This fixes: hiltAggregateDepsDebug -> NoSuchMethodError: ClassName.canonicalName()
// The Hilt Gradle plugin needs JavaPoet 1.13.0+ but Gradle 8.9 bundles an older version.
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.squareup:javapoet:1.13.0")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    // Chaquopy 17.0.0 — embeds Python 3.11 in the app to run yt-dlp natively.
    // Compatible with AGP 7.3.x - 9.2.x (we use 8.7.3).
    // Available on Maven Central (no special repository needed).
    id("com.chaquo.python") version "17.0.0" apply false
}
