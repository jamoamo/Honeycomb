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
import io.github.jamoamo.honeycomb.types.ErrorCategory;
import io.github.jamoamo.honeycomb.types.ErrorCode;
import io.github.jamoamo.honeycomb.usecase.UseCaseResult;
import io.github.jamoamo.honeycomb.usecase.validation.ValidationError;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link EndpointResult}.
 *
 * @author James Amoore
 */
@DisplayName("EndpointResult")
public class EndpointResultTest
{
   private static final ErrorCode NOT_FOUND = new ErrorCode("THING_NOT_FOUND", ErrorCategory.NOT_FOUND, "No thing.");

   @Test
   @DisplayName("creates successes with the expected statuses")
   public void testSuccessFactories()
   {
      assertThat(EndpointResult.ok("a")).isEqualTo(new EndpointResult.Ok<>(200, "a", Map.of()));
      assertThat(EndpointResult.accepted("a")).isEqualTo(new EndpointResult.Ok<>(202, "a", Map.of()));
      assertThat(EndpointResult.noContent()).isEqualTo(new EndpointResult.Ok<>(204, null, Map.of()));
      assertThat(EndpointResult.status(206, "a")).isEqualTo(new EndpointResult.Ok<>(206, "a", Map.of()));
   }

   @Test
   @DisplayName("creates a 201 carrying a Location header")
   public void testCreated()
   {
      EndpointResult<String> result = EndpointResult.created("a", URI.create("/things/1"));

      assertThat(result).isEqualTo(new EndpointResult.Ok<>(201, "a", Map.of("Location", List.of("/things/1"))));
   }

   @Test
   @DisplayName("rejects a success status outside 2xx and 3xx")
   public void testRejectsNonSuccessStatus()
   {
      assertThatIllegalArgumentException().isThrownBy(() -> EndpointResult.status(404, "a"));
      assertThatIllegalArgumentException().isThrownBy(() -> EndpointResult.status(199, "a"));
   }

   @Test
   @DisplayName("creates errors from a problem or an error code")
   public void testErrorFactories()
   {
      ProblemDetail problem = ProblemDetail.badRequest("Bad.", "/x", "BAD");
      List<FieldError> errors = List.of(new FieldError("name", "is required"));

      assertThat(EndpointResult.problem(problem)).isEqualTo(new EndpointResult.Problem<>(problem, Map.of()));
      assertThat(EndpointResult.failed(NOT_FOUND)).isEqualTo(new EndpointResult.Failed<>(NOT_FOUND, List.of(), Map.of()));
      assertThat(EndpointResult.failed(NOT_FOUND, errors))
         .isEqualTo(new EndpointResult.Failed<>(NOT_FOUND, errors, Map.of()));
   }

   @Test
   @DisplayName("converts a use case success to a 200 carrying the mapped output")
   public void testFromSuccess()
   {
      assertThat(EndpointResult.from(UseCaseResult.success(2), n -> "n=" + n)).isEqualTo(EndpointResult.ok("n=2"));
   }

   @Test
   @DisplayName("converts a use case failure to a failed result carrying its validation errors as field errors")
   public void testFromFailure()
   {
      UseCaseResult<Integer> failure =
         UseCaseResult.failure(NOT_FOUND, List.of(new ValidationError("id", "is unknown")));

      EndpointResult<String> result = EndpointResult.from(failure, n -> "n=" + n);

      assertThat(result).isEqualTo(EndpointResult.failed(NOT_FOUND, List.of(new FieldError("id", "is unknown"))));
   }

   @Test
   @DisplayName("lets a success decide its own result")
   public void testRespond()
   {
      EndpointResult<String> result = EndpointResult.respond(
         UseCaseResult.success(7), n -> EndpointResult.created("n=" + n, URI.create("/n/" + n)));

      assertThat(result).isEqualTo(EndpointResult.created("n=7", URI.create("/n/7")));
   }

   @Test
   @DisplayName("does not call the success function for a failure")
   public void testRespondSkipsSuccessFunctionOnFailure()
   {
      EndpointResult<String> result = EndpointResult.respond(UseCaseResult.<Integer>failure(NOT_FOUND), n ->
      {
         throw new AssertionError("should not be called");
      });

      assertThat(result).isInstanceOf(EndpointResult.Failed.class);
   }

   @Test
   @DisplayName("adds header values to every variant, keeping earlier values")
   public void testWithHeader()
   {
      assertThat(EndpointResult.ok("a").withHeader("X", "1").withHeader("X", "2").headers())
         .containsExactly(Map.entry("X", List.of("1", "2")));
      assertThat(EndpointResult.problem(ProblemDetail.badRequest("Bad.", "/x", null)).withHeader("X", "1").headers())
         .containsEntry("X", List.of("1"));
      assertThat(EndpointResult.failed(NOT_FOUND).withHeader("X", "1").headers()).containsEntry("X", List.of("1"));
   }

   @Test
   @DisplayName("copies headers defensively and exposes them read-only")
   public void testHeadersAreImmutable()
   {
      Map<String, List<String>> headers = new HashMap<>(Map.of("X", List.of("1")));
      EndpointResult.Ok<String> result = new EndpointResult.Ok<>(200, "a", headers);
      headers.put("Y", List.of("2"));

      assertThat(result.headers()).containsOnlyKeys("X");
      assertThatThrownBy(() -> result.headers().put("Z", List.of())).isInstanceOf(UnsupportedOperationException.class);
   }

   @Test
   @DisplayName("rejects null arguments")
   public void testRejectsNull()
   {
      assertThatNullPointerException().isThrownBy(() -> EndpointResult.from(null, n -> n));
      assertThatNullPointerException().isThrownBy(() -> EndpointResult.respond(UseCaseResult.success(1), n -> null));
      assertThatNullPointerException().isThrownBy(() -> EndpointResult.created("a", null));
      assertThatNullPointerException().isThrownBy(() -> EndpointResult.problem(null));
      assertThatNullPointerException().isThrownBy(() -> EndpointResult.failed(null));
      assertThatNullPointerException().isThrownBy(() -> EndpointResult.ok("a").withHeader(null, "1"));
   }
}
