pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Attendo"

include(":app")
include(":core:model")
include(":core:data")
include(":core:designsystem")
include(":core:backup")
include(":feature:onboarding")
include(":feature:attendance")
include(":feature:subjects")
include(":feature:stats")
include(":feature:settings")
include(":feature:splash")
include(":feature:pomodoro")
include(":feature:calendar")
include(":feature:tasks")
include(":feature:dashboard")
include(":feature:curriculum")
