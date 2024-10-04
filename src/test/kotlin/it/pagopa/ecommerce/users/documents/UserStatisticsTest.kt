package it.pagopa.ecommerce.users.documents

import it.pagopa.ecommerce.users.UserTestUtils
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class UserStatisticsTest {

    @Test
    fun `can build UserStatistics document`() {
        val walletType =
            UserTestUtils.userStatisticsByType(
                type = LastUsage.PaymentType.WALLET,
                instrumentId = UserTestUtils.lastUsageWalletId
            )
        val guestType =
            UserTestUtils.userStatisticsByType(
                type = LastUsage.PaymentType.GUEST,
                instrumentId = UserTestUtils.lastUsagePaymentMethodId
            )
        assertNotNull(walletType)
        assertNotNull(walletType.userId)
        assertNotNull(walletType.lastUsage)
        assertNotNull(walletType.lastUsage.type)
        assertNotNull(walletType.lastUsage.date)
        assertNotNull(walletType.lastUsage.instrumentId)
        assertNotNull(guestType)
        assertNotNull(guestType.userId)
        assertNotNull(guestType.lastUsage)
        assertNotNull(guestType.lastUsage.type)
        assertNotNull(guestType.lastUsage.date)
        assertNotNull(guestType.lastUsage.instrumentId)
    }
}
