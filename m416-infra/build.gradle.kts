plugins { `java-library` }
dependencies {
    api(project(":m416-core"))
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.1.1"))
    implementation("org.springframework.boot:spring-boot-starter-webclient")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
    runtimeOnly("com.mysql:mysql-connector-j")
    testImplementation("com.h2database:h2:2.4.240")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
