plugins {
    id("com.gradleup.shadow") version "8.3.1"
    java
}

val buildNum = System.getenv("CI_PIPELINE_IID") ?: "dirty"
group = "com.vanillarite"
version = "0.3.0-$buildNum"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.incendo.org/content/repositories/snapshots")
    maven("https://ci.frostcast.net/plugin/repository/everything")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://mvn-repo.arim.space/affero-gpl3/")
    maven("https://mvn-repo.arim.space/lesser-gpl3/")
    maven("https://mvn-repo.arim.space/gpl3/")
}

dependencies {
    compileOnly("io.papermc.paper", "paper-api", "1.21.4-R0.1-SNAPSHOT")
    implementation("org.incendo", "cloud-paper", "2.0.0-beta.10")
    implementation("org.incendo", "cloud-annotations", "2.0.0")
    implementation("org.spongepowered", "configurate-yaml", "4.1.2")
    compileOnly("me.confuser.banmanager", "BanManagerCommon", "7.6.0-SNAPSHOT")
    compileOnly("me.confuser.banmanager", "BanManagerBukkit", "7.6.0-SNAPSHOT")
    implementation("info.debatty", "java-string-similarity", "2.0.0")
    compileOnly("space.arim.libertybans", "bans-api", "1.0.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
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

        relocate("org.incendo.cloud", "${rootProject.group}.filter.shade.cloud")
        relocate("io.leangen.geantyref", "${rootProject.group}.filter.shade.typetoken")
        relocate("org.spongepowered.configurate", "${rootProject.group}.filter.shade.configurate")
        relocate("info.debatty", "${rootProject.group}.filter.shade.debatty")
        relocate("org.yaml.snakeyaml", "${rootProject.group}.filter.shade.snakeyaml")

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
