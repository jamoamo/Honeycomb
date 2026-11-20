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

import io.github.jamoamo.honeycomb.rest.response.FieldError;
import io.github.jamoamo.honeycomb.rest.response.ProblemDetail;
import io.github.jamoamo.honeycomb.types.ErrorCode;
import io.github.jamoamo.honeycomb.usecase.UseCaseResult;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * The outcome of a rest controller adapter endpoint method: either a successful response body, or an error.
 *
 * <p>
 * Endpoint methods return this type so that their signature states what they produce on success while still
 * allowing them to fail, without tying the adapter to a web framework. There are three variants:
 * </p>
 * <ul>
 * <li>{@link Ok}: a success, carrying the response body and a {@code 2xx} or {@code 3xx} status;</li>
 * <li>{@link Problem}: an error the endpoint has described itself as a {@link ProblemDetail};</li>
 * <li>{@link Failed}: an error identified by an {@link ErrorCode}, mapped to a {@link ProblemDetail} by the
 * configured {@link FailureProblemMapper} when the response is written.</li>
 * </ul>
 *
 * <p>
 * A use case result is converted with {@link #from(UseCaseResult, Function)}, or with
 * {@link #respond(UseCaseResult, Function)} when the success needs more than a {@code 200 OK}:
 * </p>
 *
 * <pre>{@code
 * @Endpoint(path = "/", method = HttpVerb.POST)
 * public EndpointResult<UserResponse> registerUser(final UserRequest request)
 * {
 *    return EndpointResult.respond(
 *       registerUser.execute(mapper.toInput(request)),
 *       user -> EndpointResult.created(mapper.toResponse(user), URI.create("/users/" + user.id())));
 * }
 * }</pre>
 *
 * @author James Amoore
 * @param <T> the type of the response body on success
 * @since 1.1.0
 */
public sealed interface EndpointResult<T>
{
   /**
    * The headers the response is written with, by name.
    *
    * @return the headers; never {@code null}
    */
   Map<String, List<String>> headers();

   /**
    * Returns a copy of this result with the supplied header value added.
    *
    * @param name  the header name
    * @param value the header value
    * @return the copy
    */
   EndpointResult<T> withHeader(String name, String value);

   /**
    * A successful outcome.
    *
    * @author James Amoore
    * @param <T>     the type of the response body
    * @param status  the HTTP status, {@code 2xx} or {@code 3xx}
    * @param body    the response body, null for a response without data; ignored for {@code 204 No Content}
    * @param headers the response headers, by name
    * @since 1.1.0
    */
   record Ok<T>(int status, T body, Map<String, List<String>> headers) implements EndpointResult<T>
   {
      private static final int FIRST_SUCCESS_STATUS = 200;
      private static final int FIRST_CLIENT_ERROR_STATUS = 400;

      /**
       * Constructor.
       *
       * @param status  the HTTP status, {@code 2xx} or {@code 3xx}
       * @param body    the response body
       * @param headers the response headers, by name
       */
      public Ok
      {
         if (status < FIRST_SUCCESS_STATUS || status >= FIRST_CLIENT_ERROR_STATUS)
         {
            throw new IllegalArgumentException("A successful status must be 2xx or 3xx, but was " + status);
         }
         headers = copyHeaders(headers);
      }

      /**
       * Returns a copy of this result with the supplied header value added.
       *
       * @param name  the header name
       * @param value the header value
       * @return the copy
       */
      @Override
      public Ok<T> withHeader(final String name, final String value)
      {
         return new Ok<>(status, body, addHeader(headers, name, value));
      }
   }

   /**
    * An error the endpoint has described as a problem detail, written as it is.
    *
    * @author James Amoore
    * @param <T>     the type of the response body on success
    * @param problem the problem
    * @param headers the response headers, by name
    * @since 1.1.0
    */
   record Problem<T>(ProblemDetail problem, Map<String, List<String>> headers) implements EndpointResult<T>
   {
      /**
       * Constructor.
       *
       * @param problem the problem
       * @param headers the response headers, by name
       */
      public Problem
      {
         Objects.requireNonNull(problem, "problem cannot be null");
         headers = copyHeaders(headers);
      }

      /**
       * Returns a copy of this result with the supplied header value added.
       *
       * @param name  the header name
       * @param value the header value
       * @return the copy
       */
      @Override
      public Problem<T> withHeader(final String name, final String value)
      {
         return new Problem<>(problem, addHeader(headers, name, value));
      }
   }

   /**
    * An error identified by an error code, mapped to a problem detail by the configured
    * {@link FailureProblemMapper} when the response is written.
    *
    * @author James Amoore
    * @param <T>       the type of the response body on success
    * @param errorCode the error code identifying the failure
    * @param errors    the individual field-level errors, empty unless the failure was a validation failure
    * @param headers   the response headers, by name
    * @since 1.1.0
    */
   record Failed<T>(ErrorCode errorCode, List<FieldError> errors, Map<String, List<String>> headers)
      implements EndpointResult<T>
   {
      /**
       * Constructor.
       *
       * @param errorCode the error code identifying the failure
       * @param errors    the individual field-level errors
       * @param headers   the response headers, by name
       */
      public Failed
      {
         Objects.requireNonNull(errorCode, "errorCode cannot be null");
         errors = List.copyOf(Objects.requireNonNull(errors, "errors cannot be null"));
         headers = copyHeaders(headers);
      }

      /**
       * Returns a copy of this result with the supplied header value added.
       *
       * @param name  the header name
       * @param value the header value
       * @return the copy
       */
      @Override
      public Failed<T> withHeader(final String name, final String value)
      {
         return new Failed<>(errorCode, errors, addHeader(headers, name, value));
      }
   }

   /**
    * A {@code 200 OK} result carrying the supplied body.
    *
    * @param body the response body
    * @param <T>  the type of the response body
    * @return the result
    */
   static <T> EndpointResult<T> ok(final T body)
   {
      return status(SuccessStatus.OK, body);
   }

   /**
    * A {@code 201 Created} result carrying the supplied body and a {@code Location} header.
    *
    * @param body     the response body
    * @param location the location of the created resource
    * @param <T>      the type of the response body
    * @return the result
    */
   static <T> EndpointResult<T> created(final T body, final URI location)
   {
      Objects.requireNonNull(location, "location cannot be null");
      return status(SuccessStatus.CREATED, body).withHeader("Location", location.toString());
   }

   /**
    * A {@code 202 Accepted} result carrying the supplied body.
    *
    * @param body the response body
    * @param <T>  the type of the response body
    * @return the result
    */
   static <T> EndpointResult<T> accepted(final T body)
   {
      return status(SuccessStatus.ACCEPTED, body);
   }

   /**
    * A {@code 204 No Content} result.
    *
    * @param <T> the type of the response body on success
    * @return the result
    */
   static <T> EndpointResult<T> noContent()
   {
      return status(SuccessStatus.NO_CONTENT, null);
   }

   /**
    * A successful result with the supplied status and body.
    *
    * @param status the HTTP status, {@code 2xx} or {@code 3xx}
    * @param body   the response body
    * @param <T>    the type of the response body
    * @return the result
    */
   static <T> EndpointResult<T> status(final int status, final T body)
   {
      return new Ok<>(status, body, Map.of());
   }

   /**
    * An error result written as the supplied problem.
    *
    * @param problem the problem
    * @param <T>     the type of the response body on success
    * @return the result
    */
   static <T> EndpointResult<T> problem(final ProblemDetail problem)
   {
      return new Problem<>(problem, Map.of());
   }

   /**
    * An error result identified by the supplied error code.
    *
    * @param errorCode the error code
    * @param <T>       the type of the response body on success
    * @return the result
    */
   static <T> EndpointResult<T> failed(final ErrorCode errorCode)
   {
      return failed(errorCode, List.of());
   }

   /**
    * An error result identified by the supplied error code and carrying field-level errors.
    *
    * @param errorCode the error code
    * @param errors    the field-level errors
    * @param <T>       the type of the response body on success
    * @return the result
    */
   static <T> EndpointResult<T> failed(final ErrorCode errorCode, final List<FieldError> errors)
   {
      return new Failed<>(errorCode, errors, Map.of());
   }

   /**
    * Converts a use case result: a success becomes a {@code 200 OK} carrying the mapped output, a failure
    * becomes a {@link Failed} result.
    *
    * @param result    the use case result
    * @param onSuccess maps the use case output to the response body
    * @param <O>       the type of the use case output
    * @param <T>       the type of the response body
    * @return the endpoint result
    */
   static <O, T> EndpointResult<T> from(
      final UseCaseResult<O> result,
      final Function<? super O, ? extends T> onSuccess)
   {
      requireOnSuccess(onSuccess);
      return respond(result, output -> ok(onSuccess.apply(output)));
   }

   /**
    * Converts a use case result whose success needs more than a {@code 200 OK}, such as a {@code 201 Created}:
    * a success becomes whatever the supplied function returns, a failure becomes a {@link Failed} result.
    *
    * @param result    the use case result
    * @param onSuccess builds the endpoint result from the use case output
    * @param <O>       the type of the use case output
    * @param <T>       the type of the response body
    * @return the endpoint result
    */
   static <O, T> EndpointResult<T> respond(
      final UseCaseResult<O> result,
      final Function<? super O, ? extends EndpointResult<T>> onSuccess)
   {
      Objects.requireNonNull(result, "result cannot be null");
      requireOnSuccess(onSuccess);
      return result.fold(
         output -> Objects.requireNonNull(onSuccess.apply(output), "onSuccess cannot return null"),
         failure -> failed(
            failure.errorCode(),
            failure.validationErrors().stream().map(e -> new FieldError(e.field(), e.message())).toList()));
   }

   private static void requireOnSuccess(final Object onSuccess)
   {
      Objects.requireNonNull(onSuccess, "onSuccess cannot be null");
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
