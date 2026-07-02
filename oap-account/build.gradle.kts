plugins {
    id("oap.java-convention")
    `java-library`
}

dependencies {
    implementation(platform("oap:oap-dependencies:${property("oap.java-convention.version")}"))

    api(project(":oap-account-api"))
    api(project(":oap-account-social"))

    val oapVersion = property("oap.deps.oap.version")
    api("oap:oap-stdlib:$oapVersion")
    api("oap:oap-http:$oapVersion")
    api("oap:oap-application:$oapVersion")
    api("oap:oap-http-prometheus:$oapVersion")
    api("oap:oap-ws-sso:$oapVersion")
    api("oap:oap-ws:$oapVersion")
    api("oap:oap-ws-api-ws:$oapVersion")
    api("oap:oap-ws-openapi-ws:$oapVersion")
    api("oap:oap-ws-sso-api:$oapVersion")
    api("oap:oap-ws-admin-ws:$oapVersion")
    api("oap:oap-mail:$oapVersion")
    api("oap:oap-mail-mongo:$oapVersion")
    api("oap:oap-logstream:$oapVersion")
    api("oap:oap-storage:$oapVersion")
    api("oap:oap-storage-mongo:$oapVersion")

    testImplementation("oap:oap-ws-test:$oapVersion")
    testImplementation("oap:oap-stdlib-test:$oapVersion")
}
