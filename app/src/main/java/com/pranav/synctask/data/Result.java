package com.pranav.synctask.data;

/**
 * A generic wrapper class to represent the state of a data request.
 * It can be in a Loading, Success, or Error state.
 */
public class Result<T> {
    private Result() {}

    public static final class Loading<T> extends Result<T> {}

    public static final class Success<T> extends Result<T> {
        public final T data;
        public Success(T data) {
            this.data = data;
        }
    }

    public static final class Error<T> extends Result<T> {
        public final Exception exception;
        public Error(Exception exception) {
            this.exception = exception;
        }
    }
}