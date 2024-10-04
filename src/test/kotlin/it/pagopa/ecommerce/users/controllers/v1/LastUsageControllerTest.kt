package it.pagopa.ecommerce.users.controllers.v1

import it.pagopa.ecommerce.users.UserTestUtils
import it.pagopa.ecommerce.users.services.UserStatisticsService
import it.pagopa.generated.ecommerce.users.model.GuestMethodLastUsageData
import it.pagopa.generated.ecommerce.users.model.ProblemJson
import java.time.ZoneId
import java.util.*
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.*
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient

@WebFluxTest(LastUsageController::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
class LastUsageControllerTest {
    @Autowired lateinit var webClient: WebTestClient

    @MockBean lateinit var userStatisticsService: UserStatisticsService

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
                .exchange()
                .expectStatus()
                .isBadRequest
                .expectBody(ProblemJson::class.java)
                .isEqualTo(expectedErrorResponse)
        }

    @Test
    fun `Should return bad request for invalid UUID saving user last method usage data`() =
        runTest {
            val expectedErrorResponse =
                ProblemJson().apply {
                    this.status = HttpStatus.BAD_REQUEST.value()
                    this.title = "Bad request"
                    this.detail = "Input request is invalid."
                }
            val body =
                """
            {
                "type": "wallet",
                "walletId": "e20284cf-88f8-4b3b-aaa7-5f909becb8e2",
                "date": "2024-10-03T16:13:26.628548+02:00"
            }
        """
                    .trimIndent()
            webClient
                .post()
                .uri("/user/lastPaymentMethodUsed")
                .header("x-user-id", "invalid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isBadRequest
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
                    this.detail = "Input request is invalid. Invalid fields: walletId"
                }
            val body =
                """
            {
                "type": "wallet",
                "date": "2024-10-03T16:13:26.628548+02:00"
            }
        """
                    .trimIndent()
            webClient
                .post()
                .uri("/user/lastPaymentMethodUsed")
                .header("x-user-id", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isBadRequest
                .expectBody(ProblemJson::class.java)
                .isEqualTo(expectedErrorResponse)
        }

    @Test
    fun `Should return HTTP 201 created saving last usage to DB successfully`() = runTest {

        // pre-conditions
        val expectedRequest = UserTestUtils.guestMethodLastUsageData
        val userId = UserTestUtils.userId
        given(
                userStatisticsService.saveUserLastUsedMethodInfo(
                    userId = any(),
                    userLastPaymentMethodData = any()
                )
            )
            .willReturn(mono {})
        // test
        webClient
            .post()
            .uri("/user/lastPaymentMethodUsed")
            .header("x-user-id", userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(expectedRequest)
            .exchange()
            .expectStatus()
            .isCreated
        verify(userStatisticsService, times(1))
            .saveUserLastUsedMethodInfo(
                userId = org.mockito.kotlin.eq(userId),
                userLastPaymentMethodData =
                    org.mockito.kotlin.argThat {
                        val actualRequest =
                            (this as GuestMethodLastUsageData).apply {
                                // workaround to set jackson parsed request to the system default
                                // one
                                this.date =
                                    this.date
                                        .atZoneSameInstant(ZoneId.systemDefault())
                                        .toOffsetDateTime()
                            }
                        assertEquals(expectedRequest, actualRequest)
                        true
                    }
            )
    }
}
