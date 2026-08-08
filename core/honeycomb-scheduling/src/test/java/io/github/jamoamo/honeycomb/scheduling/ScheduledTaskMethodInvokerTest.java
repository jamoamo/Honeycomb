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

import io.github.jamoamo.honeycomb.scheduling.adapter.Scheduled;
import io.github.jamoamo.honeycomb.scheduling.adapter.ScheduledTaskAdapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ScheduledTaskMethodInvoker}.
 *
 * @author James Amoore
 */
@DisplayName("ScheduledTaskMethodInvoker")
public class ScheduledTaskMethodInvokerTest
{
   @ScheduledTaskAdapter
   public static final class TaskAdapter
   {
      private int invocations;

      @Scheduled(cron = "0 0 * * * ?")
      public void run()
      {
         invocations++;
      }

      @Scheduled(cron = "0 0 * * * ?")
      public void fail()
      {
         throw new IllegalStateException("boom");
      }

      private void inaccessible()
      {
         // never invoked directly; used to force IllegalAccessException
      }

      public int invocations()
      {
         return invocations;
      }
   }

   private final ScheduledTaskScanner scanner = new ScheduledTaskScanner();
   private final ScheduledTaskMethodInvoker invoker = new ScheduledTaskMethodInvoker();

   @Test
   @DisplayName("invokes the handler method")
   public void testInvokesHandlerMethod()
   {
      TaskAdapter adapter = new TaskAdapter();
      ScheduledTaskRegistration registration = scanner.scan(adapter).stream()
         .filter(r -> "run".equals(r.name()))
         .findFirst()
         .orElseThrow();

      invoker.invokeScheduledTaskMethod(registration);

      assertThat(adapter.invocations()).isEqualTo(1);
   }

   @Test
   @DisplayName("wraps an exception thrown by the handler method")
   public void testWrapsHandlerException()
   {
      TaskAdapter adapter = new TaskAdapter();
      ScheduledTaskRegistration registration = scanner.scan(adapter).stream()
         .filter(r -> "fail".equals(r.name()))
         .findFirst()
         .orElseThrow();

      assertThatExceptionOfType(SchedulingException.class)
         .isThrownBy(() -> invoker.invokeScheduledTaskMethod(registration))
         .withMessageContaining("Error invoking scheduled task 'fail'")
         .withCauseInstanceOf(IllegalStateException.class);
   }

   @Test
   @DisplayName("wraps an inaccessible handler method")
   public void testWrapsInaccessibleMethod() throws NoSuchMethodException
   {
      TaskAdapter adapter = new TaskAdapter();
      HandlerMethod handler = new HandlerMethod(adapter, TaskAdapter.class.getDeclaredMethod("inaccessible"));
      ScheduledTaskRegistration registration = new ScheduledTaskRegistration("inaccessible", "0 0 * * * ?", handler);

      assertThatExceptionOfType(SchedulingException.class)
         .isThrownBy(() -> invoker.invokeScheduledTaskMethod(registration))
         .withMessageContaining("Method couldn't be executed for task 'inaccessible'");
   }
}
