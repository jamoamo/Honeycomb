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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ScheduledTaskScanner}.
 *
 * @author James Amoore
 */
@DisplayName("ScheduledTaskScanner")
public class ScheduledTaskScannerTest
{
   @ScheduledTaskAdapter
   public static final class ReportAdapter
   {
      @Scheduled(cron = "0 0 * * * ?")
      public void hourlyReport()
      {
         // handled
      }

      @Scheduled(cron = "0 0 0 * * ?", name = "midnight-cleanup")
      public void cleanup()
      {
         // handled
      }

      public void notScheduled()
      {
         // ignored
      }
   }

   @ScheduledTaskAdapter
   public static final class BlankCronAdapter
   {
      @Scheduled(cron = "   ")
      public void task()
      {
         // handled
      }
   }

   @ScheduledTaskAdapter
   public static final class ParameterisedAdapter
   {
      @Scheduled(cron = "0 0 * * * ?")
      public void task(final String argument)
      {
         // handled
      }
   }

   @ScheduledTaskAdapter
   public static final class DuplicateNameAdapter
   {
      @Scheduled(cron = "0 0 * * * ?", name = "hourlyReport")
      public void task()
      {
         // handled
      }
   }

   @ScheduledTaskAdapter
   public static final class SelfDuplicateNameAdapter
   {
      @Scheduled(cron = "0 0 * * * ?", name = "same")
      public void first()
      {
         // handled
      }

      @Scheduled(cron = "0 30 * * * ?", name = "same")
      public void second()
      {
         // handled
      }
   }

   public static final class NotAnAdapter
   {
   }

   private final ScheduledTaskScanner scanner = new ScheduledTaskScanner();

   @Test
   @DisplayName("resolves each scheduled method to its name and cron expression")
   public void testScansScheduledTasks()
   {
      List<ScheduledTaskRegistration> registrations = scanner.scan(new ReportAdapter());

      assertThat(registrations).hasSize(2);
      assertThat(registrations)
         .extracting(ScheduledTaskRegistration::name)
         .containsExactlyInAnyOrder("hourlyReport", "midnight-cleanup");
      assertThat(registrations)
         .filteredOn(registration -> "midnight-cleanup".equals(registration.name()))
         .singleElement()
         .satisfies(registration ->
         {
            assertThat(registration.cron()).isEqualTo("0 0 0 * * ?");
            assertThat(registration.handler().method().getName()).isEqualTo("cleanup");
         });
   }

   @Test
   @DisplayName("defaults the task name to the method name")
   public void testDefaultsNameToMethodName()
   {
      List<ScheduledTaskRegistration> registrations = scanner.scan(new ReportAdapter());

      assertThat(registrations)
         .filteredOn(registration -> "hourlyReport".equals(registration.name()))
         .singleElement()
         .satisfies(registration -> assertThat(registration.handler().method().getName()).isEqualTo("hourlyReport"));
   }

   @Test
   @DisplayName("scans several adapters at once")
   public void testScansAllAdapters()
   {
      List<ScheduledTaskRegistration> registrations =
         scanner.scanAll(List.of(new ReportAdapter(), new AnotherAdapter()));

      assertThat(registrations).hasSize(3);
   }

   @Test
   @DisplayName("rejects a class that is not a scheduled task adapter")
   public void testRejectsNonAdapter()
   {
      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> scanner.scan(new NotAnAdapter()))
         .withMessageContaining("Not a scheduled task adapter");
   }

   @Test
   @DisplayName("rejects a scheduled method declaring a blank cron expression")
   public void testRejectsBlankCron()
   {
      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> scanner.scan(new BlankCronAdapter()))
         .withMessageContaining("must declare a cron expression");
   }

   @Test
   @DisplayName("rejects a scheduled method declaring parameters")
   public void testRejectsParameterisedMethod()
   {
      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> scanner.scan(new ParameterisedAdapter()))
         .withMessageContaining("must declare no parameters");
   }

   @Test
   @DisplayName("rejects two scheduled tasks sharing a name")
   public void testRejectsDuplicateNameAcrossAdapters()
   {
      List<Object> adapters = List.of(new ReportAdapter(), new DuplicateNameAdapter());

      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> scanner.scanAll(adapters))
         .withMessageContaining("Duplicate scheduled task name");
   }

   @Test
   @DisplayName("rejects two scheduled methods on the same adapter sharing a name")
   public void testRejectsDuplicateNameWithinAdapter()
   {
      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> scanner.scan(new SelfDuplicateNameAdapter()))
         .withMessageContaining("Duplicate scheduled task name");
   }

   @ScheduledTaskAdapter
   public static final class AnotherAdapter
   {
      @Scheduled(cron = "0 30 * * * ?")
      public void anotherTask()
      {
         // handled
      }
   }
}
