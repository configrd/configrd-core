package com.appcrossings.common.util;

import javax.naming.OperationNotSupportedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.cxf.jaxrs.impl.WebApplicationExceptionMapper;
import org.codehaus.jackson.map.ObjectMapper;

import com.appcrossings.common.exceptions.BadRequestException;
import com.appcrossings.common.exceptions.BaseException;
import com.appcrossings.common.exceptions.EntityNotFoundException;
import com.appcrossings.common.exceptions.ErrorResponse;
import com.google.common.base.Throwables;

public class HttpResponseUtil {

  private static ObjectMapper mapper = new ObjectMapper();

  public static boolean isNotFound(Response resp) {
    return resp.getStatus() == Status.NOT_FOUND.getStatusCode();
  }

  public static boolean isOk(Response resp) {
    return resp.getStatus() == Status.OK.getStatusCode();
  }

  public static boolean isCreated(Response resp) {
    return resp.getStatus() == Status.CREATED.getStatusCode();
  }

  public static boolean isDeleted(Response resp) {
    return resp.getStatus() == Status.NO_CONTENT.getStatusCode();
  }

  public static boolean hasEntity(Response resp) {
    return resp.getEntity() != null;
  }

  public static boolean isSuccessful(Response resp) {
    return resp.getStatus() >= 200 && resp.getStatus() < 300;
  }

  public static boolean isServerError(Response resp) {
    return resp.getStatus() >= 500 && resp.getStatus() < 600;
  }

  public static boolean isBadRequest(Response resp) {
    return resp.getStatus() >= 400 && resp.getStatus() < 500;
  }

  public static boolean hasEntity(Response resp, Class<?> clazz) {
    return (hasEntity(resp) && resp.getEntity().getClass().isAssignableFrom(clazz));
  }

  public static boolean hasErrorEntity(Response resp) {

    boolean error = false;

    if (resp.getEntity() == null) {
      return false;
    } else if (resp.getEntity() instanceof String && (((String) resp.getEntity()).contains("extErrorCode"))) {
      error = true;
    } else if (resp.getEntity() instanceof ErrorResponse) {
      error = true;
    }

    return error;
  }

  public static ErrorResponse getErrorEntity(Response resp) {

    if (!hasErrorEntity(resp) || resp.getEntity() == null) {
      return null;
    }

    ErrorResponse err = null;

    if (resp.getEntity() instanceof ErrorResponse) {

      err = (ErrorResponse) resp.getEntity();

    } else if (resp.getEntity() instanceof String) {

      String errString = (String) resp.getEntity();

      try {
        err = mapper.readValue(errString, ErrorResponse.class);
      } catch (Exception e) {
        throw new IllegalArgumentException("Unable to map string to ErrorResponse. Maybe not an error");
      }

    }

    return err;

  }

  public static Response convertException(Throwable e) {

    Response resp = null;

    ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);

    Throwable exception = Throwables.getRootCause(e);

    if (exception instanceof BaseException) {

      ErrorResponse err = new ErrorResponse((BaseException) exception);

      if (exception instanceof EntityNotFoundException) {

        builder = Response.status(Status.NOT_FOUND);

      } else if (exception instanceof OperationNotSupportedException) {

        builder = Response.status(Status.METHOD_NOT_ALLOWED);

      } else if (exception instanceof BadRequestException) {
        builder = Response.status(Status.BAD_REQUEST);
      }

      resp = builder.entity(err).build();

    } else if (exception instanceof WebApplicationException) {

      WebApplicationExceptionMapper mapper = new WebApplicationExceptionMapper();
      resp = mapper.toResponse((WebApplicationException) exception);

    } else {

      if (exception instanceof IllegalArgumentException) {

        builder = Response.status(Status.BAD_REQUEST);

      }

      resp = builder.build();

    }

    return resp;
  }

}
