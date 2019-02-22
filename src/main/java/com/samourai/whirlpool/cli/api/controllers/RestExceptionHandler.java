package com.samourai.whirlpool.cli.api.controllers;

import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.protocol.rest.RestErrorResponse;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @ExceptionHandler(value = {Exception.class})
  protected ResponseEntity<Object> handleException(Exception e) {
    NotifiableException notifiable = NotifiableException.computeNotifiableException(e);
    if (log.isDebugEnabled()) {
      log.debug("RestExceptionHandler: " + notifiable.getMessage());
    }
    RestErrorResponse restErrorResponse = new RestErrorResponse(notifiable.getMessage());
    HttpStatus httpStatus = HttpStatus.valueOf(notifiable.getStatus());
    return new ResponseEntity<>(restErrorResponse, httpStatus);
  }
}
