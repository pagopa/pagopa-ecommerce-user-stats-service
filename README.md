# PagoPA eCommerce user stats service

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=pagopa_pagopa-ecommerce-user-stats-service&metric=alert_status)](https://sonarcloud.io/dashboard?id=pagopa_pagopa-ecommerce-user-stats-service)

This microservice is responsible for expose stats for user performing transactions on eCommerce.

Those stats include information about the latest used payment method for a transaction

- [PagoPA eCommerce user stats service](#pagopa-ecommerce-user-stats-service)
    * [Api Documentation 📖](#api-documentation-)
    * [Technology Stack](#technology-stack)
    * [Start Project Locally 🚀](#start-project-locally-)
        + [Prerequisites](#prerequisites)
        + [Run docker container](#run-docker-container)
    * [Develop Locally 💻](#develop-locally-)
        + [Prerequisites](#prerequisites-1)
        + [Run the project](#run-the-project)
        + [Testing 🧪](#testing-)
            - [Unit testing](#unit-testing)
            - [Integration testing](#integration-testing)
            - [Performance testing](#performance-testing)
    * [Dependency management 🔧](#dependency-management-)
        + [Dependency lock](#dependency-lock)
        + [Dependency verification](#dependency-verification)
    * [Contributors 👥](#contributors-)
        + [Maintainers](#maintainers)


---

## Api Documentation 📖

See
the [OpenAPI 3 here.](https://editor.swagger.io/?url=TODO)

---

## Technology Stack

- Kotlin
- Spring Boot

---

## Start Project Locally 🚀

### Prerequisites

- docker

### Populate the environment

The microservice needs a valid `.env` file in order to be run.

If you want to start the application without too much hassle, you can just copy `.env.example` with

```shell
$ cp .env.example .env
```

to get a good default configuration.

If you want to customize the application environment, reference this table:

| Variable name                     | Description                                                                                                                                                | type   | default |
|-----------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|--------|---------|
| ROOT_LOGGING_LEVEL                | Root logging level                                                                                                                                         | string | INFO    |
| APP_LOGGING_LEVEL                 | Application logging level (package it.pagopa)                                                                                                              | string | INFO    |
| WEB_LOGGING_LEVEL                 | Spring web logging level (logs about http requests/responses                                                                                               | string | OFF     |
| MONGO_HOST                        | Host where MongoDB instance used to persise events and view resides                                                                                        | string |         |
| MONGO_USERNAME                    | Username used for connecting to MongoDB instance                                                                                                           | string |         |
| MONGO_PASSWORD                    | Password used for connecting to MongoDB instance                                                                                                           | string |         |
| MONGO_SSL_ENABLED                 | Whether SSL is enabled while connecting to MongoDB                                                                                                         | string |         |
| MONGO_PORT                        | Port used for connecting to MongoDB instance                                                                                                               | string |         |
| MONGO_MIN_POOL_SIZE               | Min amount of connections to be retained into connection pool. See docs *                                                                                  | string |         |
| MONGO_MAX_POOL_SIZE               | Max amount of connections to be retained into connection pool.See docs *                                                                                   | string |         |
| MONGO_MAX_IDLE_TIMEOUT_MS         | Max timeout after which an idle connection is killed in milliseconds. See docs *                                                                           | string |         |
| MONGO_CONNECTION_TIMEOUT_MS       | Max time to wait for a connection to be opened. See docs *                                                                                                 | string |         |
| MONGO_SOCKET_TIMEOUT_MS           | Max time to wait for a command send or receive before timing out. See docs *                                                                               | string |         |
| MONGO_SERVER_SELECTION_TIMEOUT_MS | Max time to wait for a server to be selected while performing a communication with Mongo in milliseconds. See docs *                                       | string |         |
| MONGO_WAITING_QUEUE_MS            | Max time a thread has to wait for a connection to be available in milliseconds. See docs *                                                                 | string |         |
| MONGO_HEARTBEAT_FREQUENCY_MS      | Hearth beat frequency in milliseconds. This is an hello command that is sent periodically on each active connection to perform an health check. See docs * | string |         |

(*): for Mongo connection string options
see [docs](https://www.mongodb.com/docs/drivers/java/sync/v4.3/fundamentals/connection/connection-options/#connection-options)

### Run docker container

```shell
$ docker compose up --build
```

---

## Develop Locally 💻

### Prerequisites

- git
- gradle
- jdk-21
- kotlin 2.2

### Run the project

```shell
$ export $(grep -v '^#' .env.local | xargs)
$ ./gradlew bootRun
```


### Testing 🧪

#### Unit testing

To run the **Junit** tests:

```shell
$ ./gradlew test
```

#### Integration testing

TODO

#### Performance testing

install [k6](https://k6.io/) and then from `./performance-test/src`

1. `k6 run --env VARS=local.environment.json --env TEST_TYPE=./test-types/load.json main_scenario.js`

### Dependency management 🔧

For support reproducible build this project has the following gradle feature enabled:

- [dependency lock](https://docs.gradle.org/8.1/userguide/dependency_locking.html)
- [dependency verification](https://docs.gradle.org/8.1/userguide/dependency_verification.html)

#### Dependency lock

This feature use the content of `gradle.lockfile` to check the declared dependencies against the locked one.

If a transitive dependencies have been upgraded the build will fail because of the locked version mismatch.

The following command can be used to upgrade dependency lockfile:

```shell
./gradlew dependencies --write-locks 
```

Running the above command will cause the `gradle.lockfile` to be updated against the current project dependency
configuration

#### Dependency verification

This feature is enabled by adding the gradle `./gradle/verification-metadata.xml` configuration file.

Perform checksum comparison against dependency artifact (jar files, zip, ...) and metadata (pom.xml, gradle module
metadata, ...) used during build
and the ones stored into `verification-metadata.xml` file raising error during build in case of mismatch.

The following command can be used to recalculate dependency checksum:

```shell
./gradlew --write-verification-metadata sha256 clean spotlessApply build --no-build-cache --refresh-dependencies
```

In the above command the `clean`, `spotlessApply` `build` tasks where chosen to be run
in order to discover all transitive dependencies used during build and also the ones used during
spotless apply task used to format source code.

The above command will upgrade the `verification-metadata.xml` adding all the newly discovered dependencies' checksum.
Those checksum should be checked against a trusted source to check for corrispondence with the library author published
checksum.

`/gradlew --write-verification-metadata sha256` command appends all new dependencies to the verification files but does
not remove
entries for unused dependencies.

This can make this file grow every time a dependency is upgraded.

To detect and remove old dependencies make the following steps:

1. Delete, if present, the `gradle/verification-metadata.dryrun.xml`
2. Run the gradle write-verification-metadata in dry-mode (this will generate a verification-metadata-dryrun.xml file
   leaving untouched the original verification file)
3. Compare the verification-metadata file and the verification-metadata.dryrun one checking for differences and removing
   old unused dependencies

The 1-2 steps can be performed with the following commands

```Shell
rm -f ./gradle/verification-metadata.dryrun.xml 
./gradlew --write-verification-metadata sha256 clean spotlessApply build --dry-run
```

The resulting `verification-metadata.xml` modifications must be reviewed carefully checking the generated
dependencies checksum against official websites or other secure sources.

If a dependency is not discovered during the above command execution it will lead to build errors.

You can add those dependencies manually by modifying the `verification-metadata.xml`
file adding the following component:

```xml

<verification-metadata>
    <!-- other configurations... -->
    <components>
        <!-- other components -->
        <component group="GROUP_ID" name="ARTIFACT_ID" version="VERSION">
            <artifact name="artifact-full-name.jar">
                <sha256 value="sha value"
                        origin="Description of the source of the checksum value"/>
            </artifact>
            <artifact name="artifact-pom-file.pom">
                <sha256 value="sha value"
                        origin="Description of the source of the checksum value"/>
            </artifact>
        </component>
    </components>
</verification-metadata>
```

Add those components at the end of the components list and then run the

```shell
./gradlew --write-verification-metadata sha256 clean spotlessApply build --no-build-cache --refresh-dependencies
```

that will reorder the file with the added dependencies checksum in the expected order.

Finally, you can add new dependencies both to gradle.lockfile writing verification metadata running

```shell
./gradlew --write-locks --write-verification-metadata sha256 clean spotlessApply build --no-build-cache --refresh-dependencies
```

For more information read the
following [article](https://docs.gradle.org/8.1/userguide/dependency_verification.html#sec:checksum-verification)

## Contributors 👥

Made with ❤️ by PagoPA S.p.A.

### Maintainers

See `CODEOWNERS` file
