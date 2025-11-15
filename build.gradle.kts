plugins {
    alias(libs.plugins.indra)
    alias(libs.plugins.indra.publishing)
}

repositories {
    mavenCentral()
    maven("https://libraries.minecraft.net") {
        metadataSources {
            artifact()
        }
    }
}

dependencies {
    api(libs.adventure.nbt)
    api(libs.dfu)
    api(libs.fastutil)
    api(libs.jspecify)
}

indra {
    javaVersions {
        target(21)
    }

    publishReleasesTo("roxymc", "https://repo.roxymc.net/releases")
    publishSnapshotsTo("roxymc", "https://repo.roxymc.net/snapshots")
}

tasks.withType<Sign>().configureEach {
    enabled = false
}
