package com.l3on1kl.mviewer.data.datasource

sealed class DataSourceException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {
    class ReadFailed(message: String, cause: Throwable? = null) :
        DataSourceException(message, cause)

    class WriteFailed(message: String, cause: Throwable? = null) :
        DataSourceException(message, cause)
}
