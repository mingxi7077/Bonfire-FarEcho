import org.gradle.api.GradleException
import org.gradle.api.tasks.Copy

plugins {
    id("fabric-loom") version "1.10.4"
}

base {
    archivesName.set("farecho-client")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("farecho.minecraftVersion")}")
    mappings("net.fabricmc:yarn:${property("farecho.yarnMappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("farecho.fabricLoaderVersion")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("farecho.fabricApiVersion")}")

    implementation(project(":shared-protocol"))
    include(project(":shared-protocol"))
}

description = "FarEcho Fabric client mod"

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

val archiveBaseName = base.archivesName.get()
val fallbackDevJarName = "${archiveBaseName}-${project.version}-fallback-dev.jar"
val devJarPath = layout.buildDirectory.file("devlibs/${archiveBaseName}-${project.version}-dev.jar")

tasks.register<Copy>("releaseJarFallback") {
    group = "build"
    description = "Builds the dev jar and copies it as fallback when remapJar cannot run due network/TLS issues."
    dependsOn("jar")
    from(devJarPath)
    into(layout.buildDirectory.dir("libs-fallback"))
    rename { fallbackDevJarName }
    doFirst {
        val devJar = devJarPath.get().asFile
        if (!devJar.exists()) {
            throw GradleException("Fallback source jar not found: $devJar")
        }
    }
}

tasks.register("releaseJarPreferred") {
    group = "build"
    description = "Preferred release path: runs remapJar and outputs production jar to build/libs."
    dependsOn("remapJar")
}


