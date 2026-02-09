package com.tollbooth.validation;

import java.util.function.Supplier;

public class Validation {

  /**
   * Check that the {@code condition} is true, or raise a {@link ServiceException} with the {@link
   * String#format(String, Object...)} style {@code content} and {@code args}.
   *
   * @param condition the condition to check
   * @param errorCode the error code to use if the condition is false
   * @param message the content
   * @param args the content args
   */
  public static void check(boolean condition, ErrorCode errorCode, String message, Object... args) {
    if (!condition) {
      raiseServiceException(errorCode, message, args);
    }
  }

  /**
   * Return a {@link Supplier} set to raise an {@link ServiceException} with the {@code errorCode}
   * and {@link String#format(String, Object...)} style {@code content} and {@code args}.
   *
   * @param errorCode the error code
   * @param message the content
   * @param args the content args
   * @return an exception supplier
   */
  public static Supplier<ServiceException> serviceExceptionSupplier(
      ErrorCode errorCode, String message, Object... args) {
    return () -> serviceException(errorCode, message, args);
  }

  /**
   * Raise a {@link ServiceException} with the {@link String#format(String, Object...)} style {@code
   * content} and {@code args}.
   *
   * @param errorCode the error code
   * @param message the content
   * @param args the content args
   */
  public static void raiseServiceException(ErrorCode errorCode, String message, Object... args) {
    throw new ServiceException(errorCode, String.format(message, args));
  }

  /**
   * Build a {@link ServiceException} with the {@link String#format(String, Object...)} style {@code
   * content} and {@code args}.
   *
   * @param errorCode the error code
   * @param message the content
   * @param args the content args
   * @return the exception
   */
  public static ServiceException serviceException(
      ErrorCode errorCode, String message, Object... args) {
    return new ServiceException(errorCode, String.format(message, args));
  }

  /**
   * Wrap the unexpected {@code exception} in a {@link ServiceException} with a {@link
   * ErrorCode#INTERNAL_SERVER_ERROR} code and the {@link String#format(String, Object...)} style
   * {@code content} and {@code args}.
   *
   * @param exception the exception
   * @param message the content
   * @param args the content args
   * @return the exception
   */
  public static ServiceException unexpectedException(
      Exception exception, String message, Object... args) {
    return new ServiceException(
        ErrorCode.INTERNAL_SERVER_ERROR, String.format(message, args), exception);
  }
}
