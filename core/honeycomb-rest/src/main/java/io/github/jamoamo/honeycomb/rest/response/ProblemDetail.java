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
package io.github.jamoamo.honeycomb.rest.response;

/**
 * An RFC 9457 problem details document, the shape every API error response is surfaced in so that error
 * handling is consistent across endpoints and integrations.
 *
 * @author James Amoore
 * @param type     a URI reference identifying the problem type, {@code about:blank} when unspecified
 * @param title    a short, human-readable summary of the problem type
 * @param status   the HTTP status code for this occurrence of the problem
 * @param detail   a human-readable explanation specific to this occurrence of the problem
 * @param instance a URI reference identifying this occurrence of the problem, typically the request path
 * @param code     a stable, machine-readable identifier of the specific failure
 * @since 1.0.0
 */
public record ProblemDetail(String type, String title, int status, String detail, String instance, String code)
{
   /**
    * The problem type used when no specific type is supplied.
    */
   public static final String BLANK_TYPE = "about:blank";

   private static final int STATUS_BAD_REQUEST = 400;
   private static final int STATUS_INTERNAL_SERVER_ERROR = 500;

   /**
    * Creates a {@code 400 Bad Request} problem.
    *
    * @param detail   the explanation of the problem
    * @param instance the request path the problem occurred on
    * @param code     the machine-readable failure identifier
    * @return the problem
    */
   public static ProblemDetail badRequest(final String detail, final String instance, final String code)
   {
      return forStatus(STATUS_BAD_REQUEST, "Bad Request", detail, instance, code);
   }

   /**
    * Creates a {@code 500 Internal Server Error} problem.
    *
    * @param detail   the explanation of the problem
    * @param instance the request path the problem occurred on
    * @param code     the machine-readable failure identifier
    * @return the problem
    */
   public static ProblemDetail internalServerError(final String detail, final String instance, final String code)
   {
      return forStatus(STATUS_INTERNAL_SERVER_ERROR, "Internal Server Error", detail, instance, code);
   }

   /**
    * Creates a problem for the given status.
    *
    * @param status   the HTTP status code
    * @param title    the summary of the problem type
    * @param detail   the explanation of the problem
    * @param instance the request path the problem occurred on
    * @param code     the machine-readable failure identifier
    * @return the problem
    */
   public static ProblemDetail forStatus(
      final int status,
      final String title,
      final String detail,
      final String instance,
      final String code)
   {
      return new ProblemDetail(BLANK_TYPE, title, status, detail, instance, code);
   }
}
