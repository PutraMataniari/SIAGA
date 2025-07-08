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
    }
    plugins {
        id("com.android.application") version "8.1.4" apply false
        id("org.jetbrains.kotlin.android") version "1.9.21" apply false
        id("org.jetbrains.kotlin.kapt") version "1.9.21" apply false
        id("com.google.dagger.hilt.android") version "2.56.2" apply false
    }

}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SIAGA"
include(":app")
 