plugins {
    id("com.android.application")
}

android {
    namespace = "org.miniopensource.miniclassisland"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "org.miniopensource.miniclassisland"
        minSdk = 26
        targetSdk = 35
        versionCode = 240
        versionName = "2.4.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("com.google.zxing:core:3.5.3")
}
