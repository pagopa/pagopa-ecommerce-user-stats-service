package it.pagopa.ecommerce.users.documents

import java.time.OffsetDateTime
import java.util.*

sealed interface LastUsageMethodDetails

data class WalletLastUsageMethodDetails(val walletId: UUID) : LastUsageMethodDetails

data class GuestLastUsageMethodDetails(val paymentMethodId: UUID) : LastUsageMethodDetails

data class LastUsage(
    val details: LastUsageMethodDetails,
    /** The date of the last usage */
    val date: OffsetDateTime
)
