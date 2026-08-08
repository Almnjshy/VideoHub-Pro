// VideoHub Pro Enterprise — Root Gradle Config
// buildscript block — ensures JavaPoet 1.13.0 is on the plugin classpath
// This fixes: hiltAggregateDepsDebug -> NoSuchMethodError: ClassName.canonicalName()
// The Hilt Gradle plugin needs JavaPoet 1.13.0+ but Gradle 8.9 bundles an older version.
buildscript {
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
}
