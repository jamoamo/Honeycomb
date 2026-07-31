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
package io.github.jamoamo.honeycomb.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ErrorCode}.
 *
 * @author James Amoore
 */
@DisplayName("ErrorCode")
public class ErrorCodeTest
{
   @Test
   @DisplayName("retains the code, category and description")
   public void testRetainsComponents()
   {
      ErrorCode errorCode = new ErrorCode("USER_NOT_FOUND", ErrorCategory.NOT_FOUND, "No user with that id.");

      assertThat(errorCode.code()).isEqualTo("USER_NOT_FOUND");
      assertThat(errorCode.category()).isEqualTo(ErrorCategory.NOT_FOUND);
      assertThat(errorCode.description()).isEqualTo("No user with that id.");
   }

   @Test
   @DisplayName("is equal to another error code with the same components")
   public void testValueEquality()
   {
      ErrorCode one = new ErrorCode("CODE", ErrorCategory.CONFLICT, "description");
      ErrorCode two = new ErrorCode("CODE", ErrorCategory.CONFLICT, "description");

      assertThat(one).isEqualTo(two);
      assertThat(one).hasSameHashCodeAs(two);
   }

   @Test
   @DisplayName("rejects a null code")
   public void testRejectsNullCode()
   {
      assertThatNullPointerException()
         .isThrownBy(() -> new ErrorCode(null, ErrorCategory.INTERNAL, "description"));
   }

   @ParameterizedTest
   @ValueSource(strings = {"", " ", "\t"})
   @DisplayName("rejects a blank code")
   public void testRejectsBlankCode(final String code)
   {
      assertThatIllegalArgumentException()
         .isThrownBy(() -> new ErrorCode(code, ErrorCategory.INTERNAL, "description"));
   }

   @Test
   @DisplayName("rejects a null category")
   public void testRejectsNullCategory()
   {
      assertThatNullPointerException()
         .isThrownBy(() -> new ErrorCode("CODE", null, "description"));
   }

   @Test
   @DisplayName("rejects a null description")
   public void testRejectsNullDescription()
   {
      assertThatNullPointerException()
         .isThrownBy(() -> new ErrorCode("CODE", ErrorCategory.INTERNAL, null));
   }
}
