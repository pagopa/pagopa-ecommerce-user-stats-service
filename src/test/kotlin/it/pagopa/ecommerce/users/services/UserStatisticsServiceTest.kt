package it.pagopa.ecommerce.users.services

import it.pagopa.ecommerce.users.UserTestUtils
import it.pagopa.ecommerce.users.documents.LastUsage
import it.pagopa.ecommerce.users.documents.UserStatistics
import it.pagopa.ecommerce.users.exceptions.UserNotFoundException
import it.pagopa.ecommerce.users.repositories.UserStatisticsRepository
import it.pagopa.generated.ecommerce.users.model.UserLastPaymentMethodData
import it.pagopa.generated.ecommerce.users.model.UserLastPaymentMethodRequest
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
import org.mockito.Mockito.*
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono
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
                    UserTestUtils.guestMethodLastUsageRequest,
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
                    UserTestUtils.walletLastUsageRequest,
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

        @JvmStatic
        fun `Last used payment methods to get method source`(): Stream<Arguments> =
            Stream.of(
                // wallet method
                Arguments.of(
                    UserTestUtils.userId,
                    UserTestUtils.userStatisticsByType(
                        LastUsage.PaymentType.WALLET,
                        UserTestUtils.lastUsageWalletId
                    ),
                    UserTestUtils.walletLastUsageDetails
                ),
                // guest method
                Arguments.of(
                    UserTestUtils.userId,
                    UserTestUtils.userStatisticsByType(
                        LastUsage.PaymentType.GUEST,
                        UserTestUtils.lastUsagePaymentMethodId,
                    ),
                    UserTestUtils.guestLastUsageDetails
                )
            )
    }

    @ParameterizedTest
    @MethodSource("Last used payment methods to save method source")
    fun `Should save user last used method`(
        requestDto: UserLastPaymentMethodRequest,
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
                    userLastPaymentMethodRequest = requestDto
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
        val userLastPaymentMethodData: UserLastPaymentMethodRequest = mock()
        given(userLastPaymentMethodData.userId).willReturn(UserTestUtils.userId)
        given(userLastPaymentMethodData.details).willReturn(mock())

        // test
        Hooks.onOperatorDebug()
        StepVerifier.create(
                userStatisticsService.saveUserLastUsedMethodInfo(
                    userLastPaymentMethodRequest = userLastPaymentMethodData
                )
            )
            .expectError(IllegalArgumentException::class.java)
            .verify()
    }

    @ParameterizedTest
    @MethodSource("Last used payment methods to get method source")
    fun `Should retrieve lastUsed method successfully`(
        userId: UUID,
        userStatistics: UserStatistics,
        expectedLastUsageDate: UserLastPaymentMethodData
    ) {
        given(userStatisticsRepository.findById(userId.toString()))
            .willReturn(Mono.just(userStatistics))

        StepVerifier.create(userStatisticsService.findUserLastMethodById(userId.toString()))
            .expectNext(expectedLastUsageDate)
            .verifyComplete()

        verify(userStatisticsRepository, times(1))
            .findById(org.mockito.kotlin.eq(userId.toString()))
    }

    @Test
    fun `Should return error for not found user`() {
        val userId = UserTestUtils.userId.toString()
        given(userStatisticsRepository.findById(userId)).willReturn(Mono.empty())

        StepVerifier.create(userStatisticsService.findUserLastMethodById(userId))
            .expectError(UserNotFoundException::class.java)
            .verify()

        verify(userStatisticsRepository, times(1)).findById(org.mockito.kotlin.eq(userId))
    }
}
