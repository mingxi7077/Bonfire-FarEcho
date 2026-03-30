import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.GradleException
import org.gradle.api.tasks.Copy

plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation(project(":shared-protocol"))
    compileOnly("io.papermc.paper:paper-api:${property("farecho.paperApiVersion")}")
}

description = "FarEcho Paper/Purpur server plugin"

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("all")
}

tasks.register<Copy>("releasePluginJar") {
    group = "build"
    description = "Builds shaded plugin jar that is directly runnable on Paper/Purpur."
    dependsOn("shadowJar")
    val shadedJar = layout.buildDirectory.file("libs/server-paper-${project.version}-all.jar")
    from(shadedJar)
    into(layout.buildDirectory.dir("release"))
    rename { "bonfire-farecho-server-${project.version}.jar" }
    doFirst {
        val file = shadedJar.get().asFile
        if (!file.exists()) {
            throw GradleException("Shaded plugin jar not found: $file")
        }
    }
}
