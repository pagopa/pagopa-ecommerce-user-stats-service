package it.pagopa.ecommerce.users

import it.pagopa.ecommerce.users.documents.LastUsage
import it.pagopa.ecommerce.users.documents.UserStatistics
import it.pagopa.generated.ecommerce.users.model.GuestMethodLastUsageData
import it.pagopa.generated.ecommerce.users.model.UserLastPaymentMethodRequest
import it.pagopa.generated.ecommerce.users.model.WalletLastUsageData
import java.time.OffsetDateTime
import java.util.*

object UserTestUtils {

    val userId: UUID = UUID.randomUUID()

    val lastUsagePaymentMethodId: UUID = UUID.randomUUID()

    val lastUsageWalletId: UUID = UUID.randomUUID()

    val lastUsageDate: OffsetDateTime = OffsetDateTime.now()

    fun userStatisticsByType(type: LastUsage.PaymentType, instrumentId: UUID): UserStatistics =
        UserStatistics(
            userId = userId.toString(),
            lastUsage = LastUsage(type = type, instrumentId = instrumentId, date = lastUsageDate)
        )

    val guestLastUsageDetails =
        GuestMethodLastUsageData().apply {
            this.paymentMethodId = lastUsagePaymentMethodId
            this.date = lastUsageDate
            this.type = "guest"
        }

    val walletLastUsageDetails =
        WalletLastUsageData().apply {
            this.walletId = lastUsageWalletId
            this.date = lastUsageDate
            this.type = "wallet"
        }
    val guestMethodLastUsageRequest =
        UserLastPaymentMethodRequest().apply {
            this.userId = UserTestUtils.userId
            this.details = guestLastUsageDetails
        }

    val walletLastUsageRequest =
        UserLastPaymentMethodRequest().apply {
            this.userId = UserTestUtils.userId
            this.details = walletLastUsageDetails
        }
}
