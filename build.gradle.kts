import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.apache.tools.ant.taskdefs.condition.Os

plugins {
    id("org.springframework.boot") version "3.0.2"
    id("io.spring.dependency-management") version "1.1.0"
    kotlin("jvm") version "1.7.22"
    kotlin("plugin.spring") version "1.7.22"
}

group = "com.xxuz.piclane"
version = "0.0.1"
java.sourceCompatibility = JavaVersion.VERSION_17

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
//    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-graphql")
//    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("com.fazecast:jSerialComm:2.9.3")
    implementation("commons-codec:commons-codec:1.15")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("com.mysql:mysql-connector-j")
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
            commandLine(yarnBin, "install", "--frozen-lockfile")
        }

        project.exec {
            workingDir = webProjectDir
            commandLine(yarnBin, "build")
        }

        ant.withGroovyBuilder {
            "mkdir"("dir" to "${webProjectDir}/build/")
            "move"("todir" to "${buildDir}/resources/main/static/", "overwrite" to true) {
                "fileset"("dir" to "${webProjectDir}/build/")
            }
        }
    }
}

tasks.withType<org.gradle.language.jvm.tasks.ProcessResources> {
    dependsOn(tasks.getByName("buildFront"))
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
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
