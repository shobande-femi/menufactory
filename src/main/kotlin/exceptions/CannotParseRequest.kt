package exceptions

import java.lang.Exception

class CannotParseRequest(message: String) : Exception(message)