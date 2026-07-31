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
package io.github.jamoamo.honeycomb.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HoneycombException}.
 *
 * @author James Amoore
 */
@DisplayName("HoneycombException")
public class HoneycombExceptionTest
{
   private static final class TestException extends HoneycombException
   {
      private static final long serialVersionUID = 1L;

      private TestException(final String message)
      {
         super(message);
      }

      private TestException(final Throwable cause)
      {
         super(cause);
      }

      private TestException(final String message, final Throwable cause)
      {
         super(message, cause);
      }
   }

   @Test
   @DisplayName("is unchecked")
   public void testIsUnchecked()
   {
      assertThat(RuntimeException.class).isAssignableFrom(HoneycombException.class);
   }

   @Test
   @DisplayName("retains the message")
   public void testRetainsMessage()
   {
      TestException exception = new TestException("something went wrong");

      assertThat(exception).hasMessage("something went wrong");
      assertThat(exception).hasNoCause();
   }

   @Test
   @DisplayName("retains the cause")
   public void testRetainsCause()
   {
      IllegalStateException cause = new IllegalStateException("root cause");

      TestException exception = new TestException(cause);

      assertThat(exception).hasCause(cause);
   }

   @Test
   @DisplayName("retains both the message and the cause")
   public void testRetainsMessageAndCause()
   {
      IllegalStateException cause = new IllegalStateException("root cause");

      TestException exception = new TestException("something went wrong", cause);

      assertThat(exception).hasMessage("something went wrong");
      assertThat(exception).hasCause(cause);
   }
}
