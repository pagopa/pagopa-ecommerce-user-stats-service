import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "it.pagopa.ecommerce.users"

version = "0.0.1"

description = "pagopa-ecommerce-user-service"

plugins {
  id("java")
  id("org.springframework.boot") version "3.0.5"
  id("io.spring.dependency-management") version "1.1.0"
  id("com.diffplug.spotless") version "6.18.0"
  id("org.openapi.generator") version "7.8.0"
  id("org.sonarqube") version "4.0.0.2929"
  id("com.dipien.semantic-version") version "2.0.0" apply false
  kotlin("plugin.spring") version "1.8.10"
  kotlin("jvm") version "1.8.10"
  jacoco
  application
}

java.sourceCompatibility = JavaVersion.VERSION_17

tasks.withType<KotlinCompile> { kotlinOptions.jvmTarget = "17" }

repositories {
  mavenCentral()
  mavenLocal()
}

object Dependencies {
  const val ecsLoggingVersion = "1.5.0"
  const val openTelemetryVersion = "1.37.0"
  // eCommerce commons library version
  const val ecommerceCommonsVersion = "1.27.0"

  // eCommerce commons library git ref (by default tag)
  const val ecommerceCommonsGitRef = ecommerceCommonsVersion

  const val swaggerAnnotationsVersion = "2.2.8"
  const val findBugsVersion = "3.0.2"

  const val jacksonDatabindNullableVersion = "0.2.6"

  const val ioVavrVersion = "0.10.4"
}

dependencyManagement {
  imports { mavenBom("org.springframework.boot:spring-boot-dependencies:3.0.5") }
  imports { mavenBom("com.azure.spring:spring-cloud-azure-dependencies:5.13.0") }
  // Kotlin BOM
  imports { mavenBom("org.jetbrains.kotlin:kotlin-bom:1.7.22") }
  imports { mavenBom("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.6.4") }
}

dependencies {
  implementation("io.projectreactor:reactor-core")
  implementation("io.projectreactor.netty:reactor-netty")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("com.azure.spring:spring-cloud-azure-starter")
  implementation("com.azure.spring:spring-cloud-azure-starter-data-cosmos")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-web-services")
  implementation("org.glassfish.jaxb:jaxb-runtime")
  implementation("jakarta.xml.bind:jakarta.xml.bind-api")
  implementation("io.swagger.core.v3:swagger-annotations:${Dependencies.swaggerAnnotationsVersion}")
  implementation("org.apache.httpcomponents:httpclient")
  implementation("com.google.code.findbugs:jsr305:${Dependencies.findBugsVersion}")
  implementation("org.projectlombok:lombok")
  implementation(
    "org.openapitools:jackson-databind-nullable:${Dependencies.jacksonDatabindNullableVersion}"
  )
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
  implementation("org.springframework.boot:spring-boot-starter-aop")
  implementation("io.vavr:vavr:${Dependencies.ioVavrVersion}")
  // Kotlin dependencies
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

  // ECS logback encoder
  implementation("co.elastic.logging:logback-ecs-encoder:${Dependencies.ecsLoggingVersion}")

  // otel api
  implementation("io.opentelemetry:opentelemetry-api:${Dependencies.openTelemetryVersion}")

  // eCommerce commons library
  implementation("it.pagopa:pagopa-ecommerce-commons:${Dependencies.ecommerceCommonsVersion}")

  runtimeOnly("org.springframework.boot:spring-boot-devtools")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.mockito:mockito-inline")
  testImplementation("io.projectreactor:reactor-test")
  // Kotlin dependencies
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
  testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
  // eCommerce commons tests utility library
  testImplementation(
    "it.pagopa:pagopa-ecommerce-commons:${Dependencies.ecommerceCommonsVersion}:tests"
  )
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
    kotlin { srcDirs("src/main/kotlin", "$buildDir/generated/src/main/kotlin") }
    resources { srcDirs("src/resources") }
  }
}

springBoot {
  mainClass.set("it.pagopa.ecommerce.users.UserApplicationKt")
  buildInfo { properties { additional.set(mapOf("description" to project.description)) } }
}

tasks.create("applySemanticVersionPlugin") {
  group = "semantic-versioning"
  description = "Semantic versioning plugin"
  dependsOn("prepareKotlinBuildScriptModel")
  apply(plugin = "com.dipien.semantic-version")
}

tasks.withType<KotlinCompile> {
  dependsOn("install-commons", "user-stats-v1")
  kotlinOptions.jvmTarget = "17"
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
  jvmArgs(listOf("--enable-preview"))
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
