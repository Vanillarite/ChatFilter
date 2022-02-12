plugins {
    id("com.github.johnrengelman.shadow") version "7.0.0"
    java
}

val buildNum = System.getenv("CI_PIPELINE_IID") ?: "dirty"
group = "com.vanillarite"
version = "0.2.3-$buildNum"

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.incendo.org/content/repositories/snapshots")
    maven("https://ci.frostcast.net/plugin/repository/everything")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://mvn-repo.arim.space/affero-gpl3/")
    maven("https://mvn-repo.arim.space/lesser-gpl3/")
    maven("https://mvn-repo.arim.space/gpl3/")
}

dependencies {
    compileOnly("io.papermc.paper", "paper-api", "1.18-R0.1-SNAPSHOT")
    implementation("cloud.commandframework", "cloud-paper", "1.6.0")
    implementation("cloud.commandframework", "cloud-annotations", "1.6.0")
    implementation("net.kyori", "adventure-text-minimessage", "4.10.0-SNAPSHOT") {
        exclude("net.kyori", "adventure-api")
    }
    implementation("org.spongepowered", "configurate-yaml", "4.1.2")
    compileOnly("me.confuser.banmanager", "BanManagerCommon", "7.6.0-SNAPSHOT")
    compileOnly("me.confuser.banmanager", "BanManagerBukkit", "7.6.0-SNAPSHOT")
    implementation("info.debatty", "java-string-similarity", "2.0.0")
    compileOnly("space.arim.libertybans", "bans-api", "1.0.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks {
    shadowJar {
        dependencies {
            exclude(dependency("com.google.guava:"))
            exclude(dependency("com.google.errorprone:"))
            exclude(dependency("org.checkerframework:"))
            exclude(dependency("org.jetbrains:"))
            exclude(dependency("org.intellij:"))
            exclude(dependency("org.yaml:snakeyaml:"))
            exclude(dependency("net.jcip:"))
        }

        relocate("cloud.commandframework", "${rootProject.group}.filter.shade.cloud")
        relocate("io.leangen.geantyref", "${rootProject.group}.filter.shade.typetoken")
        relocate("net.kyori.adventure.text.minimessage", "${rootProject.group}.filter.shade.minimessage")
        relocate("org.spongepowered.configurate", "${rootProject.group}.filter.shade.configurate")
        relocate("info.debatty", "${rootProject.group}.filter.shade.debatty")

        archiveClassifier.set(null as String?)
        destinationDirectory.set(rootProject.tasks.shadowJar.get().destinationDirectory.get())
    }
    build {
        dependsOn(shadowJar)
    }

    withType<ProcessResources> {
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand("version" to version)
        }
    }
}
