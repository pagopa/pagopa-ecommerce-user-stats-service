package it.pagopa.ecommerce.users

import it.pagopa.ecommerce.users.documents.LastUsage
import it.pagopa.ecommerce.users.documents.UserStatistics
import java.time.Instant
import java.util.*

object UserTestUtils {

    fun userStatisticsByType(type: LastUsage.PaymentType): UserStatistics =
        UserStatistics(
            userId = UUID.randomUUID().toString(),
            lastUsage =
                LastUsage(type = type, instrumentId = UUID.randomUUID(), date = Instant.now())
        )
}
