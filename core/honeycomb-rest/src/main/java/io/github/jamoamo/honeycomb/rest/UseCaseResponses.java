/*
 * The MIT License
 *
 * Copyright 2026 James Amoore.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.github.jamoamo.honeycomb.rest;

import io.github.jamoamo.honeycomb.rest.response.ApiResponse;
import io.github.jamoamo.honeycomb.rest.response.FieldError;
import io.github.jamoamo.honeycomb.rest.response.ProblemDetail;
import io.github.jamoamo.honeycomb.rest.response.RestResponse;
import io.github.jamoamo.honeycomb.types.ErrorCategory;
import io.github.jamoamo.honeycomb.types.ErrorCode;
import io.github.jamoamo.honeycomb.usecase.UseCaseResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Framework-neutral helpers for turning a {@link UseCaseResult} into an HTTP response.
 *
 * <p>
 * These helpers hold the standard Honeycomb mapping of use case outcomes to HTTP so that any REST framework can
 * reuse it. Combine them with {@link UseCaseResult#fold} to produce whatever response type the framework expects:
 * </p>
 *
 * <pre>{@code
 * return result.fold(
 *    ResponseEntity::ok,
 *    failure -> ResponseEntity.status(UseCaseResponses.status(failure))
 *       .body(UseCaseResponses.problemDetail(failure, request.getRequestURI())));
 * }</pre>
 *
 * <p>
 * Server-side failures (status 500 and above) are reported with a generic detail and a fresh error id instead of
 * the failure description, since the description may contain internal information. The description is logged
 * against the error id so the response can be correlated with the log entry.
 * </p>
 *
 * @author James Amoore
 * @since 1.1.0
 */
public final class UseCaseResponses
{
   /**
    * The detail reported for server-side failures in place of the failure description.
    */
   public static final String INTERNAL_SERVER_ERROR_DETAIL =
      ExceptionProblemDetailMapper.INTERNAL_SERVER_ERROR_DETAIL;

   private static final int OK = 200;
   private static final int FIRST_SERVER_ERROR_STATUS = 500;

   private static final Logger LOGGER = LoggerFactory.getLogger(UseCaseResponses.class);

   private UseCaseResponses()
   {
   }

   /**
    * The HTTP status code a failure of the supplied category maps to.
    *
    * @param category the error category
    * @return the HTTP status code, 500 for a category with no specific mapping
    */
   public static int status(final ErrorCategory category)
   {
      return HttpStatus.of(category).code();
   }

   /**
    * The HTTP status code the supplied failure maps to.
    *
    * @param failure the failure
    * @return the HTTP status code
    */
   public static int status(final UseCaseResult.Failure<?> failure)
   {
      return status(requireFailure(failure).errorCode().category());
   }

   /**
    * The HTTP status code the supplied result maps to: 200 for a success, otherwise the failure's status.
    *
    * @param result the result
    * @return the HTTP status code
    */
   public static int status(final UseCaseResult<?> result)
   {
      requireResult(result);
      return switch (result)
      {
         case UseCaseResult.Success<?> success -> OK;
         case UseCaseResult.Failure<?> failure -> status(failure);
      };
   }

   /**
    * The validation errors of the supplied failure as response field errors.
    *
    * @param failure the failure
    * @return the field errors, empty if the failure carries no validation errors
    */
   public static List<FieldError> fieldErrors(final UseCaseResult.Failure<?> failure)
   {
      return requireFailure(failure).validationErrors().stream()
         .map(e -> new FieldError(e.field(), e.message()))
         .toList();
   }

   /**
    * Maps the supplied failure to a problem detail.
    *
    * @param failure  the failure
    * @param instance the request path the failure occurred on
    * @return the problem detail
    */
   public static ProblemDetail problemDetail(final UseCaseResult.Failure<?> failure, final String instance)
   {
      return toProblemDetail(failure, instance, instance);
   }

