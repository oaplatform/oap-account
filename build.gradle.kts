subprojects {
    plugins.withType<JavaPlugin> {
        configurations {
            named("testAnnotationProcessor") {
                extendsFrom(configurations["annotationProcessor"])
            }
            named("testCompileOnly") {
                extendsFrom(configurations["compileOnly"])
            }
        }
    }
}
