package it.pagopa.ecommerce.users.documents

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("user-stats")
data class UserStatistics(
    /** The user identifier */
    @Id val userId: String,
    /** The object of the last usage */
    val lastUsage: LastUsage
)