   /**
    * Maps the supplied result to a Honeycomb {@link RestResponse}: an {@link ApiResponse} carrying the output on
    * success, otherwise the failure's {@link ProblemDetail}.
    *
    * @param result   the result
    * @param instance the request path being handled
    * @return the response
    */
   public static RestResponse toRestResponse(final UseCaseResult<?> result, final String instance)
   {
      return toRestResponse(result, instance, instance);
   }

   /**
    * Maps the supplied result, describing the request in the log entry of a server-side failure.
    *
    * @param result             the result
    * @param instance           the request path being handled
    * @param requestDescription how the request is described in the log
    * @return the response
    */
   static RestResponse toRestResponse(
      final UseCaseResult<?> result,
      final String instance,
      final String requestDescription)
   {
      requireResult(result);
      return switch (result)
      {
         case UseCaseResult.Success<?> success -> ApiResponse.of(success.output());
         case UseCaseResult.Failure<?> failure -> toProblemDetail(failure, instance, requestDescription);
      };
   }

   private static ProblemDetail toProblemDetail(
      final UseCaseResult.Failure<?> failure,
      final String instance,
      final String requestDescription)
   {
      ErrorCode error = requireFailure(failure).errorCode();
      HttpStatus status = HttpStatus.of(error.category());
      List<FieldError> errors = fieldErrors(failure);

      if (status.code() >= FIRST_SERVER_ERROR_STATUS)
      {
         String errorId = UUID.randomUUID().toString();
         LOGGER.error("Use case failed handling request {} [code={}]: {} [errorId={}]",
            requestDescription, error.code(), error.description(), errorId);
         return ProblemDetail.forStatus(
               status.code(), status.title(), INTERNAL_SERVER_ERROR_DETAIL, instance, error.code())
            .withErrorId(errorId)
            .withErrors(errors);
      }

      return ProblemDetail.forStatus(status.code(), status.title(), error.description(), instance, error.code())
         .withErrors(errors);
   }

   private static void requireResult(final UseCaseResult<?> result)
   {
      Objects.requireNonNull(result, "result cannot be null");
   }

   private static UseCaseResult.Failure<?> requireFailure(final UseCaseResult.Failure<?> failure)
   {
      return Objects.requireNonNull(failure, "failure cannot be null");
   }

   /**
    * The HTTP statuses use case failures map to.
    */
   private enum HttpStatus
   {
      BAD_REQUEST(ErrorCategory.VALIDATION, 400, "Bad Request"),
      UNAUTHORIZED(ErrorCategory.UNAUTHORISED, 401, "Unauthorized"),
      FORBIDDEN(ErrorCategory.FORBIDDEN, 403, "Forbidden"),
      NOT_FOUND(ErrorCategory.NOT_FOUND, 404, "Not Found"),
      CONFLICT(ErrorCategory.CONFLICT, 409, "Conflict"),
      PRECONDITION_FAILED(ErrorCategory.PRECONDITION_FAILED, 412, "Precondition Failed"),
      TOO_MANY_REQUESTS(ErrorCategory.RATE_LIMITED, 429, "Too Many Requests"),
      INTERNAL_SERVER_ERROR(ErrorCategory.INTERNAL, 500, "Internal Server Error"),
      SERVICE_UNAVAILABLE(ErrorCategory.UNAVAILABLE, 503, "Service Unavailable"),
      GATEWAY_TIMEOUT(ErrorCategory.TIMEOUT, 504, "Gateway Timeout");

      private final ErrorCategory category;
      private final int code;
      private final String title;

      HttpStatus(final ErrorCategory category, final int code, final String title)
      {
         this.category = category;
         this.code = code;
         this.title = title;
      }

      int code()
      {
         return code;
      }

      String title()
      {
         return title;
      }

      static HttpStatus of(final ErrorCategory category)
      {
         for (HttpStatus status : values())
         {
            if (status.category == category)
            {
               return status;
            }
         }
         return INTERNAL_SERVER_ERROR;
      }
   }
}
