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
package io.github.jamoamo.honeycomb.boundary.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Tests for {@link ValidationError}.
 *
 * @author James Amoore
 */
@DisplayName("ValidationError")
public class ValidationErrorTest
{
   @Test
   @DisplayName("retains the field and message")
   public void testRetainsComponents()
   {
      ValidationError error = new ValidationError("email", "must not be blank");

      assertThat(error.field()).isEqualTo("email");
      assertThat(error.message()).isEqualTo("must not be blank");
   }

   @Test
   @DisplayName("rejects a null field")
   public void testRejectsNullField()
   {
      assertThatNullPointerException()
         .isThrownBy(() -> new ValidationError(null, "must not be blank"));
   }

   @Test
   @DisplayName("rejects a null message")
   public void testRejectsNullMessage()
   {
      assertThatNullPointerException()
         .isThrownBy(() -> new ValidationError("email", null));
   }
}
