pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
}

rootProject.name = "bonfire-farecho"

include(
    ":shared-protocol",
    ":server-paper",
    ":client-fabric-1.21.8",
    ":testbed"
)
