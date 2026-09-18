plugins { `java-library` }

dependencies {
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")
    implementation("jakarta.transaction:jakarta.transaction-api:2.0.1")
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
}
