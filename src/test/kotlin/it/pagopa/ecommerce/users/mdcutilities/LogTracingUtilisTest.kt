package it.pagopa.ecommerce.users.mdcutilities

import it.pagopa.ecommerce.users.mdcutilities.LogTracingUtils.AttributeKeys
import java.util.EnumMap
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*
import org.slf4j.Logger
import org.slf4j.MDC
import reactor.util.context.Context

class LogTracingUtilsTest {

    private lateinit var mockLogger: Logger

    @BeforeEach
    fun setUp() {
        mockLogger = mock(Logger::class.java)
        MDC.clear() // Ensure clean state before each test
    }

    @AfterEach
    fun tearDown() {
        MDC.clear() // Ensure clean state after each test
    }

    @Test
    fun testLogInfo_withSuccessAndAttributes() {
        // Arrange
        val attributes = EnumMap<AttributeKeys, String?>(AttributeKeys::class.java)
        attributes[AttributeKeys.EVENT_ACTION] = "test-action"
        attributes[AttributeKeys.CORRELATION_ID] = "12345"

        // Act
        // We use doAnswer to inspect MDC exactly when logger.info() is called
        doAnswer {
                assertEquals("test-action", MDC.get("event.action"))
                assertEquals("12345", MDC.get("correlation.id"))
                assertEquals("success", MDC.get("event.outcome"))
                null
            }
            .`when`(mockLogger)
            .info(anyString())

        LogTracingUtils.loggerTracingUtils()
            .attributes(attributes as Map<AttributeKeys, String>)
            .success()
            .logInfo(mockLogger, "Test info message")

        // Assert
        verify(mockLogger, times(1)).info("Test info message")
        // Verify Cleanup
        assertNull(MDC.get("event.action"), "MDC should be cleaned up after logging")
        assertNull(MDC.get("correlation.id"))
        assertNull(MDC.get("event.outcome"))
    }

    @Test
    fun testLogError_withExceptionAndStackTrace() {
        // Arrange
        val testException = RuntimeException("Something went wrong")

        doAnswer {
                assertEquals("failure", MDC.get("event.outcome"))
                assertEquals(RuntimeException::class.java.name, MDC.get("error.type"))
                assertEquals("Something went wrong", MDC.get("error.message"))
                assertNotNull(MDC.get("error.stack_trace"))
                assertTrue(MDC.get("error.stack_trace").contains("Something went wrong"))
                null
            }
            .`when`(mockLogger)
            .error(anyString())

        // Act
        LogTracingUtils.loggerTracingUtils()
            .failure()
            .logErrorWithStackTrace(mockLogger, testException, "Test error message")

        // Assert
        verify(mockLogger, times(1)).error("Test error message")
        assertNull(MDC.get("error.type"))
        assertNull(MDC.get("error.stack_trace"))
    }

    @Test
    fun testLogDebug_withDetailsAndDependency() {
        // Arrange
        val details = mapOf("userId" to "u-123", "retryCount" to "3")

        doAnswer {
                // Note: because LogTracingUtils puts dependencies into the details map,
                // we should parse the JSON to verify both the details and the dependency are
                // present
                val mdcDetails = MDC.get("ctx.details")
                assertNotNull(mdcDetails)
                assertTrue(mdcDetails.contains("\"userId\":\"u-123\""))
                assertTrue(mdcDetails.contains("\"retryCount\":\"3\""))
                assertTrue(mdcDetails.contains("\"dependency\":\"my-dependency\""))
                null
            }
            .`when`(mockLogger)
            .debug(anyString())

        // Act
        LogTracingUtils.loggerTracingUtils()
            .details(details)
            .dependency("my-dependency")
            .logDebug(mockLogger, "Test debug message")

        // Assert
        verify(mockLogger, times(1)).debug("Test debug message")
        assertNull(MDC.get("ctx.details"))
    }

