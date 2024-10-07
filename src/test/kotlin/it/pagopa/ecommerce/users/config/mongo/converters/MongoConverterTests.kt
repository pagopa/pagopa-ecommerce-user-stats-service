package it.pagopa.ecommerce.users.config.mongo.converters

import java.time.OffsetDateTime
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.core.convert.converter.Converter

class MongoConverterTests {

    companion object {
        private val offsetDateTimeWritingConverter = OffsetDateTimeWritingConverter()
        private val offsetDateTimeReadingConverter = OffsetDateTimeReadingConverter()
        private val uuidWritingConverter = UUIDWritingConverter()
        private val uuidReadingConverter = UUIDReadingConverter()

        @JvmStatic
        fun `Mongo type converter method source`(): Stream<Arguments> =
            Stream.of(
                Arguments.of(UUID.randomUUID(), uuidReadingConverter, uuidWritingConverter),
                Arguments.of(
                    OffsetDateTime.now(),
                    offsetDateTimeReadingConverter,
                    offsetDateTimeWritingConverter
                )
            )
    }

    @ParameterizedTest
    @MethodSource("Mongo type converter method source")
    fun <S : Any, T : Any> `Should round trip offset date time conversion`(
        initialValue: S,
        readConverter: Converter<T, S>,
        writeConverter: Converter<S, T>
    ) = runTest {
        val serialized = writeConverter.convert(initialValue)
        val deserialized = readConverter.convert(serialized!!)
        assertEquals(initialValue, deserialized)
    }
}
