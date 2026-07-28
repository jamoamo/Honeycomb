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

import io.github.jamoamo.honeycomb.exception.HoneycombException;
import io.github.jamoamo.honeycomb.types.ErrorCategory;
import io.github.jamoamo.honeycomb.types.ErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Tests for {@link UseCaseException}.
 *
 * @author James Amoore
 */
@DisplayName("UseCaseException")
public class UseCaseExceptionTest
{
   private static final ErrorCode AN_ERROR =
      new ErrorCode("USER_NOT_FOUND", ErrorCategory.NOT_FOUND, "No user with that id.");

   @Test
   @DisplayName("is a Honeycomb exception")
   public void testIsHoneycombException()
   {
      assertThat(HoneycombException.class).isAssignableFrom(UseCaseException.class);
   }

   @Test
   @DisplayName("retains the error code")
   public void testRetainsErrorCode()
   {
      UseCaseException exception = new UseCaseException(AN_ERROR);

      assertThat(exception.getErrorCode()).isEqualTo(AN_ERROR);
   }

   @Test
   @DisplayName("uses the error description as its message")
   public void testUsesDescriptionAsMessage()
   {
      UseCaseException exception = new UseCaseException(AN_ERROR);

      assertThat(exception).hasMessage("No user with that id.");
   }

   @Test
   @DisplayName("retains the cause")
   public void testRetainsCause()
   {
      IllegalStateException cause = new IllegalStateException("root cause");

      UseCaseException exception = new UseCaseException(AN_ERROR, cause);

      assertThat(exception).hasCause(cause);
      assertThat(exception.getErrorCode()).isEqualTo(AN_ERROR);
   }

   @Test
   @DisplayName("rejects a null error code")
   public void testRejectsNullErrorCode()
   {
      assertThatNullPointerException()
         .isThrownBy(() -> new UseCaseException(null));
   }
}
