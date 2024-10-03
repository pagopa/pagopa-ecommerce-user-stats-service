package it.pagopa.ecommerce.users.exceptions.handlers

import it.pagopa.ecommerce.users.exceptions.ApiError
import it.pagopa.generated.ecommerce.users.model.ProblemJson
import jakarta.validation.ConstraintViolationException
import jakarta.validation.ValidationException
import java.util.stream.Collectors
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ServerWebInputException

/**
 * Exception handler used to map runtime exceptions to proper response http code and problem json
 * body
 */
@RestControllerAdvice
class ExceptionHandler {
    val logger: Logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val INVALID_REQUEST_ERROR_MESSAGE = "Input request is invalid."
    }

    @ExceptionHandler(ApiError::class)
    fun handleApiErrorException(exception: ApiError): ResponseEntity<ProblemJson> {
        logger.error("Exception processing the request", exception)
        val errorDetails = exception.errorDetails()
        return ResponseEntity.status(errorDetails.httpStatusCode)
            .body(
                ProblemJson()
                    .detail(errorDetails.description)
                    .status(errorDetails.httpStatusCode.value())
                    .title(errorDetails.title)
            )
    }

    /** Validation request exception handler */
    @ExceptionHandler(
        MethodArgumentNotValidException::class,
        MethodArgumentTypeMismatchException::class,
        ServerWebInputException::class,
        ValidationException::class,
        HttpMessageNotReadableException::class,
        WebExchangeBindException::class,
        ConstraintViolationException::class
    )
    fun handleRequestValidationException(exception: Exception): ResponseEntity<ProblemJson> {
        // stacktrace not logged to avoid logging of sensitive data such as mail
        logger.error(INVALID_REQUEST_ERROR_MESSAGE, exception)
        val validationErrorCause =
            when (exception) {
                is ConstraintViolationException ->
                    exception.constraintViolations
                        .stream()
                        .map { it.propertyPath.toString() }
                        .collect(Collectors.joining(","))
                is WebExchangeBindException ->
                    exception.bindingResult.allErrors
                        .stream()
                        .map {
                            if (it is FieldError) {
                                it.field
                            } else {
                                it.toString()
                            }
                        }
                        .collect(Collectors.joining(","))
                else -> null
            }
        val validationErrorMessage =
            if (validationErrorCause != null) {
                "$INVALID_REQUEST_ERROR_MESSAGE Invalid fields: $validationErrorCause"
            } else {
                INVALID_REQUEST_ERROR_MESSAGE
            }
        return ResponseEntity.badRequest()
            .body(
                ProblemJson()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .title("Bad request")
                    .detail(validationErrorMessage)
            )
    }

    /** Handler for generic exception */
    @ExceptionHandler(Throwable::class)
    fun handleGenericException(e: Throwable): ResponseEntity<ProblemJson> {
        logger.error("Exception processing the request", e)
        return ResponseEntity.internalServerError()
            .body(
                ProblemJson()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .title("Error processing the request")
                    .detail("Generic error occurred")
            )
    }
}
