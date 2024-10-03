package it.pagopa.ecommerce.users.services

import it.pagopa.ecommerce.users.documents.LastUsage
import it.pagopa.ecommerce.users.repositories.UserStatisticsRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class UserStatisticsService(
    @Autowired private val userStatisticsRepository: UserStatisticsRepository
) {

    fun findUserLastMethodById(userId: String): Mono<LastUsage> {
        return userStatisticsRepository
            .findById(userId)
            .switchIfEmpty(Mono.error(Exception("User not found")))
            .map { it.lastUsage }
    }
}
