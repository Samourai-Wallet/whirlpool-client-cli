package com.samourai.whirlpool.cli.api.rest;

import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.protocol.rest.RestErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(value = {Exception.class})
  protected ResponseEntity<Object> handleException(Exception e) {
    NotifiableException notifiable = NotifiableException.computeNotifiableException(e);
    RestErrorResponse restErrorResponse = new RestErrorResponse(notifiable.getMessage());
    HttpStatus httpStatus = HttpStatus.valueOf(notifiable.getStatus());
    return new ResponseEntity<>(restErrorResponse, httpStatus);
  }
}
