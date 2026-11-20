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
import io.github.jamoamo.honeycomb.usecase.validation.ValidationError;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Tests for {@link UseCaseResponses}.
 *
 * @author James Amoore
 */
@DisplayName("UseCaseResponses")
public class UseCaseResponsesTest
{
   private static final String PATH = "/things";

   @ParameterizedTest
   @CsvSource({
      "VALIDATION, 400", "UNAUTHORISED, 401", "FORBIDDEN, 403", "NOT_FOUND, 404", "CONFLICT, 409",
      "PRECONDITION_FAILED, 412", "RATE_LIMITED, 429", "INTERNAL, 500", "UNAVAILABLE, 503", "TIMEOUT, 504"
   })
   @DisplayName("maps each error category to its HTTP status")
   public void testStatusForCategory(final ErrorCategory category, final int expected)
   {
      assertThat(UseCaseResponses.status(category)).isEqualTo(expected);
   }

   @Test
   @DisplayName("maps a success to 200 and a failure to its category's status")
   public void testStatusForResult()
   {
      assertThat(UseCaseResponses.status(UseCaseResult.success("x"))).isEqualTo(200);
      assertThat(UseCaseResponses.status(failure(ErrorCategory.NOT_FOUND))).isEqualTo(404);
   }

   @Test
   @DisplayName("maps a client failure to a problem carrying its description, code and field errors")
   public void testClientFailureProblemDetail()
   {
      UseCaseResult.Failure<?> failure = new UseCaseResult.Failure<>(
         new ErrorCode("INVALID", ErrorCategory.VALIDATION, "Rejected the input."),
         List.of(new ValidationError("name", "is required")));

      ProblemDetail problem = UseCaseResponses.problemDetail(failure, PATH);

      assertThat(problem.status()).isEqualTo(400);
      assertThat(problem.title()).isEqualTo("Bad Request");
      assertThat(problem.detail()).isEqualTo("Rejected the input.");
      assertThat(problem.instance()).isEqualTo(PATH);
      assertThat(problem.code()).isEqualTo("INVALID");
      assertThat(problem.errorId()).isNull();
      assertThat(problem.errors()).containsExactly(new FieldError("name", "is required"));
   }

   @Test
   @DisplayName("hides the description of a server failure behind a generic detail and an error id")
   public void testServerFailureProblemDetailIsSanitised()
   {
      UseCaseResult.Failure<?> failure = new UseCaseResult.Failure<>(
         new ErrorCode("BOOM", ErrorCategory.INTERNAL, "Failed: internal secret"), List.of());

      ProblemDetail problem = UseCaseResponses.problemDetail(failure, PATH);

      assertThat(problem.status()).isEqualTo(500);
      assertThat(problem.detail()).isEqualTo(UseCaseResponses.INTERNAL_SERVER_ERROR_DETAIL);
      assertThat(problem.code()).isEqualTo("BOOM");
      assertThat(problem.errorId()).isNotBlank();
   }

   @Test
   @DisplayName("maps a success to an api response carrying the output")
   public void testToRestResponseSuccess()
   {
      RestResponse response = UseCaseResponses.toRestResponse(UseCaseResult.success("out"), PATH);

      assertThat(response).isEqualTo(ApiResponse.of("out"));
   }

   @Test
   @DisplayName("maps a failure to its problem detail")
   public void testToRestResponseFailure()
   {
      RestResponse response = UseCaseResponses.toRestResponse(failure(ErrorCategory.CONFLICT), PATH);

      assertThat(response).isInstanceOfSatisfying(ProblemDetail.class, p -> assertThat(p.status()).isEqualTo(409));
   }

   @Test
   @DisplayName("composes with fold to build a framework specific response")
   public void testComposesWithFold()
   {
      record Response(int status, Object body) {}

      UseCaseResult<String> result = failure(ErrorCategory.NOT_FOUND);
      Response response = result.fold(
         out -> new Response(200, out),
         f -> new Response(UseCaseResponses.status(f), UseCaseResponses.problemDetail(f, PATH)));

      assertThat(response.status()).isEqualTo(404);
      assertThat(response.body()).isInstanceOf(ProblemDetail.class);
   }

   @Test
   @DisplayName("rejects null arguments")
   public void testRejectsNull()
   {
      assertThatNullPointerException().isThrownBy(() -> UseCaseResponses.status((UseCaseResult<?>) null));
      assertThatNullPointerException().isThrownBy(() -> UseCaseResponses.problemDetail(null, PATH));
      assertThatNullPointerException().isThrownBy(() -> UseCaseResponses.toRestResponse(null, PATH));
   }

   private static <O> UseCaseResult<O> failure(final ErrorCategory category)
   {
      return UseCaseResult.failure(new ErrorCode("CODE", category, "Description."));
   }
}
