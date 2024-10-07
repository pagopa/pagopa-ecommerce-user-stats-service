package it.pagopa.ecommerce.users.documents

import it.pagopa.ecommerce.users.UserTestUtils
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class UserStatisticsTest {

    companion object {

        @JvmStatic
        fun `User statistics method source`(): Stream<Arguments> =
            Stream.of(
                // guest method
                Arguments.of(
                    UserTestUtils.userStatisticsByType(
                        type = LastUsage.PaymentType.WALLET,
                        instrumentId = UserTestUtils.lastUsageWalletId
                    )
                ),
                // wallet method
                Arguments.of(
                    UserTestUtils.userStatisticsByType(
                        type = LastUsage.PaymentType.GUEST,
                        instrumentId = UserTestUtils.lastUsagePaymentMethodId
                    )
                )
            )
    }

    @ParameterizedTest
    @MethodSource("User statistics method source")
    fun `Can build UserStatistics document`(userStatistics: UserStatistics) {
        assertNotNull(userStatistics)
        assertNotNull(userStatistics.userId)
        assertNotNull(userStatistics.lastUsage)
        assertNotNull(userStatistics.lastUsage.type)
        assertNotNull(userStatistics.lastUsage.date)
        assertNotNull(userStatistics.lastUsage.instrumentId)
    }
}
