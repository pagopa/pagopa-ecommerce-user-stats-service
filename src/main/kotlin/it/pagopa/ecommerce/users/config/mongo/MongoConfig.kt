package it.pagopa.ecommerce.users.config.mongo

import it.pagopa.ecommerce.users.config.mongo.converters.OffsetDateTimeReadingConverter
import it.pagopa.ecommerce.users.config.mongo.converters.OffsetDateTimeWritingConverter
import it.pagopa.ecommerce.users.config.mongo.converters.UUIDReadingConverter
import it.pagopa.ecommerce.users.config.mongo.converters.UUIDWritingConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.convert.MongoCustomConversions

/** Mongo configuration */
@Configuration
class MongoConfig {

    /** Register custom conversion implementation */
    @Bean
    fun mongoCustomConversions(): MongoCustomConversions {
        return MongoCustomConversions(
            listOf(
                OffsetDateTimeReadingConverter(),
                OffsetDateTimeWritingConverter(),
                UUIDReadingConverter(),
                UUIDWritingConverter(),
            )
        )
    }
}
