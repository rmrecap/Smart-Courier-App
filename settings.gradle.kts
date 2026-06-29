pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SmartCourier"

include(":app")
include(":core:domain")
include(":core:data")
include(":core:ui:theme")
include(":feature:auth")
include(":feature:dashboard")
include(":feature:route_planner")
include(":feature:active_delivery")
