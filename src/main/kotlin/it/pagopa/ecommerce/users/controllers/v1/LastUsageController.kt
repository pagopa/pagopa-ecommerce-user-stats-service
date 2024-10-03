package it.pagopa.ecommerce.users.controllers.v1

import it.pagopa.generated.ecommerce.users.api.UserApi
import it.pagopa.generated.ecommerce.users.model.UserLastPaymentMethodData
import it.pagopa.generated.ecommerce.users.model.WalletLastUsageData
import java.time.OffsetDateTime
import java.util.*
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController("LastUsageControllerV1")
@Validated
class LastUsageController : UserApi {
    override fun getLastPaymentMethodUsed(
        userId: UUID,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<UserLastPaymentMethodData>> =
        Mono.just(
            ResponseEntity.ok(
                WalletLastUsageData()
                    .date(OffsetDateTime.now())
                    .walletId(UUID.randomUUID())
                    .type("wallet")
            )
        )

    override fun saveLastPaymentMethodUsed(
        userId: UUID,
        userLastPaymentMethodDataDto: Mono<UserLastPaymentMethodData>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> {
        TODO("Save api not implemented yet!")
    }
}
