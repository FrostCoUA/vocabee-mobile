package com.vocabee.android.core.domain

sealed interface DataError : Error {
    enum class Local : DataError {
        DISK_FULL,
        UNKNOWN,
    }

    enum class Remote : DataError {
        REQUEST_TIMEOUT,
        TOO_MANY_REQUESTS,
        NO_INTERNET,
        SERVER,
        SERIALIZATION,
        UNAUTHORIZED,
        UNKNOWN,
    }
}
