package it.pagopa.ecommerce.users.exceptions

import org.springframework.http.HttpStatus

/** exception thrown where the user cannot be found */
class UserNotFoundException(override val message: String?, override val cause: Throwable?) :
    ApiError(message = message, cause = cause) {
    override fun errorDetails() =
        ErrorDetails(
            httpStatusCode = HttpStatus.NOT_FOUND,
            description = "The input user cannot be found",
            title = "User not found"
        )
}
