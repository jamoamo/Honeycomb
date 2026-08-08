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
package io.github.jamoamo.honeycomb.scheduling;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SchedulingException}.
 *
 * @author James Amoore
 */
@DisplayName("SchedulingException")
public class SchedulingExceptionTest
{
   @Test
   @DisplayName("carries a message")
   public void testMessageOnly()
   {
      SchedulingException exception = new SchedulingException("failed");

      assertThat(exception.getMessage()).isEqualTo("failed");
      assertThat(exception.getCause()).isNull();
   }

   @Test
   @DisplayName("carries a message and a cause")
   public void testMessageAndCause()
   {
      Throwable cause = new IllegalStateException("root cause");

      SchedulingException exception = new SchedulingException("failed", cause);

      assertThat(exception.getMessage()).isEqualTo("failed");
      assertThat(exception.getCause()).isSameAs(cause);
   }
}