    @Test
    fun testLogWarn_basic() {
        // Arrange
        doAnswer {
                val contextMap = MDC.getCopyOfContextMap()
                assertTrue(
                    contextMap.isNullOrEmpty(),
                    "MDC should be empty since no attributes were added"
                )
                null
            }
            .`when`(mockLogger)
            .warn(anyString())

        // Act
        LogTracingUtils.loggerTracingUtils().logWarn(mockLogger, "Warning message")

        // Assert
        verify(mockLogger, times(1)).warn("Warning message")
    }

    @Test
    fun testLogTrace_basic() {
        // Act
        LogTracingUtils.loggerTracingUtils().logTrace(mockLogger, "Trace message")

        // Assert
        verify(mockLogger, times(1)).trace("Trace message")
    }

    @Test
    fun testErrorWithoutMessage() {
        // Arrange
        val exceptionNoMessage = Exception() // No message provided

        doAnswer {
                assertEquals(Exception::class.java.name, MDC.get("error.type"))
                // Fallback to default value from AttributeKeysPrivate
                assertEquals("{errorMessage-not-found}", MDC.get("error.message"))
                null
            }
            .`when`(mockLogger)
            .error(anyString())

        // Act
        LogTracingUtils.loggerTracingUtils()
            .logError(mockLogger, exceptionNoMessage, "Error happened")

        // Assert
        verify(mockLogger, times(1)).error("Error happened")
    }

    @Test
    fun testNullAttributeKeysAndValuesAreIgnored() {
        // Arrange
        val attributes = EnumMap<AttributeKeys, String?>(AttributeKeys::class.java)
        attributes[AttributeKeys.CTX_USER_ID] = null // Null value

        doAnswer {
                assertNull(MDC.get("ctx.user.id"))
                null
            }
            .`when`(mockLogger)
            .info(anyString())

        // Act
        LogTracingUtils.loggerTracingUtils()
            // We cast because the builder method signature in Kotlin expects Map<AttributeKeys,
            // String>
            // and this is specifically testing the edge-case of null entries leaking in.
            .attributes(attributes as Map<AttributeKeys, String>)
            .logInfo(mockLogger, "Testing nulls")

        // Assert
        verify(mockLogger, times(1)).info("Testing nulls")
    }

    @Test
    fun shouldReturnSameContextWhenTracingEntriesAreNull() {
        // prerequisite
        val reactorContext = Context.of("existing-key", "existing-value")

        // test
        val enrichedContext = LogTracingUtils.enrichContextForEvent(null, reactorContext)

        // assertions
        assertSame(reactorContext, enrichedContext)
        assertEquals("existing-value", enrichedContext.get("existing-key"))
    }

    @Test
    fun shouldPreserveExistingContextEntriesWhenEnriching() {
        // prerequisite
        val existingContext = Context.of("pre-existing-key", "pre-existing-value")
        val tracingEntries = mapOf(AttributeKeys.EVENT_ACTION to "event_action")

        // test
        val enrichedContext = LogTracingUtils.enrichContextForEvent(tracingEntries, existingContext)

        // assertions
        assertEquals("pre-existing-value", enrichedContext.get("pre-existing-key"))
        assertEquals("event_action", enrichedContext.get(AttributeKeys.EVENT_ACTION.key))
    }

    @Test
    fun shouldEnrichContextUsingProvidedAndDefaultValues() {
        // prerequisite
        val tracingEntries = EnumMap<AttributeKeys, String?>(AttributeKeys::class.java)
        tracingEntries[AttributeKeys.EVENT_ACTION] = "event_action"
        tracingEntries[AttributeKeys.CORRELATION_ID] = null

        // test
        val enrichedContext = LogTracingUtils.enrichContextForEvent(tracingEntries, Context.empty())

        // assertions
        assertEquals("event_action", enrichedContext.get("event.action"))
        assertEquals("{correlationId-not-found}", enrichedContext.get("correlation.id"))
    }
}
