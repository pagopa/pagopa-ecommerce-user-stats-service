import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "it.pagopa.ecommerce.users"

version = "0.0.1"

description = "pagopa-ecommerce-user-stats-service"

plugins {
    id("java")
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
    id("com.diffplug.spotless") version "6.18.0"
  id("org.openapi.generator") version "6.3.0"
    id("org.sonarqube") version "4.0.0.2929"
    id("com.dipien.semantic-version") version "2.0.0" apply false
    kotlin("plugin.spring") version "1.8.10"
    kotlin("jvm") version "1.9.25"
    jacoco
    application
}

repositories {
    mavenCentral()
    mavenLocal()
}

object Dependencies {
    const val ecsLoggingVersion = "1.5.0"

    const val ioVavrVersion = "0.10.4"
}

dependencyManagement {
    // spring boot BOM
    imports { mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.4") }
    // spring cloud BOM
    imports { mavenBom("com.azure.spring:spring-cloud-azure-dependencies:5.16.0") }
    // Kotlin BOM
    imports { mavenBom("org.jetbrains.kotlin:kotlin-bom:1.9.25") }
    imports { mavenBom("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.9.0") }
}

dependencies {
    // spring dependencies
    implementation("io.projectreactor:reactor-core")
    implementation("io.projectreactor.netty:reactor-netty")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // spring cloud azure dependencies
    implementation("com.azure.spring:spring-cloud-azure-starter")
    implementation("com.azure.spring:spring-cloud-azure-starter-data-cosmos")

    // Kotlin dependencies
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // ECS logback encoder
    implementation("co.elastic.logging:logback-ecs-encoder:${Dependencies.ecsLoggingVersion}")

    runtimeOnly("org.springframework.boot:spring-boot-devtools")
    // test dependencies
  testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    // Kotlin dependencies
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

configurations {
    implementation.configure {
        exclude(module = "spring-boot-starter-web")
        exclude("org.apache.tomcat")
        exclude(group = "org.slf4j", module = "slf4j-simple")
    }
}
// Dependency locking - lock all dependencies
dependencyLocking { lockAllConfigurations() }

sourceSets {
    main {
        java { srcDirs("$buildDir/generated/src/main/java") }
        kotlin { srcDirs("src/main/kotlin", "${layout.buildDirectory}/generated/src/main/kotlin") }
        resources { srcDirs("src/resources") }
    }
}

springBoot {
    mainClass.set("it.pagopa.ecommerce.users.UserApplicationKt")
    buildInfo { properties { additional.set(mapOf("description" to project.description)) } }
}
// compilation configurations
java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

tasks.withType<KotlinCompile> {
    dependsOn()
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.create("applySemanticVersionPlugin") {
    group = "semantic-versioning"
    description = "Semantic versioning plugin"
    dependsOn("prepareKotlinBuildScriptModel")
    apply(plugin = "com.dipien.semantic-version")
}

tasks.withType(JavaCompile::class.java).configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("--enable-preview")
}

tasks.withType(Javadoc::class.java).configureEach { options.encoding = "UTF-8" }

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
  kotlin {
    toggleOffOn()
    targetExclude("build/**/*")
    ktfmt().kotlinlangStyle()
  }
  kotlinGradle {
    toggleOffOn()
    targetExclude("build/**/*.kts")
    ktfmt().googleStyle()
  }
  java {
    target("**/*.java")
    targetExclude("build/**/*")
    eclipse().configFile("eclipse-style.xml")
    toggleOffOn()
    removeUnusedImports()
    trimTrailingWhitespace()
    endWithNewline()
  }
}

tasks.named<Jar>("jar") { enabled = false }

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it).matching {
                    exclude("it/pagopa/ecommerce/users/UserApplicationKt.class")
                }
            }
        )
    )

    reports { xml.required.set(true) }
}

/**
 * Task used to expand application properties with build specific properties such as artifact name
 * and version
 */
tasks.processResources { filesMatching("application.properties") { expand(project.properties) } }

tasks.register<Exec>("install-commons") {
  group = "build"
  description = "Compile eCommerce commons library pulling referred version branch"
  val buildCommons = providers.gradleProperty("buildCommons")
  onlyIf("To build commons library run gradle build -PbuildCommons") { buildCommons.isPresent }
  commandLine(
    "sh",
    "./pagopa-ecommerce-commons-maven-install.sh",
    Dependencies.ecommerceCommonsGitRef
  )
}


/*
 * used java generator here instead of kotlin-spring one since kotlin generator generates
 * interfaces for one of interface that are not implemented by data concrete data classes
 * making code unusable
 */
tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("user-stats-v1") {
  group = "code-generator"
  description = "Generate stubs from openapi"
  generatorName.set("spring")
  inputSpec.set("$rootDir/api-spec/v1/user-stats-api.yaml")
  outputDir.set("$buildDir/generated")
  apiPackage.set("it.pagopa.generated.ecommerce.users.api")
  modelPackage.set("it.pagopa.generated.ecommerce.users.model")
  generateApiTests.set(false)
  generateApiDocumentation.set(false)
  generateApiTests.set(false)
  generateModelTests.set(false)
  library.set("spring-boot")
  modelNameSuffix.set("Dto")
  configOptions.set(
    mapOf(
      "swaggerAnnotations" to "false",
      "openApiNullable" to "true",
      "interfaceOnly" to "true",
      "hideGenerationTimestamp" to "true",
      "skipDefaultInterface" to "true",
      "useSwaggerUI" to "false",
      "reactive" to "true",
      "useSpringBoot3" to "true",
      "oas3" to "true",
      "generateSupportingFiles" to "true",
      "legacyDiscriminatorBehavior" to "true",
      "useOneOfInterfaces" to "true",
    )
  )
}