package it.pagopa.ecommerce.users.warmup.utils

import it.pagopa.generated.ecommerce.users.model.UserLastPaymentMethodRequest
import it.pagopa.generated.ecommerce.users.model.WalletLastUsageData
import java.time.OffsetDateTime
import java.util.*

object WarmupUtils {
    const val LAST_PAYMENT_METHOD_USED_PATH = "http://localhost:8080/user/lastPaymentMethodUsed"
    const val X_USER_ID_HEADER_KEY = "x-user-id"
    val zeroUUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    val warmupLastMethodUsedBody =
        UserLastPaymentMethodRequest().apply {
            this.userId = zeroUUID
            this.details =
                WalletLastUsageData().apply {
                    this.walletId = zeroUUID
                    this.date = OffsetDateTime.now()
                }
        }
}
