package it.pagopa.ecommerce.users.controllers.v1

import it.pagopa.ecommerce.users.UserTestUtils
import it.pagopa.ecommerce.users.exceptions.UserNotFoundException
import it.pagopa.ecommerce.users.services.UserStatisticsService
import it.pagopa.generated.ecommerce.users.model.*
import java.time.ZoneId
import java.util.*
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.BDDMockito.*
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@WebFluxTest(LastUsageController::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
class LastUsageControllerTest {
    @Autowired lateinit var webClient: WebTestClient

    @MockBean lateinit var userStatisticsService: UserStatisticsService

    @Test
    fun `Should return last used method for valid userId`() = runTest {
        val lastUsageData = UserTestUtils.walletLastUsageDetails
        val userId = UserTestUtils.userId.toString()
        given(
                userStatisticsService.findUserLastMethodById(
                    userId = any(),
                )
            )
            .willReturn(mono { lastUsageData })

        webClient
            .get()
            .uri("/user/lastPaymentMethodUsed")
            .header("x-user-id", userId)
            .header("x-api-key", "primary-key")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(UserLastPaymentMethodData::class.java)
            .consumeWith {
                val actualResponse =
                    (it.responseBody as WalletLastUsageData).apply {
                        // workaround to set jackson parsed request to the system default
                        // one
                        this.date =
                            this.date.atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime()
                    }
                assertEquals(lastUsageData, actualResponse)
            }
    }

    @Test
    fun `Should return bad request for invalid UUID retrieving user last method usage data`() =
        runTest {
            val expectedErrorResponse =
                ProblemJson().apply {
                    this.status = HttpStatus.BAD_REQUEST.value()
                    this.title = "Bad request"
                    this.detail = "Input request is invalid."
                }
            webClient
                .get()
                .uri("/user/lastPaymentMethodUsed")
                .header("x-user-id", "invalid")
                .header("x-api-key", "primary-key")
                .exchange()
                .expectStatus()
                .isBadRequest
                .expectBody(ProblemJson::class.java)
                .isEqualTo(expectedErrorResponse)
        }

    @Test
    fun `Should return not found with not present user`() = runTest {
        val expectedErrorResponse =
            ProblemJson().apply {
                this.status = HttpStatus.NOT_FOUND.value()
                this.title = "User not found"
                this.detail = "The input user cannot be found"
            }
        val userId = UUID.randomUUID().toString()
        given(userStatisticsService.findUserLastMethodById(userId = any()))
            .willReturn(
                Mono.error(
                    UserNotFoundException(
                        message = "User with id [${userId}] not found",
                        cause = null
                    )
                )
            )

        webClient
            .get()
            .uri("/user/lastPaymentMethodUsed")
            .header("x-user-id", userId)
            .header("x-api-key", "primary-key")
            .exchange()
            .expectStatus()
            .isNotFound
            .expectBody(ProblemJson::class.java)
            .isEqualTo(expectedErrorResponse)
    }

    @Test
    fun `Should return bad request for invalid request saving user last method usage data`() =
        runTest {
            val expectedErrorResponse =
                ProblemJson().apply {
                    this.status = HttpStatus.BAD_REQUEST.value()
                    this.title = "Bad request"
                    this.detail = "Input request is invalid. Invalid fields: details"
                }
            val body =
                UserLastPaymentMethodRequest().apply {
                    this.userId = UserTestUtils.userId
                    this.details = null
                }
            webClient
                .put()
                .uri("/user/lastPaymentMethodUsed")
                .header("x-api-key", "primary-key")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isBadRequest
                .expectBody(ProblemJson::class.java)
                .isEqualTo(expectedErrorResponse)
        }

    @Test
    fun `Should return HTTP 204 created saving last usage to DB successfully`() = runTest {

        // pre-conditions
        val expectedRequest = UserTestUtils.guestMethodLastUsageRequest
        val userId = UserTestUtils.userId
        given(
                userStatisticsService.saveUserLastUsedMethodInfo(
                    userLastPaymentMethodRequest = any()
                )
            )
            .willReturn(mono {})
        // test
        webClient
            .put()
            .uri("/user/lastPaymentMethodUsed")
            .header("x-api-key", "primary-key")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(expectedRequest)
            .exchange()
            .expectStatus()
            .isNoContent
        verify(userStatisticsService, times(1))
            .saveUserLastUsedMethodInfo(
                userLastPaymentMethodRequest =
                    org.mockito.kotlin.argThat {
                        val actualRequest =
                            this.apply {
                                // workaround to set jackson parsed request to the system default
                                // one
                                val details = this.details as (GuestMethodLastUsageData)
                                details.date =
                                    details.date
                                        .atZoneSameInstant(ZoneId.systemDefault())
                                        .toOffsetDateTime()
                            }
                        assertEquals(expectedRequest, actualRequest)
                        true
                    }
            )
    }

    @ParameterizedTest
    @ValueSource(strings = ["invalid-key"])
    @NullSource
    fun `Should return HTTP 401 for invalid service api key`(apiKey: String?) = runTest {

        // pre-conditions
        val expectedRequest = UserTestUtils.guestMethodLastUsageRequest
        // test
        webClient
            .put()
            .uri("/user/lastPaymentMethodUsed")
            .header("x-api-key", apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(expectedRequest)
            .exchange()
            .expectStatus()
            .isUnauthorized
    }
}
