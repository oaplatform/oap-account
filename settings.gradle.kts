rootProject.name = "oap-account"

pluginManagement {
    plugins {
        id("oap.java-convention") version providers.gradleProperty("oap.java-convention.version").get()
    }
    repositories {
        maven {
            url = uri(providers.gradleProperty("altRepositoryUri")
                .getOrElse("https://maven.xenoss.net/repository/oap-maven/"))
            val oapUsername = providers.gradleProperty("oap.repository.user").orNull
            val oapPassword = providers.gradleProperty("oap.repository.password").orNull
                ?: System.getenv("CODEARTIFACT_AUTH_TOKEN")
            if (oapUsername != null && oapPassword != null) {
                credentials {
                    username = oapUsername
                    password = oapPassword
                }
            }
        }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            url = uri(providers.gradleProperty("altRepositoryUri")
                .getOrElse("https://maven.xenoss.net/repository/oap-maven/"))
            val oapUsername = providers.gradleProperty("oap.repository.user").orNull
            val oapPassword = providers.gradleProperty("oap.repository.password").orNull
                ?: System.getenv("CODEARTIFACT_AUTH_TOKEN")
            if (oapUsername != null && oapPassword != null) {
                credentials {
                    username = oapUsername
                    password = oapPassword
                }
            }
        }
        mavenCentral()
    }
}

include("oap-account-api")
include("oap-account-social")
include("oap-account")
include("oap-account-test")
