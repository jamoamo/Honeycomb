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
package io.github.jamoamo.honeycomb.quartz;

import io.github.jamoamo.honeycomb.scheduling.SchedulingException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link HoneycombQuartzJob}.
 *
 * @author James Amoore
 */
@DisplayName("HoneycombQuartzJob")
public class HoneycombQuartzJobTest
{
   private final HoneycombQuartzJob job = new HoneycombQuartzJob();

   @AfterEach
   public void cleanup()
   {
      HoneycombQuartzJob.unregister(new JobKey("run", "test").toString());
      HoneycombQuartzJob.unregister(new JobKey("fail", "test").toString());
      HoneycombQuartzJob.unregister(new JobKey("missing", "test").toString());
   }

   @Test
   @DisplayName("runs the task registered for the firing job's key")
   public void testRunsRegisteredTask() throws JobExecutionException
   {
      JobKey jobKey = new JobKey("run", "test");
      AtomicInteger runs = new AtomicInteger();
      HoneycombQuartzJob.register(jobKey.toString(), runs::incrementAndGet);

      job.execute(contextFor(jobKey));

      assertThat(runs.get()).isEqualTo(1);
   }

   @Test
   @DisplayName("fails when no task is registered for the firing job's key")
   public void testFailsWhenTaskNotRegistered()
   {
      JobKey jobKey = new JobKey("missing", "test");

      assertThatExceptionOfType(JobExecutionException.class)
         .isThrownBy(() -> job.execute(contextFor(jobKey)))
         .withMessageContaining("No scheduled task registered");
   }

   @Test
   @DisplayName("wraps a scheduling exception thrown by the task")
   public void testWrapsSchedulingException()
   {
      JobKey jobKey = new JobKey("fail", "test");
      HoneycombQuartzJob.register(jobKey.toString(), () ->
      {
         throw new SchedulingException("boom");
      });

      assertThatExceptionOfType(JobExecutionException.class)
         .isThrownBy(() -> job.execute(contextFor(jobKey)))
         .withMessageContaining("failed")
         .withCauseInstanceOf(SchedulingException.class);
   }

   private static JobExecutionContext contextFor(final JobKey jobKey)
   {
      JobExecutionContext context = mock(JobExecutionContext.class);
      JobDetail jobDetail = mock(JobDetail.class);
      when(context.getJobDetail()).thenReturn(jobDetail);
      when(jobDetail.getKey()).thenReturn(jobKey);
      return context;
   }
}
