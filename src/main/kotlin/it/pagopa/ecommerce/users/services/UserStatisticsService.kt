package it.pagopa.ecommerce.users.services

import it.pagopa.ecommerce.users.documents.LastUsage
import it.pagopa.ecommerce.users.documents.UserStatistics
import it.pagopa.ecommerce.users.exceptions.UserNotFoundException
import it.pagopa.ecommerce.users.repositories.UserStatisticsRepository
import it.pagopa.generated.ecommerce.users.model.GuestMethodLastUsageData
import it.pagopa.generated.ecommerce.users.model.UserLastPaymentMethodData
import it.pagopa.generated.ecommerce.users.model.WalletLastUsageData
import java.util.*
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
    }

    /** Save user last payment method data */
    fun saveUserLastUsedMethodInfo(
        userId: UUID,
        userLastPaymentMethodData: UserLastPaymentMethodData
    ): Mono<Unit> {
        return mono { userLastPaymentMethodData }
            .map {
                it.let {
                    UserStatistics(
                        userId = userId.toString(),
                        lastUsage =
                            when (it) {
                                is GuestMethodLastUsageData ->
                                    LastUsage(
                                        type = LastUsage.PaymentType.GUEST,
                                        instrumentId = it.paymentMethodId,
                                        date = it.date
                                    )
                                is WalletLastUsageData ->
                                    LastUsage(
                                        type = LastUsage.PaymentType.WALLET,
                                        instrumentId = it.walletId,
                                        date = it.date
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
        when (lastUsage.type) {
            LastUsage.PaymentType.WALLET ->
                WalletLastUsageData()
                    .walletId(lastUsage.instrumentId)
                    .date(lastUsage.date)
            LastUsage.PaymentType.GUEST ->
                GuestMethodLastUsageData()
                    .paymentMethodId(lastUsage.instrumentId)
                    .date(lastUsage.date)
                    .type(lastUsage.type.toString().lowercase(Locale.getDefault()))
        }
}
