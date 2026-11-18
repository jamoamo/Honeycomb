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
import io.github.jamoamo.honeycomb.rest.response.ProblemDetail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Maps exceptions raised while handling a request to {@link ProblemDetail}s.
 *
 * <p>
 * Internal for now; intended to become a pluggable extension point.
 * </p>
 *
 * @author James Amoore
 * @since 1.1.0
 */
final class ExceptionProblemDetailMapper
{
   static final String INTERNAL_SERVER_ERROR_DETAIL = "An unexpected error occurred.";

   private static final int FIRST_SERVER_ERROR_STATUS = 500;

   private final Logger logger;

   /**
    * Constructor.
    */
   ExceptionProblemDetailMapper()
   {
      this(LoggerFactory.getLogger(ExceptionProblemDetailMapper.class));
   }

   /**
    * Constructor.
    *
    * @param logger the logger failures are reported to
    */
   ExceptionProblemDetailMapper(final Logger logger)
   {
      this.logger = logger;
   }

   /**
    * Maps the exception to a problem detail.
    *
    * @param exception the exception to map
    * @param request   the request being handled
    * @return the problem detail
    */
   ProblemDetail map(final Throwable exception, final RestRequest request)
   {
      return switch (exception)
      {
         case ProblemDetailException ex -> carried(ex, request);
         case BadRequestException ex ->
         {
            logger.debug("Bad request {} {}: {}", request.method(), request.path(), ex.getMessage());
            yield ProblemDetail.badRequest(ex.getMessage(), request.path(), null);
         }
         case null, default -> internalServerError(
            "Unhandled exception handling request " + request.method() + " " + request.path(), exception, request);
      };
   }

   /**
    * Creates a generic internal server error problem with a fresh error id, logging the id alongside the
    * message and cause so that the response can be correlated with the log entry.
    *
    * @param message the message to log
    * @param cause   the cause to log, possibly {@code null}
    * @param request the request being handled
    * @return the problem detail
    */
   ProblemDetail internalServerError(final String message, final Throwable cause, final RestRequest request)
   {
      String errorId = UUID.randomUUID().toString();
      logger.error("{} [errorId={}]", message, errorId, cause);
      return ProblemDetail.internalServerError(INTERNAL_SERVER_ERROR_DETAIL, request.path(), null)
         .withErrorId(errorId);
   }

   private ProblemDetail carried(final ProblemDetailException ex, final RestRequest request)
   {
      ProblemDetail detail = ex.problemDetail();
      if (detail.status() >= FIRST_SERVER_ERROR_STATUS)
      {
         logger.error("Request {} {} failed with status {} [code={}].",
            request.method(), request.path(), detail.status(), detail.code(), ex);
      }
      else
      {
         logger.warn("Request {} {} failed with status {} [code={}]: {}",
            request.method(), request.path(), detail.status(), detail.code(), detail.detail());
      }
      return detail;
   }
}
