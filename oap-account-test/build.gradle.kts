plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api(project(":oap-account"))
    api(project(":oap-account-api"))

    val oapVersion = property("oap.deps.oap.version")
    api("oap:oap-ws-sso:$oapVersion")
    api("oap:oap-stdlib:$oapVersion")
    api("oap:oap-ws-test:$oapVersion")
    api("oap:oap-ws-sso-api:$oapVersion")
    api("oap:oap-storage-mongo-test:$oapVersion")
    api("oap:oap-mail-test:$oapVersion")
}
