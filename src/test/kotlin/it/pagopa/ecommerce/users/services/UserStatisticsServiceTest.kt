package it.pagopa.ecommerce.users.services

import it.pagopa.ecommerce.users.UserTestUtils
import it.pagopa.ecommerce.users.documents.LastUsage
import it.pagopa.ecommerce.users.documents.UserStatistics
import it.pagopa.ecommerce.users.repositories.UserStatisticsRepository
import it.pagopa.generated.ecommerce.users.model.UserLastPaymentMethodData
import java.util.*
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import reactor.core.publisher.Hooks
import reactor.test.StepVerifier

class UserStatisticsServiceTest {

    private val userStatisticsRepository: UserStatisticsRepository = mock()

    private val userStatisticsArgumentCaptor: ArgumentCaptor<UserStatistics> =
        ArgumentCaptor.forClass(UserStatistics::class.java)

    private val userStatisticsService =
        UserStatisticsService(userStatisticsRepository = userStatisticsRepository)

    companion object {

        @JvmStatic
        fun `Last used payment methods to save method source`(): Stream<Arguments> =
            Stream.of(
                // guest method
                Arguments.of(
                    UserTestUtils.userId,
                    UserTestUtils.guestMethodLastUsageData,
                    UserStatistics(
                        userId = UserTestUtils.userId.toString(),
                        lastUsage =
                            LastUsage(
                                type = LastUsage.PaymentType.GUEST,
                                date = UserTestUtils.lastUsageDate,
                                instrumentId = UserTestUtils.lastUsagePaymentMethodId
                            )
                    )
                ),
                // wallet method
                Arguments.of(
                    UserTestUtils.userId,
                    UserTestUtils.walletLastUsageData,
                    UserStatistics(
                        userId = UserTestUtils.userId.toString(),
                        lastUsage =
                            LastUsage(
                                type = LastUsage.PaymentType.WALLET,
                                date = UserTestUtils.lastUsageDate,
                                instrumentId = UserTestUtils.lastUsageWalletId
                            )
                    )
                )
            )
    }

    @ParameterizedTest
    @MethodSource("Last used payment methods to save method source")
    fun `Should save user last used method`(
        userId: UUID,
        requestDto: UserLastPaymentMethodData,
        expectedSavedObject: UserStatistics
    ) = runTest {
        // pre-condition
        given(userStatisticsRepository.save(userStatisticsArgumentCaptor.capture())).willAnswer {
            mono { it.arguments[0] }
        }

        // test
        Hooks.onOperatorDebug()
        StepVerifier.create(
                userStatisticsService.saveUserLastUsedMethodInfo(
                    userId = userId,
                    userLastPaymentMethodData = requestDto
                )
            )
            .expectNext(Unit)
            .verifyComplete()

        // assertions
        val savedObject = userStatisticsArgumentCaptor.allValues[0]
        assertEquals(expectedSavedObject, savedObject)
    }

    @Test
    fun `Should throw error for unhandled UserLastPaymentMethodData`() = runTest {
        // pre-condition
        val userLastPaymentMethodData: UserLastPaymentMethodData = mock()

        // test
        Hooks.onOperatorDebug()
        StepVerifier.create(
                userStatisticsService.saveUserLastUsedMethodInfo(
                    userId = UUID.randomUUID(),
                    userLastPaymentMethodData = userLastPaymentMethodData
                )
            )
            .expectError(IllegalArgumentException::class.java)
            .verify()
    }
}
