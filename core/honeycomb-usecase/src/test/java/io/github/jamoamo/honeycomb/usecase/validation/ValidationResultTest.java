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
package io.github.jamoamo.honeycomb.usecase.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Tests for {@link ValidationResult}.
 *
 * @author James Amoore
 */
@DisplayName("ValidationResult")
public class ValidationResultTest
{
   private static final ValidationError AN_ERROR = new ValidationError("email", "must not be blank");

   @Nested
   @DisplayName("valid()")
   class Valid
   {
      @Test
      @DisplayName("is valid and carries no errors")
      public void testIsValid()
      {
         ValidationResult result = ValidationResult.valid();

         assertThat(result.isValid()).isTrue();
         assertThat(result.errors()).isEmpty();
      }
   }

   @Nested
   @DisplayName("invalid()")
   class Invalid
   {
      @Test
      @DisplayName("is invalid and carries the supplied errors")
      public void testCarriesErrors()
      {
         ValidationResult result = ValidationResult.invalid(AN_ERROR);

         assertThat(result.isValid()).isFalse();
         assertThat(result.errors()).containsExactly(AN_ERROR);
      }

      @Test
      @DisplayName("accepts a list of errors")
      public void testAcceptsList()
      {
         ValidationError second = new ValidationError("name", "must not be blank");

         ValidationResult result = ValidationResult.invalid(List.of(AN_ERROR, second));

         assertThat(result.errors()).containsExactly(AN_ERROR, second);
      }

      @Test
      @DisplayName("rejects an empty list of errors")
      public void testRejectsEmpty()
      {
         assertThatIllegalArgumentException()
            .isThrownBy(() -> ValidationResult.invalid(List.of()));
      }

      @Test
      @DisplayName("rejects a null list of errors")
      public void testRejectsNull()
      {
         assertThatNullPointerException()
            .isThrownBy(() -> ValidationResult.invalid((List<ValidationError>) null));
      }
   }

   @Nested
   @DisplayName("errors()")
   class Errors
   {
      @Test
      @DisplayName("is unmodifiable")
      public void testUnmodifiable()
      {
         ValidationResult result = ValidationResult.invalid(AN_ERROR);

         assertThat(result.errors()).isUnmodifiable();
      }

      @Test
      @DisplayName("is unaffected by later changes to the supplied list")
      public void testDefensivelyCopied()
      {
         List<ValidationError> errors = new ArrayList<>();
         errors.add(AN_ERROR);
         ValidationResult result = ValidationResult.invalid(errors);

         errors.add(new ValidationError("name", "must not be blank"));

         assertThat(result.errors()).containsExactly(AN_ERROR);
      }
   }
}
