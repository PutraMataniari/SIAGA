pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
//        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
    }
    plugins {
        id("com.android.application") version "8.6.0" apply false
        id("org.jetbrains.kotlin.android") version "2.0.21" apply false
        id("org.jetbrains.kotlin.kapt") version "1.9.21" apply false
        id("com.google.dagger.hilt.android") version "2.56.2" apply false
    }

}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "SIAGA"
include(":app")
 