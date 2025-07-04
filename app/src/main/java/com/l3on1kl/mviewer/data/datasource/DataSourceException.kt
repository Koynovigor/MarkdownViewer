package com.l3on1kl.mviewer.data.datasource

sealed class DataSourceException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {
    class ReadFailed(msg: String, cause: Throwable? = null) :
        DataSourceException(msg, cause)

    class WriteFailed(msg: String, cause: Throwable? = null) :
        DataSourceException(msg, cause)
}
