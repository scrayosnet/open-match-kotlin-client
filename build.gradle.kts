@file:Suppress("UnstableApiUsage")

import com.google.protobuf.gradle.id
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

// provide general GAV coordinates
group = "net.justchunks"
version = "5.0.0"
description = "Open Match Java/Kotlin Client"

// hook the plugins for the builds
plugins {
    `java-library`
    `maven-publish`
    idea
    alias(libs.plugins.kotlin)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kover)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.ktlint)
}

// configure the repositories for the dependencies
repositories {
    // official maven repository
    mavenCentral()
}

// declare all dependencies (for compilation and runtime)
dependencies {
    // add protobuf as api for now for the models
    api(libs.protobuf.kotlin)

    // add gRPC dependencies that are necessary for compilation and execution
    implementation(libs.bundles.grpc)

    // add coroutines for our coroutine based communication
    implementation(libs.kotlin.coroutines.core)

    // compile against the slf4j API for logging
    compileOnly(libs.slf4j)

    // specify test dependencies
    testImplementation(libs.kotlin.test)
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.mockk)
    testImplementation(libs.bundles.log4j)
    testRuntimeOnly(libs.grpc.netty)
}

// configure the kotlin extension
kotlin {
    // set the toolchain version that is required to build this project
    // replaces sourceCompatibility and targetCompatibility as it also sets these implicitly
    // https://kotlinlang.org/docs/gradle-configure-project.html#gradle-java-toolchains-support
    jvmToolchain(21)
}

// configure the protobuf extension (protoc + grpc)
protobuf {
    // configure the protobuf compiler for the proto compilation
    protoc {
        // set the artifact for protoc (the compiler version to use)
        artifact = libs.protoc.core.get().toString()
    }

    // configure the plugins for the protobuf build process
    plugins {
        // add a new "grpc" plugin for the java stub generation
        id("grpc") {
            // set the artifact for protobuf code generation (stubs)
            artifact = libs.protoc.genJava.get().toString()
        }
        // add a new "grpckt" plugin for the kotlin stub generation
        id("grpckt") {
            artifact = libs.protoc.genKotlin.get().toString() + ":jdk8@jar"
        }
    }

    // configure the proto tasks (extraction, generation, etc.)
    generateProtoTasks {
        // only modify the main source set, we don't have any proto files in test
        all().configureEach {
            // apply the "java" and "kotlin" builtin tasks as we are compiling against java and kotlin
            builtins {
                // id("java") – is added implicitly by default
                id("kotlin")
            }

            // apply the "grpc" and "grpckt" plugins whose specs are defined above, without special options
            plugins {
                id("grpc")
                id("grpckt")
            }
        }
    }
}

// configure testing suites within gradle check phase
testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter(libs.versions.junit)
        }
    }
}

// configure the publishing in the maven repository
publishing {
    // define the repositories that shall be used for publishing
    repositories {
        maven("https://gitlab.scrayos.net/api/v4/projects/118/packages/maven") {
            name = "scrayosnet"
            credentials(PasswordCredentials::class)
        }
    }
}

// configure ktlint
ktlint {
    // explicitly use a recent ktlint version for latest checks
    version = libs.versions.ktlint

    // exclude any generated files
    filter {
        // exclude generated protobuf files
        exclude { element -> element.file.path.contains("/generated/") }
    }

    // configure the reporting to use checkstyle syntax
    reporters {
        reporter(ReporterType.PLAIN)
        reporter(ReporterType.CHECKSTYLE)
        reporter(ReporterType.SARIF)
    }
}

// configure sonarqube plugin
sonarqube {
    properties {
        property("sonar.projectName", "open-match-client")
        property("sonar.projectVersion", version)
        property("sonar.projectDescription", description!!)
        property("sonar.pitest.mode", "reuseReport")
        property(
            "sonar.kotlin.ktlint.reportPaths",
            "build/reports/ktlint/ktlintKotlinScriptCheck/ktlintKotlinScriptCheck.xml," +
                "build/reports/ktlint/ktlintMainSourceSetCheck/ktlintMainSourceSetCheck.xml," +
                "build/reports/ktlint/ktlintTestSourceSetCheck/ktlintTestSourceSetCheck.xml",
        )
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/kover/report.xml")
    }
}

// configure tasks
tasks {

    jar {
        // exclude the proto files as we won't need them in downstream projects
        exclude("**/*.proto")

        // exclude the now empty folders (because the proto files were removed)
        includeEmptyDirs = false

        // remove duplicates from the final jar
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    javadoc {
        // exclude the generated protobuf files
        exclude("agones/dev/sdk/**")
        exclude("grpc/gateway/protoc_gen_openapiv2/**")
    }
}
