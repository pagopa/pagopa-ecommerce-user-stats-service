package it.pagopa.ecommerce.users.config.mongo.converters

import java.util.*
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

@ReadingConverter
class UUIDReadingConverter : Converter<String, UUID> {
    override fun convert(source: String): UUID = UUID.fromString(source)
}

@WritingConverter
class UUIDWritingConverter : Converter<UUID, String> {
    override fun convert(source: UUID): String = source.toString()
}
