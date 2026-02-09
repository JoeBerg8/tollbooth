package com.tollbooth.validation;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@SuppressWarnings("checkstyle:methodParamPad")
public enum ErrorCode {
  BAD_REQUEST(HttpStatus.BAD_REQUEST),
  INVALID_PARAMETER(HttpStatus.BAD_REQUEST),
  CONFLICT(HttpStatus.CONFLICT),
  NOT_FOUND(HttpStatus.NOT_FOUND),
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
  PAYMENT_REQUIRED(HttpStatus.PAYMENT_REQUIRED),
  FORBIDDEN(HttpStatus.FORBIDDEN),
  PRECONDITION_FAILED(HttpStatus.PRECONDITION_FAILED),
  OK(HttpStatus.OK),
  GMAIL_REAUTH_REQUIRED(HttpStatus.FORBIDDEN),
  UNKNOWN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

  private final HttpStatus status;

  ErrorCode(HttpStatus status) {
    this.status = status;
  }
}
