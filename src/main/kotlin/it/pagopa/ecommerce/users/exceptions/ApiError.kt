package it.pagopa.ecommerce.users.exceptions

import org.springframework.http.HttpStatus

/**
 * Abstract api error class: extend this class with custom exception making explicit exception to
 * http error response mapping this exception is handled by the exception handler class that
 * automatically will output the right ProblemJson response object
 */
abstract class ApiError(override val message: String?, override val cause: Throwable?) :
    RuntimeException(message, cause) {

    /** Data class containing error details */
    data class ErrorDetails(
        val httpStatusCode: HttpStatus,
        val title: String,
        val description: String
    )

    abstract fun errorDetails(): ErrorDetails
}
