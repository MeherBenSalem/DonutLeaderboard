plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
}

group = "io.nightbeam"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile>().configureEach {
    if (name == "compileTestJava") {
        options.release.set(21)
    } else {
        options.release.set(17)
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

val paperApi = "1.20.1-R0.1-SNAPSHOT"

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApi")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("org.jetbrains:annotations:24.1.0")

    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("com.mysql:mysql-connector-j:8.3.0")
    implementation("org.xerial:sqlite-jdbc:3.46.1.0")
    implementation("org.bstats:bstats-bukkit:3.1.0")

    testImplementation("io.papermc.paper:paper-api:$paperApi")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.20:3.93.2")
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.testcontainers:mysql:1.20.4")
    testImplementation("ch.vorburger.mariaDB4j:mariaDB4j:3.2.0")
    testImplementation("ch.vorburger.mariaDB4j:mariaDB4j-db-linux64:11.4.5")
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.shadowJar {
    archiveFileName.set("DonutLeaderboard-${project.version}.jar")
    relocate("com.zaxxer.hikari", "io.nightbeam.donutleaderboard.lib.hikari")
    relocate("com.mysql", "io.nightbeam.donutleaderboard.lib.mysql")
    relocate("org.bstats", "io.nightbeam.donutleaderboard.lib.bstats")
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform {
        excludeTags("integration", "smoke", "docker")
    }
    testLogging {
        events("passed", "skipped", "failed")
    }
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
}

tasks.register<Exec>("smokeServerBoot") {
    description = "Download and boot Paper/Purpur/Folia matrix with DonutLeaderboard"
    group = "verification"
    dependsOn(tasks.shadowJar)
    environment("DONUT_LEADERBOARD_SMOKE_ENABLED", "true")
    commandLine("bash", "$projectDir/scripts/smoke-server-test.sh")
}

tasks.register<Test>("integrationTest") {
    description = "MySQL storage tests via Testcontainers (requires Docker)"
    group = "verification"
    useJUnitPlatform {
        includeTags("docker")
    }
    shouldRunAfter(tasks.test)
}

tasks.register<Test>("smokeTest") {
    description = "Boots downloaded Paper/Folia jars when enabled"
    group = "verification"
    useJUnitPlatform {
        includeTags("smoke")
    }
    systemProperty("donutleaderboard.smoke.enabled", System.getProperty("donutleaderboard.smoke.enabled", "false"))
    shouldRunAfter(tasks.test)
}

tasks.named("check") {
    dependsOn(tasks.test)
}
