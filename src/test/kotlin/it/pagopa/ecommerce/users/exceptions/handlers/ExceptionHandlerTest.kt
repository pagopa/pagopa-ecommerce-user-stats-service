package it.pagopa.ecommerce.users.exceptions.handlers

import it.pagopa.ecommerce.users.exceptions.UserNotFoundException
import jakarta.xml.bind.ValidationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class ExceptionHandlerTest {

    private val exceptionHandler = ExceptionHandler()

    @Test
    fun `Should return proper error response for unhandled exception`() = runTest {
        val exception = NullPointerException("test")
        val errorResponse = exceptionHandler.handleGenericException(exception)
        val expectedHttpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR
        assertEquals(expectedHttpStatusCode, errorResponse.statusCode)
        assertEquals(expectedHttpStatusCode.value(), errorResponse.body!!.status)
        assertEquals("Generic error occurred", errorResponse.body!!.detail)
        assertEquals("Error processing the request", errorResponse.body!!.title)
    }

    @Test
    fun `Should return proper error response for validation exception`() = runTest {
        val exception = ValidationException("invalid request")
        val errorResponse = exceptionHandler.handleRequestValidationException(exception)
        val expectedHttpStatusCode = HttpStatus.BAD_REQUEST
        assertEquals(expectedHttpStatusCode, errorResponse.statusCode)
        assertEquals(expectedHttpStatusCode.value(), errorResponse.body!!.status)
        assertEquals("Input request is invalid.", errorResponse.body!!.detail)
        assertEquals("Bad request", errorResponse.body!!.title)
    }

    @Test
    fun `Should return proper error response for custom api errors`() = runTest {
        val exception = UserNotFoundException(message = "error", cause = null)
        val errorResponse = exceptionHandler.handleApiErrorException(exception)
        val expectedHttpStatusCode = HttpStatus.NOT_FOUND
        assertEquals(expectedHttpStatusCode, errorResponse.statusCode)
        assertEquals(expectedHttpStatusCode.value(), errorResponse.body!!.status)
        assertEquals("The input user cannot be found", errorResponse.body!!.detail)
        assertEquals("User not found", errorResponse.body!!.title)
    }
}
