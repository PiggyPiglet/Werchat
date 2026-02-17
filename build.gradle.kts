plugins {
    java
}

group = "com.werchat"
version = "1.1.9"

java {
    // Note: Compiling with Java 25, targeting Java 21 bytecode for Hytale compatibility
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    maven("https://repo.helpch.at/releases")
}

dependencies {
    // Hytale Server API - place HytaleServer.jar in libs/ directory
    compileOnly(files("libs/HytaleServer.jar"))
    compileOnly("at.helpch:placeholderapi-hytale:1.0.6")
    implementation("com.google.code.gson:gson:2.10.1")
}

val pluginManifestFile = layout.projectDirectory.file("src/main/resources/manifest.json").asFile

tasks.register("validateManifestServerVersion") {
    inputs.file(pluginManifestFile)
    doLast {
        val manifestText = pluginManifestFile.readText()
        val serverVersionMatch = Regex("\"ServerVersion\"\\s*:\\s*\"([^\"]+)\"").find(manifestText)
            ?: throw GradleException("manifest.json is missing ServerVersion")
        val serverVersion = serverVersionMatch.groupValues[1].trim()
        val looksLikeRange = serverVersion.startsWith("=") ||
            serverVersion.startsWith("<") ||
            serverVersion.startsWith(">") ||
            serverVersion.startsWith("^") ||
            serverVersion.startsWith("~")

        if (serverVersion.isEmpty() || serverVersion == "*" || looksLikeRange) {
            throw GradleException(
                "manifest.json ServerVersion must be the exact Hytale server version string " +
                    "(for example: \"2026.02.17-255364b8e\") and cannot use range operators."
            )
        }
    }
}

tasks.processResources {
    dependsOn("validateManifestServerVersion")
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.werchat.WerchatPlugin"
        )
    }
    // Include Gson in the JAR
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Auto-deploy to Hytale Mods folder after build
tasks.register<Copy>("deploy") {
    dependsOn(tasks.jar)
    from(tasks.jar.get().archiveFile)
    val modsDir = file(System.getenv("APPDATA") + "/Hytale/UserData/Mods")
    into(modsDir)
    doFirst {
        modsDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("Werchat-") && file.name.endsWith(".jar")) {
                if (!file.delete()) {
                    println("Warning: Could not delete old jar ${file.name} (possibly in use)")
                }
            }
        }
    }
    doLast {
        println("Deployed to Hytale Mods folder!")
    }
}

// Make build also deploy
tasks.build {
    finalizedBy("deploy")
}
