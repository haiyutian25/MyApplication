pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "My Application"
include(":app")
include(":core:annotation")
include(":core:common")
include(":core:data")
include(":core:navigation")
include(":core:network")
include(":core:ui")
include(":feature:main:api")
include(":feature:main:impl")
include(":feature:settings:api")
include(":feature:settings:impl")
