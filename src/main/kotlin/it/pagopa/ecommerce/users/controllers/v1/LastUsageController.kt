package it.pagopa.ecommerce.users.controllers.v1

import it.pagopa.generated.ecommerce.users.api.UserApi
import it.pagopa.generated.ecommerce.users.model.UserLastPaymentMethodData
import it.pagopa.generated.ecommerce.users.model.WalletLastUsageData
import java.time.OffsetDateTime
import java.util.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController("LastUsageControllerV1")
class LastUsageController : UserApi {
    override fun getLastPaymentMethodUsed(
        xUserId: UUID,
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
        xUserId: UUID,
        userLastPaymentMethodDataDto: Mono<UserLastPaymentMethodData>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> =
        userLastPaymentMethodDataDto.flatMap {
            Mono.error(NotImplementedError("SAVE NOT IMPLEMENTED YET!"))
        }
}