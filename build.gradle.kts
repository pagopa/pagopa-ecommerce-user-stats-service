import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "it.pagopa.ecommerce.users"

version = "1.1.1"

description = "pagopa-ecommerce-user-stats-service"

plugins {
  id("java")
  id("org.springframework.boot") version "3.4.5"
  id("io.spring.dependency-management") version "1.1.6"
  id("com.diffplug.spotless") version "6.25.0"
  id("org.openapi.generator") version "7.6.0"
  id("org.sonarqube") version "4.0.0.2929"
  id("com.dipien.semantic-version") version "2.0.0" apply false
  kotlin("plugin.spring") version "2.2.0"
  kotlin("jvm") version "2.2.0"
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

  const val jacksonDatabindNullableVersion = "0.2.6"

  const val ioSwaggerAnnotationVersion = "2.2.23"
  const val mockitoKotlinVersion = "5.4.0"
}

dependencyManagement {
  // spring boot BOM
  imports { mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.5") }
  // spring cloud BOM
  imports { mavenBom("com.azure.spring:spring-cloud-azure-dependencies:5.22.0") }
  // Kotlin BOM
  imports { mavenBom("org.jetbrains.kotlin:kotlin-bom:2.2.0") }
  imports { mavenBom("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.10.1") }
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
  implementation("com.azure:azure-identity")

  // Kotlin dependencies
  implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

  // openapi generator generated code required dependencies
  implementation(
    "org.openapitools:jackson-databind-nullable:${Dependencies.jacksonDatabindNullableVersion}"
  )
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
  implementation(
    "io.swagger.core.v3:swagger-annotations:${Dependencies.ioSwaggerAnnotationVersion}"
  )

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
  testImplementation("org.mockito.kotlin:mockito-kotlin:${Dependencies.mockitoKotlinVersion}")
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
    java { srcDirs("${layout.buildDirectory.get()}/generated/src/main/java") }
    kotlin {
      srcDirs("src/main/kotlin", "${layout.buildDirectory.get()}/generated/src/main/kotlin")
    }
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
  dependsOn("user-stats-v1")
  compilerOptions {
    apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
    freeCompilerArgs.addAll("-Xjsr305=strict")
  }
}

tasks.create("applySemanticVersionPlugin") {
  group = "semantic-versioning"
  description = "Semantic versioning plugin"
  dependsOn("prepareKotlinBuildScriptModel")
  apply(plugin = "com.dipien.semantic-version")
}

tasks.withType(JavaCompile::class.java).configureEach { options.encoding = "UTF-8" }

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
        fileTree(it).matching { exclude("it/pagopa/ecommerce/users/UserApplicationKt.class") }
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
  outputDir.set("${layout.buildDirectory.get()}/generated")
  apiPackage.set("it.pagopa.generated.ecommerce.users.api")
  modelPackage.set("it.pagopa.generated.ecommerce.users.model")
  generateApiTests.set(false)
  generateApiDocumentation.set(false)
  generateApiTests.set(false)
  generateModelTests.set(false)
  library.set("spring-boot")
  /*
   * Commented out model name suffix because of this issue ->
   * https://github.com/OpenAPITools/openapi-generator/issues/17343
   * that cause discriminator field to not be set properly on deserialized class.
   * With model name suffix on the generated code will use the class name as discriminator fields
   * for the deserialized responses
   */
  // modelNameSuffix.set("Dto")
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
