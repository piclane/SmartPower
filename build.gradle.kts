import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "3.5.8"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
}

group = "com.xxuz.piclane"
version = "1.4.2"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-graphql")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("com.fazecast:jSerialComm:2.10.4")
    implementation("commons-codec:commons-codec:1.15")
    implementation("io.micrometer:micrometer-registry-prometheus:1.14.2")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework.graphql:spring-graphql-test")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

tasks.register("buildFront") {
    group = "build"
    description = "yarn run build"
    doLast {
        val webProjectDir = file("${projectDir}/frontend")
        val yarnBin = if(Os.isFamily(Os.FAMILY_WINDOWS)) "yarn.cmd" else "yarn"

        project.exec {
            workingDir = webProjectDir
            commandLine(yarnBin, "install", "--immutable")
        }

        project.exec {
            workingDir = webProjectDir
            commandLine(yarnBin, "build")
        }

        ant.withGroovyBuilder {
            "delete"("dir" to "${projectDir}/src/main/resources/static/", "quiet" to true)
            "mkdir"("dir" to "${webProjectDir}/build/")
            "move"("todir" to "${projectDir}/src/main/resources/static/", "overwrite" to true) {
                "fileset"("dir" to "${webProjectDir}/build/")
            }
        }
    }
}

tasks.withType<ProcessResources> {
    dependsOn("buildFront")
}

tasks.withType<Jar> {
    manifest {
        attributes["Implementation-Title"] = group
        attributes["Implementation-Version"] = archiveVersion
        attributes["Implementation-Vendor"] = "piclane"
    }
    archiveClassifier.set("")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}
