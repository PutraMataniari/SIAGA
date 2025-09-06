plugins {
//    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
    id("com.android.application")
//    alias(libs.plugins.google.services)
//    id("com.google.gms.google-services")
//    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.siaga"
    compileSdk = 35

    packagingOptions {
        exclude ("META-INF/INDEX.LIST")
        exclude ("META-INF/io.netty.versions.properties")
        exclude ("META-INF/DEPENDENCIES")
        exclude ("META-INF/LICENSE")
        exclude ("META-INF/NOTICE")
        exclude ("META-INF/INDEX.LIST")
        exclude ("META-INF/kotlinx_coroutines_core.version")
        pickFirst ("META-INF/INDEX.LIST")
        pickFirst ("META-INF/LICENSE")
        pickFirst ("META-INF/NOTICE")
    }

    defaultConfig {
        applicationId = "com.example.siaga"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
//        compose = true
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.gridlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.material)
    implementation(libs.play.services.location)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.recyclerview)
//    implementation(libs.material)
//    implementation(libs.androidx.room.runtime)
//    implementation(libs.androidx.room.ktx)
//    annotationProcessor(libs.androidx.room.compiler)
    implementation(libs.rxandroid)
    implementation(libs.rxjava)
    implementation(libs.glide)
    implementation(libs.hilt.android)
    implementation(libs.lifecycle.viewmodel.ktx)

    // 🔽 Tambahkan Retrofit dan Gson Converter di sini
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation (libs.datastore.preferences)
    implementation(libs.androidx.datastore.core.android)
//    implementation(libs.firebase.appdistribution.gradle)

    // OkHttp
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

//hdodenhof
    implementation(libs.circleimageview)

//    Fragment
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.cardview)

    kapt(libs.hilt.compiler)

    kapt(libs.glide.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

}