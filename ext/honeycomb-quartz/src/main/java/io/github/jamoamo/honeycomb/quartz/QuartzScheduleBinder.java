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

import io.github.jamoamo.honeycomb.scheduling.ScheduledTaskMethodInvoker;
import io.github.jamoamo.honeycomb.scheduling.ScheduledTaskRegistration;
import io.github.jamoamo.honeycomb.scheduling.SchedulingException;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Binds {@link ScheduledTaskRegistration}s to Quartz jobs and triggers on a {@link Scheduler} supplied by the
 * caller.
 *
 * <p>
 * The scheduler is built and owned entirely by the caller: whether it runs a {@code RAMJobStore} for a single
 * instance or a clustered {@code JDBCJobStore} shared by several, and whether it is started before or after
 * binding, is configuration this binder has no opinion on. It only registers jobs against whatever scheduler it
 * is given.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class QuartzScheduleBinder implements AutoCloseable
{
   /**
    * The Quartz group every job and trigger bound by this class belongs to.
    */
   public static final String GROUP = "honeycomb-scheduling";

   private static final Logger LOGGER = LoggerFactory.getLogger(QuartzScheduleBinder.class);

   private final Scheduler scheduler;
   private final ScheduledTaskMethodInvoker invoker;
   private final List<JobKey> boundJobs = new ArrayList<>();

   /**
    * Constructor.
    *
    * @param scheduler the scheduler to bind jobs and triggers to
    * @param invoker   the invoker used to run a task's handler method when its job fires
    */
   public QuartzScheduleBinder(final Scheduler scheduler, final ScheduledTaskMethodInvoker invoker)
   {
      this.scheduler = scheduler;
      this.invoker = invoker;
   }

   /**
    * Binds the given scheduled tasks to cron-triggered Quartz jobs.
    *
    * @param registrations the scheduled tasks to bind
    * @throws SchedulingException if a job or trigger could not be scheduled
    */
   public void bind(final Collection<ScheduledTaskRegistration> registrations)
   {
      for (ScheduledTaskRegistration registration : registrations)
      {
         bindOne(registration);
      }
   }

   /**
    * Unschedules every job bound by this binder and removes its registered task.
    */
   @Override
   public void close()
   {
      for (JobKey jobKey : boundJobs)
      {
         unbind(jobKey);
      }

      boundJobs.clear();
   }

   private void bindOne(final ScheduledTaskRegistration registration)
   {
      JobKey jobKey = new JobKey(registration.name(), GROUP);
      HoneycombQuartzJob.register(jobKey.toString(), () -> invoker.invokeScheduledTaskMethod(registration));

      JobDetail jobDetail = newJob(HoneycombQuartzJob.class).withIdentity(jobKey).build();
      CronTrigger trigger = newTrigger()
         .withIdentity(registration.name() + "-trigger", GROUP)
         .withSchedule(CronScheduleBuilder.cronSchedule(registration.cron()))
         .build();

      try
      {
         LOGGER.info("Scheduling task '{}' with cron '{}'.", registration.name(), registration.cron());
         scheduler.scheduleJob(jobDetail, trigger);
         boundJobs.add(jobKey);
      }
      catch (final SchedulerException ex)
      {
         HoneycombQuartzJob.unregister(jobKey.toString());
         throw new SchedulingException("Could not schedule task '" + registration.name() + "'", ex);
      }
   }

   private void unbind(final JobKey jobKey)
   {
      HoneycombQuartzJob.unregister(jobKey.toString());
      try
      {
         scheduler.deleteJob(jobKey);
      }
      catch (final SchedulerException ex)
      {
         LOGGER.warn("Could not unschedule job '{}'.", jobKey, ex);
      }
   }
}
