plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    val oapVersion = property("oap.deps.oap.version")
    api("oap:oap-ws-sso:$oapVersion")
    api("oap:oap-stdlib:$oapVersion")

    api("com.restfb:restfb:2024.12.0")
    api("com.google.api-client:google-api-client:2.7.0")
    api("com.google.oauth-client:google-oauth-client-jetty:1.36.0")
    api("com.auth0:auth0:2.12.0") {
        exclude(group = "com.fasterxml.jackson.core", module = "jackson-databind")
    }
    api("software.amazon.awssdk:cognitoidentity")
    api("software.amazon.awssdk:cognitoidentityprovider")
    api("software.amazon.awssdk:secretsmanager")
    api("com.google.code.gson:gson")
}
