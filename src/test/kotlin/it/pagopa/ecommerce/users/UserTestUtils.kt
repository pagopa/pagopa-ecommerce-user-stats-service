package it.pagopa.ecommerce.users

import it.pagopa.ecommerce.users.documents.GuestLastUsageMethodDetails
import it.pagopa.ecommerce.users.documents.LastUsage
import it.pagopa.ecommerce.users.documents.UserStatistics
import it.pagopa.ecommerce.users.documents.WalletLastUsageMethodDetails
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

    val walletLastUsageUserStatistics: UserStatistics =
        UserStatistics(
            userId = userId.toString(),
            lastUsage =
                LastUsage(
                    date = lastUsageDate,
                    details = WalletLastUsageMethodDetails(walletId = lastUsageWalletId)
                )
        )

    val guestLastUsageUserStatistics: UserStatistics =
        UserStatistics(
            userId = userId.toString(),
            lastUsage =
                LastUsage(
                    date = lastUsageDate,
                    details =
                        GuestLastUsageMethodDetails(paymentMethodId = lastUsagePaymentMethodId)
                )
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
