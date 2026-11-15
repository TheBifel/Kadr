plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.thebifel.kadr"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.thebifel.kadr"
        minSdk = 31
        targetSdk = 36
        versionCode = 11
        versionName = "1.1.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs.getByName("debug") {
        val externalKey =
            providers.environmentVariable("KADR_DEBUG_KEYSTORE_PATH").orNull?.takeIf {
                it.isNotBlank()
            }
        if (externalKey != null) {
            fun required(name: String) =
                requireNotNull(
                    providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }
                ) {
                    "Missing signing environment variable: $name"
                }
            storeFile = file(externalKey)
            storePassword = required("KADR_DEBUG_STORE_PASSWORD")
            keyAlias = required("KADR_DEBUG_KEY_ALIAS")
            keyPassword = required("KADR_DEBUG_KEY_PASSWORD")
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":domain"))
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")
    implementation(platform("androidx.compose:compose-bom:2025.08.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.glance:glance-appwidget:1.2.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
