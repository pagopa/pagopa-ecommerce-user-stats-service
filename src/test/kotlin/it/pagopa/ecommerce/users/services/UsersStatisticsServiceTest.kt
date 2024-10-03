package it.pagopa.ecommerce.users.services

import it.pagopa.ecommerce.users.UserTestUtils
import it.pagopa.ecommerce.users.documents.LastUsage
import it.pagopa.ecommerce.users.repositories.UserStatisticsRepository
import java.util.UUID
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.*
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class UsersStatisticsServiceTest {

    private val userStatisticsRepository: UserStatisticsRepository = mock()
    private val userStatisticsService = UserStatisticsService(userStatisticsRepository)

    @Test
    fun `Should retrieve lastUsed method successfully`() {
        val userStatistics = UserTestUtils.userStatisticsByType(LastUsage.PaymentType.WALLET)
        val userId = userStatistics.userId
        given(userStatisticsRepository.findById(userId)).willReturn(Mono.just(userStatistics))

        StepVerifier.create(userStatisticsService.findUserLastMethodById(userId))
            .expectNext(userStatistics.lastUsage)
            .verifyComplete()

        verify(userStatisticsRepository, times(1)).findById(userId)
    }

    @Test
    fun `Should return error for not found user`() {
        val userId = UUID.randomUUID().toString()
        given(userStatisticsRepository.findById(userId)).willReturn(Mono.empty())

        StepVerifier.create(userStatisticsService.findUserLastMethodById(userId))
            .expectError(Exception::class.java)
            .verify()

        verify(userStatisticsRepository, times(1)).findById(userId)
    }
}
