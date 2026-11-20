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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The HTTP response a rest controller adapter method produces: a status, headers and an optional body.
 *
 * <p>
 * The body is an {@link ApiResponse} for a successful response, a {@link ProblemDetail} for an error, or absent
 * for a response that carries no content such as {@code 204 No Content}.
 * </p>
 *
 * @author James Amoore
 * @param status  the HTTP status code the response is written with
 * @param headers the response headers, by name; never {@code null}
 * @param body    the response body, empty when the response carries no content
 * @since 1.1.0
 */
public record RestResponse(int status, Map<String, List<String>> headers, Optional<RestResponseBody> body)
{
   private static final int STATUS_OK = 200;
   private static final int STATUS_NO_CONTENT = 204;

   /**
    * Constructor.
    */
   public RestResponse
   {
      headers = copyHeaders(headers);
      body = Objects.requireNonNull(body, "body cannot be null");
   }

   /**
    * A {@code 200 OK} response carrying the supplied body.
    *
    * @param body the body
    * @return the response
    */
   public static RestResponse ok(final ApiResponse<?> body)
   {
      return new RestResponse(STATUS_OK, Map.of(), Optional.of(Objects.requireNonNull(body, "ok body cannot be null")));
   }

   /**
    * An error response carrying the supplied problem, written with the problem's status.
    *
    * @param problem the problem
    * @return the response
    */
   public static RestResponse problem(final ProblemDetail problem)
   {
      Objects.requireNonNull(problem, "problem cannot be null");
      return new RestResponse(problem.status(), Map.of(), Optional.of(problem));
   }

   /**
    * A {@code 204 No Content} response.
    *
    * @return the response
    */
   public static RestResponse noContent()
   {
      return new RestResponse(STATUS_NO_CONTENT, Map.of(), Optional.empty());
   }

   /**
    * Returns a copy of this response with the supplied header value added.
    *
    * @param name  the header name
    * @param value the header value
    * @return the copy
    */
   public RestResponse withHeader(final String name, final String value)
   {
      return new RestResponse(status, addHeader(headers, name, value), body);
   }

   /**
    * Returns a copy of this response with the supplied headers added.
    *
    * @param additional the headers to add, by name
    * @return the copy
    */
   public RestResponse withHeaders(final Map<String, List<String>> additional)
   {
      Map<String, List<String>> merged = headers;
      for (Map.Entry<String, List<String>> entry : copyHeaders(additional).entrySet())
      {
         for (String value : entry.getValue())
         {
            merged = addHeader(merged, entry.getKey(), value);
         }
      }
      return new RestResponse(status, merged, body);
   }

   private static String requireHeaderName(final String name)
   {
      return Objects.requireNonNull(name, "header name cannot be null");
   }

   private static Map<String, List<String>> addHeader(
      final Map<String, List<String>> headers,
      final String name,
      final String value)
   {
      requireHeaderName(name);
      Objects.requireNonNull(value, "header value cannot be null");
      Map<String, List<String>> copy = new LinkedHashMap<>(headers);
      List<String> values = new ArrayList<>(copy.getOrDefault(name, List.of()));
      values.add(value);
      copy.put(name, values);
      return copy;
   }

   private static Map<String, List<String>> copyHeaders(final Map<String, List<String>> headers)
   {
      Objects.requireNonNull(headers, "headers cannot be null");
      Map<String, List<String>> copy = new LinkedHashMap<>();
      headers.forEach((name, values) -> copy.put(
         requireHeaderName(name),
         List.copyOf(Objects.requireNonNull(values, "header values cannot be null"))));
      return Collections.unmodifiableMap(copy);
   }
}
