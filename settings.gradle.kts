// Chaquopy is published from https://chaquo.com/maven. We allow a local override
// via the CHAQUOPY_MAVEN env var so we can test against a patched build without
// editing this file. Both CI and a fresh clone resolve Chaquopy from the public
// maven with no extra setup.

pluginManagement {
    repositories {
        System.getenv("CHAQUOPY_MAVEN")?.takeIf { it.isNotBlank() }
            ?.let { maven { url = uri(it) } }
        maven { url = uri("https://chaquo.com/maven") }
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
        System.getenv("CHAQUOPY_MAVEN")?.takeIf { it.isNotBlank() }
            ?.let { maven { url = uri(it) } }
        maven { url = uri("https://chaquo.com/maven") }
        google()
        mavenCentral()
    }
}

rootProject.name = "meDownloader"
include(":app")
