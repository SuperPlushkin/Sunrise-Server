
plugins {
    id("java")
    id("org.springframework.boot") version("3.5.0")
    id("io.spring.dependency-management") version("1.1.7")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

group = "com.RisingSun"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    runtimeOnly("org.postgresql:postgresql")
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    // Эта строка гарантирует, что профиль "local" будет активирован в процессе приложения, запущенном задачей bootRun
    systemProperty("spring.profiles.active", "local")

    doFirst {
        // Создаем Map, куда будем загружать переменные
        val envMap = mutableMapOf<String, String>()

        // Читаем каждую строку из .env
        File(".env").readLines().forEach { line ->
            // Пропускаем комментарии и пустые строки
            if (line.isNotEmpty() && !line.startsWith("#")) {
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    // Очищаем ключ и значение от лишних символов и кавычек
                    val key = parts[0].trim()
                    // Убираем потенциальные кавычки, которые могут быть в .env
                    val value = parts[1].trim().trim('"', '\'')
                    envMap[key] = value
                }
            }
        }
        // Добавляем загруженные переменные в окружение процесса bootRun
        environment(envMap)
    }
}

// Отключение ошибок
tasks.withType<JavaExec>().configureEach {
    jvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}