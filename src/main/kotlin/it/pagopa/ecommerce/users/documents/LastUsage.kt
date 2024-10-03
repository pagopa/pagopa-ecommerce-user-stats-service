package it.pagopa.ecommerce.users.documents

import java.time.Instant
import java.util.*

data class LastUsage(
    /** The payment type, saved_wallet or guest_payment_method */
    val type: PaymentType,
    /** The id of the method used to pay */
    val instrumentId: UUID,
    /** The date of the last usage */
    val date: Instant
) {

    /** Payment type enumeration */
    enum class PaymentType {
        /** User wallet */
        WALLET,

        /** Guest payment method */
        GUEST
    }
}
