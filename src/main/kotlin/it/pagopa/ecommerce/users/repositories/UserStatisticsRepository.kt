package it.pagopa.ecommerce.users.repositories

import it.pagopa.ecommerce.users.documents.UserStatistics
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository

@Repository interface UserStatisticsRepository : ReactiveCrudRepository<UserStatistics, String> {}
