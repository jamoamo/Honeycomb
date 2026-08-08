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

import io.github.jamoamo.honeycomb.scheduling.HandlerMethod;
import io.github.jamoamo.honeycomb.scheduling.ScheduledTaskMethodInvoker;
import io.github.jamoamo.honeycomb.scheduling.ScheduledTaskRegistration;
import io.github.jamoamo.honeycomb.scheduling.SchedulingException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link QuartzScheduleBinder}.
 *
 * @author James Amoore
 */
@DisplayName("QuartzScheduleBinder")
public class QuartzScheduleBinderTest
{
   public static final class TaskAdapter
   {
      private final AtomicInteger runs = new AtomicInteger();

      public void run()
      {
         runs.incrementAndGet();
      }

      public int runs()
      {
         return runs.get();
      }
   }

   private final Scheduler scheduler = mock(Scheduler.class);
   private final ScheduledTaskMethodInvoker invoker = new ScheduledTaskMethodInvoker();
   private final QuartzScheduleBinder binder = new QuartzScheduleBinder(scheduler, invoker);

   @Test
   @DisplayName("schedules a cron-triggered job for each registration")
   public void testBindsRegistrations() throws SchedulerException, NoSuchMethodException
   {
      ScheduledTaskRegistration registration = registrationFor(new TaskAdapter(), "0 0 * * * ?");

      binder.bind(List.of(registration));

      var jobDetailCaptor = forClass(JobDetail.class);
      var triggerCaptor = forClass(Trigger.class);
      verify(scheduler).scheduleJob(jobDetailCaptor.capture(), triggerCaptor.capture());

      assertThat(jobDetailCaptor.getValue().getJobClass()).isEqualTo(HoneycombQuartzJob.class);
      assertThat(jobDetailCaptor.getValue().getKey())
         .isEqualTo(new JobKey(registration.name(), QuartzScheduleBinder.GROUP));
      assertThat(triggerCaptor.getValue()).isInstanceOf(CronTrigger.class);
      assertThat(((CronTrigger) triggerCaptor.getValue()).getCronExpression()).isEqualTo(registration.cron());
   }

   @Test
   @DisplayName("registers a runnable that invokes the handler method when the bound job fires")
   public void testBoundJobInvokesHandler() throws JobExecutionException, NoSuchMethodException
   {
      TaskAdapter adapter = new TaskAdapter();
      ScheduledTaskRegistration registration = registrationFor(adapter, "0 0 * * * ?");
      binder.bind(List.of(registration));
      JobKey jobKey = new JobKey(registration.name(), QuartzScheduleBinder.GROUP);

      new HoneycombQuartzJob().execute(contextFor(jobKey));

      assertThat(adapter.runs()).isEqualTo(1);
      binder.close();
   }

   @Test
   @DisplayName("wraps a scheduler exception and does not leave a task registered")
   public void testWrapsSchedulerException() throws SchedulerException, NoSuchMethodException
   {
      ScheduledTaskRegistration registration = registrationFor(new TaskAdapter(), "0 0 * * * ?");
      doThrow(new SchedulerException("broker down")).when(scheduler).scheduleJob(any(), any());

      assertThatExceptionOfType(SchedulingException.class)
         .isThrownBy(() -> binder.bind(List.of(registration)))
         .withMessageContaining("Could not schedule task");

      JobKey jobKey = new JobKey(registration.name(), QuartzScheduleBinder.GROUP);
      assertThatExceptionOfType(JobExecutionException.class)
         .isThrownBy(() -> new HoneycombQuartzJob().execute(contextFor(jobKey)))
         .withMessageContaining("No scheduled task registered");
   }

   @Test
   @DisplayName("unschedules every bound job on close")
   public void testClosesBoundJobs() throws SchedulerException, NoSuchMethodException
   {
      ScheduledTaskRegistration registration = registrationFor(new TaskAdapter(), "0 0 * * * ?");
      binder.bind(List.of(registration));

      binder.close();

      verify(scheduler).deleteJob(new JobKey(registration.name(), QuartzScheduleBinder.GROUP));
   }

   @Test
   @DisplayName("does not unschedule anything when nothing was bound")
   public void testDoesNotCloseUnboundJobs() throws SchedulerException, NoSuchMethodException
   {
      ScheduledTaskRegistration registration = registrationFor(new TaskAdapter(), "0 0 * * * ?");
      doThrow(new SchedulerException("broker down")).when(scheduler).scheduleJob(any(), any());
      assertThatExceptionOfType(SchedulingException.class).isThrownBy(() -> binder.bind(List.of(registration)));

      binder.close();

      verify(scheduler, never()).deleteJob(any());
   }

   @Test
   @DisplayName("tolerates a scheduler exception while unscheduling a job")
   public void testToleratesCloseFailure() throws SchedulerException, NoSuchMethodException
   {
      ScheduledTaskRegistration registration = registrationFor(new TaskAdapter(), "0 0 * * * ?");
      binder.bind(List.of(registration));
      when(scheduler.deleteJob(any())).thenThrow(new SchedulerException("broker down"));

      binder.close();

      verify(scheduler).deleteJob(any());
   }

   private static ScheduledTaskRegistration registrationFor(final TaskAdapter adapter, final String cron)
      throws NoSuchMethodException
   {
      Method method = TaskAdapter.class.getMethod("run");
      return new ScheduledTaskRegistration("run", cron, new HandlerMethod(adapter, method));
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
