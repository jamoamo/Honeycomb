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
import io.github.jamoamo.honeycomb.boundary.validation.ValidationResult;
import io.github.jamoamo.honeycomb.types.ErrorCategory;
import io.github.jamoamo.honeycomb.types.ErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UseCase}.
 *
 * @author James Amoore
 */
@DisplayName("UseCase")
public class UseCaseTest
{
   private static final ValidationError A_VALIDATION_ERROR = new ValidationError("input", "must be lower case");
   private static final ErrorCode A_BUSINESS_ERROR =
      new ErrorCode("USER_NOT_FOUND", ErrorCategory.NOT_FOUND, "No user with that id.");

   /**
    * A use case that echoes its input.
    */
   private static final class EchoUseCase extends UseCase<String, String>
   {
      @Override
      protected UseCaseResult<String> executeUseCase(final String input)
      {
         return UseCaseResult.success("result:" + input);
      }
   }

   /**
    * A use case that records whether it was reached, and rejects input that is not lower case.
    */
   private static final class ValidatingUseCase extends UseCase<String, String>
   {
      private final AtomicBoolean reached = new AtomicBoolean(false);

      @Override
      protected ValidationResult validate(final String input)
      {
         if(input.equals(input.toLowerCase()))
         {
            return ValidationResult.valid();
         }
         return ValidationResult.invalid(A_VALIDATION_ERROR);
      }

      @Override
      protected UseCaseResult<String> executeUseCase(final String input)
      {
         this.reached.set(true);
         return UseCaseResult.success(input);
      }

      private boolean wasReached()
      {
         return this.reached.get();
      }
   }

   /**
    * A use case that always throws the supplied throwable.
    */
   private static final class ThrowingUseCase extends UseCase<String, String>
   {
      private final RuntimeException toThrow;

      private ThrowingUseCase(final RuntimeException toThrow)
      {
         this.toThrow = toThrow;
      }

      @Override
      protected UseCaseResult<String> executeUseCase(final String input)
      {
         throw this.toThrow;
      }
   }

   @Nested
   @DisplayName("execute()")
   class Execute
   {
      @Test
      @DisplayName("returns the output of the use case on success")
      public void testSuccessPassthrough()
      {
         UseCaseResult<String> result = new EchoUseCase().execute("hello");

         assertThat(result.isSuccess()).isTrue();
         assertThat(result.value()).contains("result:hello");
      }

      @Test
      @DisplayName("reaches the use case when the input is valid")
      public void testReachesUseCaseWhenValid()
      {
         ValidatingUseCase useCase = new ValidatingUseCase();

         UseCaseResult<String> result = useCase.execute("valid");

         assertThat(result.isSuccess()).isTrue();
         assertThat(useCase.wasReached()).isTrue();
      }
   }

   @Nested
   @DisplayName("null input")
   class NullInput
   {
      @Test
      @DisplayName("fails with NULL_INPUT rather than throwing")
      public void testNullInputFails()
      {
         UseCaseResult<String> result = new EchoUseCase().execute(null);

         assertThat(result.isFailure()).isTrue();
         assertThat(result.error()).map(ErrorCode::code).contains(UseCase.NULL_INPUT);
      }

      @Test
      @DisplayName("is categorised as a validation failure")
      public void testNullInputCategory()
      {
         UseCaseResult<String> result = new EchoUseCase().execute(null);

         assertThat(result.error()).map(ErrorCode::category).contains(ErrorCategory.VALIDATION);
      }

      @Test
      @DisplayName("never reaches the use case")
      public void testNullInputDoesNotReachUseCase()
      {
         ValidatingUseCase useCase = new ValidatingUseCase();

         useCase.execute(null);

         assertThat(useCase.wasReached()).isFalse();
      }
   }

   @Nested
   @DisplayName("invalid input")
   class InvalidInput
   {
      @Test
      @DisplayName("fails with VALIDATION_FAILED")
      public void testInvalidInputFails()
      {
         UseCaseResult<String> result = new ValidatingUseCase().execute("NOT LOWER CASE");

         assertThat(result.isFailure()).isTrue();
         assertThat(result.error()).map(ErrorCode::code).contains(UseCase.VALIDATION_FAILED);
      }

      @Test
      @DisplayName("carries the individual validation errors")
      public void testCarriesValidationErrors()
      {
         UseCaseResult<String> result = new ValidatingUseCase().execute("NOT LOWER CASE");

         assertThat(result.validationErrors()).containsExactly(A_VALIDATION_ERROR);
      }

      @Test
      @DisplayName("never reaches the use case")
      public void testDoesNotReachUseCase()
      {
         ValidatingUseCase useCase = new ValidatingUseCase();

         useCase.execute("NOT LOWER CASE");

         assertThat(useCase.wasReached()).isFalse();
      }
   }

   @Nested
   @DisplayName("UseCaseException")
   class ThrownUseCaseException
   {
      @Test
      @DisplayName("is converted into a failure carrying its error code")
      public void testConvertedToFailure()
      {
         ThrowingUseCase useCase = new ThrowingUseCase(new UseCaseException(A_BUSINESS_ERROR));

         UseCaseResult<String> result = useCase.execute("input");

         assertThat(result.isFailure()).isTrue();
         assertThat(result.error()).contains(A_BUSINESS_ERROR);
      }
   }

   @Nested
   @DisplayName("unhandled throwable")
   class UnhandledThrowable
   {
      @Test
      @DisplayName("is converted into an UNHANDLED_ERROR failure rather than propagating")
      public void testConvertedToFailure()
      {
         ThrowingUseCase useCase = new ThrowingUseCase(new RuntimeException("kaboom"));

         UseCaseResult<String> result = useCase.execute("input");

         assertThat(result.isFailure()).isTrue();
         assertThat(result.error()).map(ErrorCode::code).contains(UseCase.UNHANDLED_ERROR);
      }

      @Test
      @DisplayName("is categorised as internal")
      public void testCategorisedAsInternal()
      {
         ThrowingUseCase useCase = new ThrowingUseCase(new RuntimeException("kaboom"));

         UseCaseResult<String> result = useCase.execute("input");

         assertThat(result.error()).map(ErrorCode::category).contains(ErrorCategory.INTERNAL);
      }

      @Test
      @DisplayName("describes the failing use case and the original message")
      public void testDescribesFailure()
      {
         ThrowingUseCase useCase = new ThrowingUseCase(new RuntimeException("the exact error"));

         UseCaseResult<String> result = useCase.execute("input");

         assertThat(result.error()).map(ErrorCode::description)
            .get()
            .asString()
            .contains("ThrowingUseCase")
            .contains("the exact error");
      }
   }
}
