plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    val oapVersion = property("oap.deps.oap.version")
    api("oap:oap-stdlib:$oapVersion")
    api("oap:oap-ws-sso-api:$oapVersion")
    api("oap:oap-ws-sso:$oapVersion")

    testImplementation("oap:oap-ws-test:$oapVersion")
}
