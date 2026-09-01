package it.pagopa.ecommerce.users.mdcutilities.exceptions

/**
 * Exception thrown when an error occurs during the execution of logging or tracing utilities.
 *
 * This runtime exception is typically used within the logging utilities (e.g., `LogTracingUtils`)
 * to indicate invalid states, missing mandatory configurations, or unexpected behaviors during log
 * building and context enrichment.
 *
 * @param message the detail message explaining the reason for the exception, which is saved for
 *   later retrieval by the [Throwable.message] property
 */
class LogTracingUtilException(message: String) : RuntimeException(message)
