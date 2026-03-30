import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.GradleException
import java.time.LocalDate

plugins {
    base
}

allprojects {
    group = "com.bonfire.farecho"
    version = "0.2.0"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.fabricmc.net/")
    }
}

subprojects {
    plugins.withId("java") {
        extensions.configure<JavaPluginExtension>("java") {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
            withSourcesJar()
        }

        dependencies {
            add("testImplementation", platform("org.junit:junit-bom:5.10.2"))
            add("testImplementation", "org.junit.jupiter:junit-jupiter")
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}

tasks.register("verifyAll") {
    dependsOn(":shared-protocol:test", ":server-paper:test", ":testbed:test")
}

tasks.register<Copy>("assembleTestBundle") {
    group = "distribution"
    description = "Builds plugin+mod artifacts and assembles a directly testable bundle."
    dependsOn(
        ":server-paper:releasePluginJar",
        ":client-fabric-1.21.8:releaseJarPreferred",
        ":client-fabric-1.21.8:releaseJarFallback"
    )

    val bundleDir = layout.buildDirectory.dir("test-bundle")
    val versionName = project.version.toString()

    from(layout.projectDirectory.file("server-paper/build/release/bonfire-farecho-server-${versionName}.jar")) {
        into("server/plugins")
    }
    from(layout.projectDirectory.file("client-fabric-1.21.8/build/libs/farecho-client-${versionName}.jar")) {
        into("client/mods")
    }
    from(layout.projectDirectory.file("client-fabric-1.21.8/build/libs-fallback/farecho-client-${versionName}-fallback-dev.jar")) {
        into("client/mods-fallback")
    }
    from(layout.projectDirectory.file("server-paper/src/main/resources/config.yml")) {
        into("server/plugins/BonfireFarEcho")
        rename { "config.example.yml" }
    }
    from(layout.projectDirectory.file("docs/Bonfire FarEcho_性能账与角色逻辑说明_v1_2026-03-17.md")) {
        into("docs")
    }
    from(layout.projectDirectory.file("docs/Bonfire FarEcho_方案设计与研发计划_v1_2026-03-17.md")) {
        into("docs")
    }
    from(layout.projectDirectory.file("docs/Bonfire FarEcho_服主上线前检查清单_v1_2026-03-18.md")) {
        into("docs")
    }
    from(layout.projectDirectory.file("docs/Bonfire FarEcho_快速联调指南_v1_2026-03-18.md")) {
        into("docs")
    }

    into(bundleDir)

    doFirst {
        val requiredFiles = listOf(
            layout.projectDirectory.file("server-paper/build/release/bonfire-farecho-server-${versionName}.jar").asFile,
            layout.projectDirectory.file("client-fabric-1.21.8/build/libs/farecho-client-${versionName}.jar").asFile
        )
        requiredFiles.forEach { file ->
            if (!file.exists()) {
                throw GradleException("Missing build artifact: $file")
            }
        }
    }

    doLast {
        val root = bundleDir.get().asFile
        val readme = root.resolve("README.txt")
        readme.writeText(
            """
            Bonfire FarEcho Test Bundle
            Date: ${LocalDate.now()}
            Version: ${project.version}

            Server (Paper/Purpur 1.21.8):
            1) Copy server/plugins/bonfire-farecho-server-${project.version}.jar -> <server>/plugins/
            2) First launch will generate config at plugins/BonfireFarEcho/config.yml
            3) Use /farecho stats, /farecho trace <player>, /farecho reload

            Client (Fabric 1.21.8):
            1) Preferred runtime mod: client/mods/farecho-client-${project.version}.jar
            2) Fallback dev jar (only if remap unavailable): client/mods-fallback/farecho-client-${project.version}-fallback-dev.jar

            Suggested test sequence:
            - 2-player handshake + HUD/world marker + no double-display check
            - 20-player smoke
            - 50-player baseline
            - 100-player estimate

            Related docs:
            - docs/Performance-and-role-logic doc
            - docs/Plan-and-roadmap doc
            - docs/Owner preflight checklist
            - docs/Quick test guide
            """.trimIndent()
        )
    }
}

tasks.register<Copy>("assembleTestBundleOffline") {
    group = "distribution"
    description = "Assembles bundle using fallback client jar only (for remap/TLS failure scenarios)."
    dependsOn(
        ":server-paper:releasePluginJar",
        ":client-fabric-1.21.8:releaseJarFallback"
    )

    val bundleDir = layout.buildDirectory.dir("test-bundle-offline")
    val versionName = project.version.toString()

    from(layout.projectDirectory.file("server-paper/build/release/bonfire-farecho-server-${versionName}.jar")) {
        into("server/plugins")
    }
    from(layout.projectDirectory.file("client-fabric-1.21.8/build/libs-fallback/farecho-client-${versionName}-fallback-dev.jar")) {
        into("client/mods")
    }
    from(layout.projectDirectory.file("server-paper/src/main/resources/config.yml")) {
        into("server/plugins/BonfireFarEcho")
        rename { "config.example.yml" }
    }
    from(layout.projectDirectory.file("docs/Bonfire FarEcho_快速联调指南_v1_2026-03-18.md")) {
        into("docs")
    }
    into(bundleDir)
}
