package it.pagopa.ecommerce.users.documents

import java.time.OffsetDateTime
import java.util.*

/** Common interface for last usage method details */
sealed interface LastUsageMethodDetails

/** Details for wallet method */
data class WalletLastUsageMethodDetails(val walletId: UUID) : LastUsageMethodDetails

/** Details for guest payment method */
data class GuestLastUsageMethodDetails(val paymentMethodId: UUID) : LastUsageMethodDetails

/** Data class containing last usage information */
data class LastUsage(
    val details: LastUsageMethodDetails,
    /** The date of the last usage */
    val date: OffsetDateTime
)
