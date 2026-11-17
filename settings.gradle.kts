rootProject.name = "Koillectionmobile"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":androidApp", ":composeApp")
include(":core:api", ":core:domain", ":core:image", ":core:photos", ":core:storage")
include(":strings")

include(
    ":feature:auth:data",
    ":feature:auth:domain",
    ":feature:auth:dto",
    ":feature:auth:mapper",
    ":feature:auth:ui"
)
include(
    ":feature:collections:data",
    ":feature:collections:domain",
    ":feature:collections:dto",
    ":feature:collections:mapper",
    ":feature:collections:ui"
)
include(
    ":feature:items:data",
    ":feature:items:domain",
    ":feature:items:dto",
    ":feature:items:mapper",
    ":feature:items:ui"
)
include(
    ":feature:tags:data",
    ":feature:tags:domain",
    ":feature:tags:dto",
    ":feature:tags:mapper",
    ":feature:tags:ui"
)
include(
    ":feature:templates:data",
    ":feature:templates:domain",
    ":feature:templates:dto",
    ":feature:templates:mapper",
    ":feature:templates:ui"
)
include(":feature:settings:ui")
