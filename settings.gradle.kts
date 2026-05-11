pluginManagement {
    repositories {
        maven { url = uri("/home/efaz/Codes/chaquopy/maven") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.chaquo.python") version "17.0.1" apply false
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("/home/efaz/Codes/chaquopy/maven") }
        google()
        mavenCentral()
    }
}

rootProject.name = "meDownloader"
include(":app")
