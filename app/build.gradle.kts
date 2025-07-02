plugins {
        alias(libs.plugins.android.application)
        alias(libs.plugins.kotlin.android)
        id("com.google.gms.google-services")
        alias(libs.plugins.compose.compiler)
    }

    android {
        namespace = "br.edu.utfpr.social_pm46s"
        compileSdk = 36

        defaultConfig {
            applicationId = "br.edu.utfpr.social_pm46s"
            minSdk = 26
            targetSdk = 34
            versionCode = 1
            versionName = "1.0"

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            vectorDrawables {
                useSupportLibrary = true
            }
        }

        buildTypes {
            release {
                isMinifyEnabled = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
        kotlinOptions {
            jvmTarget = "1.8"
        }
        buildFeatures {
            compose = true
        }
        composeOptions {
            kotlinCompilerExtensionVersion = "1.5.1"
        }
        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
            }
        }
    }

    dependencies {
        // Core Android dependencies
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.fragment.ktx)

        // Compose BOM and related dependencies
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.ui)
        implementation(libs.androidx.ui.graphics)
        implementation(libs.androidx.ui.tooling.preview)
        implementation(libs.androidx.material3)

        // Firebase BOM and related dependencies
        implementation(platform(libs.firebase.bom))
        implementation(libs.firebase.auth)
        implementation(libs.firebase.auth.ktx)
        implementation(libs.firebase.firestore)
        implementation(libs.firebase.firestore.ktx)
        implementation(libs.firebase.database.ktx)

        // Google Services
        implementation(libs.play.services.auth)
        implementation(libs.googleid)
        implementation(libs.play.services.location)

        // Credentials API
        implementation(libs.androidx.credentials)
        implementation(libs.androidx.credentials.play.services.auth)

        // Health Connect
        implementation(libs.androidx.health.connect.client)

        // Coroutines
        implementation(libs.kotlinx.coroutines.android)
        implementation(libs.kotlinx.coroutines.play.services)

        // Testing dependencies
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(platform(libs.androidx.compose.bom))
        androidTestImplementation(libs.androidx.ui.test.junit4)

        // Debug dependencies
        debugImplementation(libs.androidx.ui.tooling)
        debugImplementation(libs.androidx.ui.test.manifest)
    }