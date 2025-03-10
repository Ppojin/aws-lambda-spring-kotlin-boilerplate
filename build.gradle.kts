import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer

plugins {
    kotlin("jvm") version "2.1.20-RC"
    kotlin("plugin.spring") version "2.1.20-RC"
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"

    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.springframework.boot.experimental.thin-launcher") version "1.0.31.RELEASE"
}

group = "com.ppojin"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven("https://repo.spring.io/milestone")
}

val springCloudVersion = "2024.0.0"

tasks.assemble {
    dependsOn(tasks.thinJar, tasks.shadowJar)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
        }
    }
}

tasks.shadowJar {
    mustRunAfter(tasks.thinJar)

    archiveClassifier = "aws"
    manifest.inheritFrom(tasks.thinJar.get().manifest)

    mergeServiceFiles()
    append("META-INF/spring.handlers")
    append("META-INF/spring.schemas")
    append("META-INF/spring.tooling")
    append("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")
    append("META-INF/spring/org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration.imports")
    transform(PropertiesFileTransformer::class.java) {
        paths = listOf("META-INF/spring.factories")
        mergeStrategy = "append"
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
//    testImplementation("org.springframework.boot:spring-boot-starter-test")
//    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
//    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-events:3.15.0")
//    implementation("software.amazon.awssdk")

    implementation("org.springframework.cloud:spring-cloud-starter-function-web:4.2.1")
    implementation("org.springframework.cloud:spring-cloud-function-kotlin:4.2.1")
    implementation("org.springframework.cloud:spring-cloud-function-adapter-aws:4.2.1")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
