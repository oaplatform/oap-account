rootProject.name = "oap-account"

pluginManagement {
    plugins {
        id("oap.java-convention") version providers.gradleProperty("oap.java-convention.version").get()
    }
    repositories {
        maven { url = uri(providers.gradleProperty("altRepositoryUri")
            .getOrElse("https://maven.xenoss.net/repository/oap-maven/")) }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri(providers.gradleProperty("altRepositoryUri")
            .getOrElse("https://maven.xenoss.net/repository/oap-maven/")) }
        mavenCentral()
    }
}

include("oap-account-api")
include("oap-account-social")
include("oap-account")
include("oap-account-test")
