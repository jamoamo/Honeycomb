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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The outcome of validating the input to a use case.
 *
 * <p>
 * A result with no errors is valid. The list of errors is immutable and is never null.
 * </p>
 *
 * @author James Amoore
 * @param errors the problems found with the input, empty if the input is valid
 * @since 1.0.0
 */
public record ValidationResult(List<ValidationError> errors)
{
   private static final String NULL_ERRORS = "errors cannot be null";
   private static final ValidationResult VALID = new ValidationResult(List.of());

   /**
    * Constructor.
    *
    * @param errors the problems found with the input, empty if the input is valid
    */
   public ValidationResult(final List<ValidationError> errors)
   {
      this.errors = List.copyOf(Objects.requireNonNull(errors, NULL_ERRORS));
   }

   /**
    * A result indicating the input is valid.
    *
    * @return a valid result
    */
   public static ValidationResult valid()
   {
      return VALID;
   }

   /**
    * A result indicating the input is invalid.
    *
    * @param errors the problems found with the input, must not be empty
    * @return an invalid result carrying the supplied errors
    */
   public static ValidationResult invalid(final List<ValidationError> errors)
   {
      ValidationResult result = new ValidationResult(errors);
      if(result.isValid())
      {
         throw new IllegalArgumentException("An invalid result requires at least one error");
      }
      return result;
   }

   /**
    * A result indicating the input is invalid.
    *
    * @param errors the problems found with the input, must not be empty
    * @return an invalid result carrying the supplied errors
    */
   public static ValidationResult invalid(final ValidationError... errors)
   {
      return invalid(Arrays.asList(Objects.requireNonNull(errors, NULL_ERRORS)));
   }

   /**
    * Whether the validated input is valid.
    *
    * @return true if no errors were found
    */
   public boolean isValid()
   {
      return this.errors.isEmpty();
   }
}
