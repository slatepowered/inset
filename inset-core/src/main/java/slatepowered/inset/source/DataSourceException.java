package slatepowered.inset.source;

/**
 * All caught exceptions which may occur when executing commands
 * on a data source should be rethrown under (a subclass of) this exception, additionally
 * if a query fails (a subclass of) this exception should be thrown with an appropriate message.
 */
public class DataSourceException extends RuntimeException {

    public DataSourceException() {

    }

    public DataSourceException(String message) {
        super(message);
    }

    public DataSourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataSourceException(Throwable cause) {
        super(cause);
    }

    public DataSourceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
