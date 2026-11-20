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

import io.github.jamoamo.honeycomb.rest.request.RestRequest;
import io.github.jamoamo.honeycomb.rest.response.FieldError;
import io.github.jamoamo.honeycomb.rest.response.ProblemDetail;
import io.github.jamoamo.honeycomb.types.ErrorCategory;
import io.github.jamoamo.honeycomb.types.ErrorCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The default {@link FailureProblemMapper}, which derives the status of the problem from the
 * {@link ErrorCategory} of the error code.
 *
 * <p>
 * Client-side failures carry the error code description as their detail. Server-side failures (status 500 and
 * above) are reported with a generic detail and a fresh error id instead, since the description may contain
 * internal information; the description is logged against the error id so the response can be correlated with
 * the log entry.
 * </p>
 *
 * @author James Amoore
 * @since 1.1.0
 */
public final class DefaultFailureProblemMapper implements FailureProblemMapper
{
   /**
    * The detail reported for server-side failures in place of the error code description.
    */
   public static final String INTERNAL_SERVER_ERROR_DETAIL =
      ExceptionProblemDetailMapper.INTERNAL_SERVER_ERROR_DETAIL;

   private static final int FIRST_SERVER_ERROR_STATUS = 500;

   private final Logger logger;

   /**
    * Constructor.
    */
   public DefaultFailureProblemMapper()
   {
      this(LoggerFactory.getLogger(DefaultFailureProblemMapper.class));
   }

   /**
    * Constructor.
    *
    * @param logger the logger server-side failures are reported to
    */
   DefaultFailureProblemMapper(final Logger logger)
   {
      this.logger = logger;
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
    * Maps the failure to a problem detail whose status is derived from the error category.
    *
    * @param errorCode the error code identifying the failure
    * @param errors    the field-level errors of the failure
    * @param request   the request being handled
    * @return the problem detail
    */
   @Override
   public ProblemDetail map(final ErrorCode errorCode, final List<FieldError> errors, final RestRequest request)
   {
      Objects.requireNonNull(errorCode, "errorCode cannot be null");
      HttpStatus status = HttpStatus.of(errorCode.category());

      if (status.code() >= FIRST_SERVER_ERROR_STATUS)
      {
         String errorId = UUID.randomUUID().toString();
         logger.error("Request {} {} failed [code={}]: {} [errorId={}]",
            request.method(), request.path(), errorCode.code(), errorCode.description(), errorId);
         return ProblemDetail.forStatus(
               status.code(), status.title(), INTERNAL_SERVER_ERROR_DETAIL, request.path(), errorCode.code())
            .withErrorId(errorId)
            .withErrors(errors);
      }

      return ProblemDetail.forStatus(
            status.code(), status.title(), errorCode.description(), request.path(), errorCode.code())
         .withErrors(errors);
   }

   /**
    * The HTTP statuses failures map to.
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
