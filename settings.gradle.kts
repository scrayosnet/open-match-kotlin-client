rootProject.name = "open-match-client"

buildCache {
    local {
        directory = File(rootDir, "build-cache")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
