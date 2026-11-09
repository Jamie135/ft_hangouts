plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.jamie.ft_hangouts"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jamie.ft_hangouts"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
}
