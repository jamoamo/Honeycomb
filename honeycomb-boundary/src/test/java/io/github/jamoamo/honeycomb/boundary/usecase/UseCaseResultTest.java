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
package io.github.jamoamo.honeycomb.boundary.usecase;

import io.github.jamoamo.honeycomb.boundary.validation.ValidationError;
import io.github.jamoamo.honeycomb.types.ErrorCategory;
import io.github.jamoamo.honeycomb.types.ErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Tests for {@link UseCaseResult}.
 *
 * @author James Amoore
 */
@DisplayName("UseCaseResult")
public class UseCaseResultTest
{
   private static final ErrorCode AN_ERROR =
      new ErrorCode("USER_NOT_FOUND", ErrorCategory.NOT_FOUND, "No user with that id.");
   private static final ValidationError A_VALIDATION_ERROR = new ValidationError("email", "must not be blank");

   @Nested
   @DisplayName("success()")
   class Success
   {
      @Test
      @DisplayName("is a success carrying the output")
      public void testCarriesOutput()
      {
         UseCaseResult<String> result = UseCaseResult.success("output");

         assertThat(result.isSuccess()).isTrue();
         assertThat(result.isFailure()).isFalse();
         assertThat(result.value()).contains("output");
      }

      @Test
      @DisplayName("carries no error")
      public void testCarriesNoError()
      {
         UseCaseResult<String> result = UseCaseResult.success("output");

         assertThat(result.error()).isEmpty();
         assertThat(result.validationErrors()).isEmpty();
      }

      @Test
      @DisplayName("supports a use case that produces no output")
      public void testNoOutput()
      {
         UseCaseResult<Void> result = UseCaseResult.success();

         assertThat(result.isSuccess()).isTrue();
         assertThat(result.value()).isEmpty();
      }
   }

   @Nested
   @DisplayName("failure()")
   class Failure
   {
      @Test
      @DisplayName("is a failure carrying the error code")
      public void testCarriesErrorCode()
      {
         UseCaseResult<String> result = UseCaseResult.failure(AN_ERROR);

         assertThat(result.isFailure()).isTrue();
         assertThat(result.isSuccess()).isFalse();
         assertThat(result.error()).contains(AN_ERROR);
      }

      @Test
      @DisplayName("carries no output")
      public void testCarriesNoOutput()
      {
         UseCaseResult<String> result = UseCaseResult.failure(AN_ERROR);

         assertThat(result.value()).isEmpty();
      }

      @Test
      @DisplayName("carries no validation errors by default")
      public void testNoValidationErrorsByDefault()
      {
         UseCaseResult<String> result = UseCaseResult.failure(AN_ERROR);

         assertThat(result.validationErrors()).isEmpty();
      }

      @Test
      @DisplayName("carries the supplied validation errors")
      public void testCarriesValidationErrors()
      {
         UseCaseResult<String> result = UseCaseResult.failure(AN_ERROR, List.of(A_VALIDATION_ERROR));

         assertThat(result.validationErrors()).containsExactly(A_VALIDATION_ERROR);
      }

      @Test
      @DisplayName("exposes unmodifiable validation errors")
      public void testUnmodifiableValidationErrors()
      {
         UseCaseResult<String> result = UseCaseResult.failure(AN_ERROR, List.of(A_VALIDATION_ERROR));

         assertThat(result.validationErrors()).isUnmodifiable();
      }

      @Test
      @DisplayName("rejects a null error code")
      public void testRejectsNullErrorCode()
      {
         assertThatNullPointerException()
            .isThrownBy(() -> UseCaseResult.failure(null));
      }
   }

   @Nested
   @DisplayName("map()")
   class Map
   {
      @Test
      @DisplayName("applies the mapper to the output of a success")
      public void testMapsSuccess()
      {
         UseCaseResult<Integer> result = UseCaseResult.success("output").map(String::length);

         assertThat(result.value()).contains("output".length());
      }

      @Test
      @DisplayName("leaves a failure untouched")
      public void testLeavesFailure()
      {
         UseCaseResult<String> failure = UseCaseResult.failure(AN_ERROR, List.of(A_VALIDATION_ERROR));

         UseCaseResult<Integer> result = failure.map(String::length);

         assertThat(result.error()).contains(AN_ERROR);
         assertThat(result.validationErrors()).containsExactly(A_VALIDATION_ERROR);
      }

      @Test
      @DisplayName("rejects a null mapper")
      public void testRejectsNullMapper()
      {
         assertThatNullPointerException()
            .isThrownBy(() -> UseCaseResult.success("output").map(null));
      }
   }

   @Nested
   @DisplayName("flatMap()")
   class FlatMap
   {
      @Test
      @DisplayName("returns the result produced by the mapper for a success")
      public void testMapsSuccess()
      {
         UseCaseResult<String> result =
            UseCaseResult.success("output").flatMap(value -> UseCaseResult.success(value + "!"));

         assertThat(result.value()).contains("output!");
      }

      @Test
      @DisplayName("propagates a failure produced by the mapper")
      public void testPropagatesMapperFailure()
      {
         UseCaseResult<String> result =
            UseCaseResult.success("output").flatMap(value -> UseCaseResult.failure(AN_ERROR));

         assertThat(result.error()).contains(AN_ERROR);
      }

      @Test
      @DisplayName("leaves a failure untouched")
      public void testLeavesFailure()
      {
         UseCaseResult<String> failure = UseCaseResult.failure(AN_ERROR);

         UseCaseResult<String> result = failure.flatMap(value -> UseCaseResult.success("unreachable"));

         assertThat(result.error()).contains(AN_ERROR);
      }

      @Test
      @DisplayName("rejects a mapper that returns null")
      public void testRejectsNullFromMapper()
      {
         assertThatNullPointerException()
            .isThrownBy(() -> UseCaseResult.success("output").flatMap(value -> null));
      }
   }

   @Nested
   @DisplayName("fold()")
   class Fold
   {
      @Test
      @DisplayName("applies the success function to a success")
      public void testFoldsSuccess()
      {
         String folded = UseCaseResult.success("output")
            .fold(value -> "ok:" + value, failure -> "error:" + failure.errorCode().code());

         assertThat(folded).isEqualTo("ok:output");
      }

      @Test
      @DisplayName("applies the failure function to a failure")
      public void testFoldsFailure()
      {
         String folded = UseCaseResult.<String>failure(AN_ERROR)
            .fold(value -> "ok:" + value, failure -> "error:" + failure.errorCode().code());

         assertThat(folded).isEqualTo("error:USER_NOT_FOUND");
      }

      @Test
      @DisplayName("rejects null functions")
      public void testRejectsNullFunctions()
      {
         UseCaseResult<String> result = UseCaseResult.success("output");

         assertThatNullPointerException()
            .isThrownBy(() -> result.fold(null, failure -> "error"));
         assertThatNullPointerException()
            .isThrownBy(() -> result.fold(value -> "ok", null));
      }
   }
}
