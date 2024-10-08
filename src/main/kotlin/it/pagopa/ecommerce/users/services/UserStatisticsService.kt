package it.pagopa.ecommerce.users.services

import it.pagopa.ecommerce.users.documents.GuestLastUsageMethodDetails
import it.pagopa.ecommerce.users.documents.LastUsage
import it.pagopa.ecommerce.users.documents.UserStatistics
import it.pagopa.ecommerce.users.documents.WalletLastUsageMethodDetails
import it.pagopa.ecommerce.users.exceptions.UserNotFoundException
import it.pagopa.ecommerce.users.repositories.UserStatisticsRepository
import it.pagopa.generated.ecommerce.users.model.GuestMethodLastUsageData
import it.pagopa.generated.ecommerce.users.model.UserLastPaymentMethodData
import it.pagopa.generated.ecommerce.users.model.UserLastPaymentMethodRequest
import it.pagopa.generated.ecommerce.users.model.WalletLastUsageData
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/** User statistic service class: this class handle user statistics information */
@Service
class UserStatisticsService(
    @Autowired private val userStatisticsRepository: UserStatisticsRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /** Find user last method by id */
    fun findUserLastMethodById(userId: String): Mono<UserLastPaymentMethodData> {
        logger.info("Finding last method used for userId: [{}]", userId)
        return userStatisticsRepository
            .findById(userId)
            .switchIfEmpty(
                Mono.error {
                    UserNotFoundException(
                        message = "User with id [${userId}] not found",
                        cause = null
                    )
                }
            )
            .map { mapUserStatisticsToUserLastPaymentMethodData(it.lastUsage) }
            .doOnNext { logger.info("Last used data found for userId: [{}] -> {}", userId, it) }
    }

    /** Save user last payment method data */
    fun saveUserLastUsedMethodInfo(
        userLastPaymentMethodRequest: UserLastPaymentMethodRequest
    ): Mono<Unit> {
        val userId = userLastPaymentMethodRequest.userId
        val userLastPaymentMethodData = userLastPaymentMethodRequest.details
        logger.info(
            "Saving last used method for userId: [{}]. Last method used data: [{}]",
            userId,
            userLastPaymentMethodData
        )
        return mono { userLastPaymentMethodData }
            .map {
                it.let {
                    UserStatistics(
                        userId = userId.toString(),
                        lastUsage =
                            when (it) {
                                is GuestMethodLastUsageData ->
                                    LastUsage(
                                        date = it.date,
                                        details =
                                            GuestLastUsageMethodDetails(
                                                paymentMethodId = it.paymentMethodId
                                            )
                                    )
                                is WalletLastUsageData ->
                                    LastUsage(
                                        date = it.date,
                                        details =
                                            WalletLastUsageMethodDetails(walletId = it.walletId)
                                    )
                                else ->
                                    throw IllegalArgumentException(
                                        "UserLastPaymentMethodData: [$userLastPaymentMethodData] not handled"
                                    )
                            }
                    )
                }
            }
            .flatMap { userStatisticsRepository.save(it) }
            .thenReturn(Unit)
    }

    private fun mapUserStatisticsToUserLastPaymentMethodData(
        lastUsage: LastUsage
    ): UserLastPaymentMethodData =
        when (lastUsage.details) {
            is WalletLastUsageMethodDetails ->
                WalletLastUsageData()
                    .walletId(lastUsage.details.walletId)
                    .date(lastUsage.date)
                    .type("wallet")
            is GuestLastUsageMethodDetails ->
                GuestMethodLastUsageData()
                    .paymentMethodId(lastUsage.details.paymentMethodId)
                    .date(lastUsage.date)
                    .type("guest")
        }
}
