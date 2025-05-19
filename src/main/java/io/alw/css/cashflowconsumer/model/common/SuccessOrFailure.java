package io.alw.css.cashflowconsumer.model.common;

public sealed interface SuccessOrFailure {
    record Success<T>(String msg, T outcome) implements SuccessOrFailure {
    }

    record Failure(String msg, Exception e) implements SuccessOrFailure {
    }
}
