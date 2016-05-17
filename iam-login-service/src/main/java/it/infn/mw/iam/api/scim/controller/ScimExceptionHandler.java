package it.infn.mw.iam.api.scim.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import it.infn.mw.iam.api.scim.exception.IllegalArgumentException;
import it.infn.mw.iam.api.scim.exception.ResourceNotFoundException;
import it.infn.mw.iam.api.scim.model.ErrorResponse;

@ControllerAdvice
public class ScimExceptionHandler extends ResponseEntityExceptionHandler {

  public static final Logger LOG = LoggerFactory
    .getLogger(ScimExceptionHandler.class);
  
  
  @ResponseStatus(code = HttpStatus.BAD_REQUEST)
  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseBody
  public ErrorResponse handleInvalidArgumentException(
    IllegalArgumentException e) {

    return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
  }

  @ResponseStatus(code = HttpStatus.NOT_FOUND)
  @ExceptionHandler(ResourceNotFoundException.class)
  @ResponseBody
  public ErrorResponse handleResourceNotFoundException(
    ResourceNotFoundException nfe) {

    return buildErrorResponse(HttpStatus.NOT_FOUND, nfe.getMessage());
  }

  private ErrorResponse buildErrorResponse(HttpStatus status, String message) {

    return new ErrorResponse(status.value(), message);
  }
}
