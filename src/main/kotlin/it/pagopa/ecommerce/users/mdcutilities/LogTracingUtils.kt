package it.pagopa.ecommerce.users.mdcutilities

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import it.pagopa.ecommerce.users.mdcutilities.exceptions.LogTracingUtilException
import java.util.EnumMap
import org.slf4j.Logger
import org.slf4j.MDC
import org.slf4j.event.Level
import reactor.util.context.Context

/**
 * Utility class for structured logging utilizing the Fluent Builder pattern.
 *
 * This class facilitates the population of the SLF4J Mapped Diagnostic Context (MDC) with
 * predefined attributes, custom details, and error information. It ensures that MDC keys are safely
 * added before logging and properly cleaned up immediately after the log is emitted, preventing
 * context leaks in concurrent environments.
 */
class LogTracingUtils private constructor() {
    private var outcome: String? = null
    private var message: String? = null
    private var error: Throwable? = null
    private var stackTrace: String? = null
    private var attributes: Map<AttributeKeys, String> = EnumMap(AttributeKeys::class.java)
    private val details: MutableMap<String, String> = hashMapOf()
    private var logger: Logger? = null

    private val mdcKeys: MutableList<String> = mutableListOf()

    enum class AttributeKeys(val key: String, val defaultValue: String) {
        EVENT_ACTION("event_action", "{eventAction-not-found}"),
        CTX_TRANSACTION_ID("ctx_transaction_id", "{transactionId-not-found}"),
        CTX_AUTHORIZATION_REQUEST_ID(
            "ctx_authorization_request_id",
            "{authorizationRequestId-not-found}"
        ),
        CTX_EVENT_CODE("ctx_event_code", "{eventCode-not-found}"),
        CTX_EVENT_ID("ctx_event_id", "{eventId-not-found}"),
        CTX_RPT_IDS("ctx_rpt_ids", "{rptIds-not-found}"),
        CTX_PAYMENT_TOKENS("ctx_payment_tokens", "{paymentTokens-not-found}"),
        CTX_USER_ID("ctx_user_id", "{userId-not-found}"),
        CORRELATION_ID("correlation_id", "{correlationId-not-found}"),
        PSP_ID("psp_id", "{pspId-not-found}")
    }

    private enum class AttributeKeysPrivate(val key: String, val defaultValue: String) {
        CTX_DETAILS("ctx_details", "{details-not-found}"),
        EVENT_OUTCOME("event_outcome", "{eventOutcome-not-found}"),
        DEPENDENCY("dependency", "{dependency-not-found}"),
        ERROR_TYPE("error.type", "{errorType-not-found}"),
        ERROR_MESSAGE("error.message", "{errorMessage-not-found}"),
        ERROR_STACK_TRACE("error.stack_trace", "{errorStackTrace-not-found}")
    }

    fun attributes(attributes: Map<AttributeKeys, String>) = apply { this.attributes = attributes }

    fun details(details: Map<String, String>) = apply { this.details.putAll(details) }

    fun dependency(dependency: String) = apply {
        this.details[AttributeKeysPrivate.DEPENDENCY.key] = dependency
    }

    fun success() = apply { this.outcome = SUCCESS }

    fun failure() = apply { this.outcome = FAILURE }

    private fun addMdcKey(key: String, value: String) {
        MDC.put(key, value)
        mdcKeys.add(key)
    }

    fun logInfo(logger: Logger, message: String) {
        this.message = message
        this.logger = logger
        log(Level.INFO)
    }

    fun logDebug(logger: Logger, message: String) {
        this.message = message
        this.logger = logger
        log(Level.DEBUG)
    }

    fun logWarn(logger: Logger, message: String) {
        this.message = message
        this.logger = logger
        log(Level.WARN)
    }

    fun logTrace(logger: Logger, message: String) {
        this.message = message
        this.logger = logger
        log(Level.TRACE)
    }

    fun logError(logger: Logger, error: Throwable?, message: String) {
        this.logger = logger
        this.message = message
        this.error = error
        log(Level.ERROR)
    }

    fun logErrorWithStackTrace(logger: Logger, error: Throwable, message: String) {
        this.stackTrace = error.stackTraceToString()
        logError(logger, error, message)
    }

    private fun log(loggerLevel: Level?) {
        val currentLogger = logger ?: throw LogTracingUtilException("logger is null.")
        val currentMessage = message ?: ""

        if (attributes.isNotEmpty()) {
            attributes.forEach { (key, value) ->
                MDC.put(key.key, value)
                mdcKeys.add(key.key)
            }
        }

        if (details.isNotEmpty()) {
            addMdcKey(AttributeKeysPrivate.CTX_DETAILS.key, serializeDetailsToMdcMap(details))
        }

        outcome?.let { addMdcKey(AttributeKeysPrivate.EVENT_OUTCOME.key, it) }

        error?.let { err ->
            addMdcKey(AttributeKeysPrivate.ERROR_TYPE.key, err.javaClass.name)
            addMdcKey(
                AttributeKeysPrivate.ERROR_MESSAGE.key,
                err.message ?: AttributeKeysPrivate.ERROR_MESSAGE.defaultValue
            )
        }

        stackTrace?.let { addMdcKey(AttributeKeysPrivate.ERROR_STACK_TRACE.key, it) }

        when (loggerLevel) {
            Level.INFO -> currentLogger.info(currentMessage)
            Level.WARN -> currentLogger.warn(currentMessage)
            Level.DEBUG -> currentLogger.debug(currentMessage)
            Level.TRACE -> currentLogger.trace(currentMessage)
            Level.ERROR -> currentLogger.error(currentMessage)
            null -> throw LogTracingUtilException("loggerLevel null or not defined.")
        }

        // Cleanup MDC
        mdcKeys.forEach { MDC.remove(it) }
        mdcKeys.clear()
    }

    companion object {
        private const val SUCCESS = "success"
        private const val FAILURE = "failure"

        const val MONGO_DEPENDENCY = "eCommerce-mongodb"
        const val REDIS_DEPENDENCY = "eCommerce-redis"

        private val OBJECT_MAPPER =
            ObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        @JvmStatic
        fun loggerTracingUtils(): LogTracingUtils {
            return LogTracingUtils()
        }

        private fun serializeDetailsToMdcMap(details: Map<String, *>?): String {
            if (details == null) return "{}"
            return try {
                OBJECT_MAPPER.writeValueAsString(details)
            } catch (ignored: JsonProcessingException) {
                "{}"
            }
        }

        fun enrichContextForEvent(
            tracingEntries: Map<AttributeKeys, String?>?,
            reactorContext: Context
        ): Context {
            var enrichedContext = reactorContext
            tracingEntries?.forEach { (key, value) ->
                enrichedContext = enrichedContext.put(key.key, value ?: key.defaultValue)
            }
            return enrichedContext
        }
    }
}
