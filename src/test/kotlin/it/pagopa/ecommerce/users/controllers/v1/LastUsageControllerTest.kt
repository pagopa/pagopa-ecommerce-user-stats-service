package it.pagopa.ecommerce.users.controllers.v1

import it.pagopa.generated.ecommerce.users.model.ProblemJson
import java.util.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@WebFluxTest(LastUsageController::class)
class LastUsageControllerTest {
    @Autowired lateinit var webClient: WebTestClient

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
}
