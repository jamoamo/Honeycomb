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
import io.github.jamoamo.honeycomb.rest.response.ApiResponse;
import io.github.jamoamo.honeycomb.rest.response.FieldError;
import io.github.jamoamo.honeycomb.rest.response.ProblemDetail;
import io.github.jamoamo.honeycomb.rest.response.RestResponse;
import io.github.jamoamo.honeycomb.types.ErrorCategory;
import io.github.jamoamo.honeycomb.types.ErrorCode;
import io.github.jamoamo.honeycomb.usecase.UseCaseResult;

import java.util.List;

/**
 * Maps a {@link UseCaseResult} returned by a rest controller adapter method to a {@link RestResponse}.
 *
 * <p>
 * A success becomes an {@link ApiResponse} carrying the output. A failure becomes a {@link ProblemDetail} whose
 * status is derived from the {@link ErrorCategory} of the failure, whose code is the failure code, and which
 * carries any validation errors. Server-side failures are reported with a generic detail and an error id
 * instead of the failure description, since the description may contain internal information.
 * </p>
 *
 * @author James Amoore
 * @since 1.1.0
 */
final class UseCaseResultMapper
{
   private static final int FIRST_SERVER_ERROR_STATUS = 500;

   private final ExceptionProblemDetailMapper exceptionMapper;

   /**
    * Constructor.
    *
    * @param exceptionMapper the mapper used to build and log internal server errors
    */
   UseCaseResultMapper(final ExceptionProblemDetailMapper exceptionMapper)
   {
      this.exceptionMapper = exceptionMapper;
   }

   /**
    * Maps the use case result.
    *
    * @param result  the result to map
    * @param request the request being handled
    * @return the response
    */
   RestResponse map(final UseCaseResult<?> result, final RestRequest request)
   {
      return switch (result)
      {
         case UseCaseResult.Success<?> success -> ApiResponse.of(success.output());
         case UseCaseResult.Failure<?> failure -> failure(failure, request);
      };
   }

   private ProblemDetail failure(final UseCaseResult.Failure<?> failure, final RestRequest request)
   {
      ErrorCode error = failure.errorCode();
      HttpStatus status = HttpStatus.of(error.category());
      List<FieldError> errors = failure.validationErrors().stream()
         .map(e -> new FieldError(e.field(), e.message()))
         .toList();

      if (status.code() >= FIRST_SERVER_ERROR_STATUS)
      {
         ProblemDetail internal = exceptionMapper.internalServerError(
            "Use case failed handling request " + request.method() + " " + request.path()
               + " [code=" + error.code() + "]: " + error.description(),
            null,
            request);
         return ProblemDetail.forStatus(status.code(), status.title(), internal.detail(), request.path(), error.code())
            .withErrorId(internal.errorId())
            .withErrors(errors);
      }

      return ProblemDetail.forStatus(status.code(), status.title(), error.description(), request.path(), error.code())
         .withErrors(errors);
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
