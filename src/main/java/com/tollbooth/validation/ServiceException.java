package com.tollbooth.validation;

import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {

  private final ErrorCode errorCode;

  public ServiceException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public ServiceException(ErrorCode errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }
}
