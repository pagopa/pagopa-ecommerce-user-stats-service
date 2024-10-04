package it.pagopa.ecommerce.users.config.mongo.converters

import java.time.OffsetDateTime
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter

@ReadingConverter
class OffsetDateTimeReadingConverter : Converter<String, OffsetDateTime> {
    override fun convert(source: String): OffsetDateTime = OffsetDateTime.parse(source)
}

@WritingConverter
class OffsetDateTimeWritingConverter : Converter<OffsetDateTime, String> {

    override fun convert(source: OffsetDateTime): String = source.toString()
}
