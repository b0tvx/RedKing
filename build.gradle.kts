import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("jvm") version "1.7.10"
    id("com.github.johnrengelman.shadow") version "7.0.0"

}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://mymavenrepo.com/repo/NIp3fBk55f5oF6VI1Wso/")
    maven("https://4thline.org/m2")
}

dependencies {
    implementation("fr.rhaz:sockets:4.0.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.0.1")
    implementation("org.apache.commons:commons-lang3:3.8.1")
    implementation("commons-codec:commons-codec:1.12")
    implementation("org.apache.bcel:bcel:6.4.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks {
    register("fatJar", Jar::class.java) {
        archiveClassifier.set("all")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes("Main-Class" to "cc.telepath.rhizome.NodeKt")
        }
        from(configurations.runtimeClasspath.get()
            .onEach { println("add from dependencies: ${it.name}") }
            .map { if (it.isDirectory) it else zipTree(it) })
        val sourcesMain = sourceSets.main.get()
        sourcesMain.allSource.forEach { println("add from sources: ${it.name}") }
        from(sourcesMain.output)
    }
}