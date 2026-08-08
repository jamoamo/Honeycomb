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

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The single Quartz {@link Job} implementation through which every Honeycomb scheduled task is fired.
 *
 * <p>
 * Quartz instantiates jobs reflectively with no way to pass constructor arguments, so the task each firing
 * invokes cannot travel through the job instance itself. Instead {@link QuartzScheduleBinder} registers a task
 * against its job key in a static registry when it binds, and this job looks its task up by key when it fires.
 * A cluster runs identical scheduled task adapters on every node, so every node populates the same keys into
 * its own registry; Quartz's clustering only decides which node's copy of a firing runs, never what runs.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class HoneycombQuartzJob implements Job
{
   private static final Logger LOGGER = LoggerFactory.getLogger(HoneycombQuartzJob.class);
   private static final Map<String, Runnable> TASKS = new ConcurrentHashMap<>();

   /**
    * Registers the task to run whenever the job identified by the given key fires.
    *
    * @param jobKey the key of the Quartz job that fires this task
    * @param task   the task to run
    */
   static void register(final String jobKey, final Runnable task)
   {
      TASKS.put(jobKey, task);
   }

   /**
    * Removes the task registered for the given job key.
    *
    * @param jobKey the key of the Quartz job to stop firing
    */
   static void unregister(final String jobKey)
   {
      TASKS.remove(jobKey);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void execute(final JobExecutionContext context) throws JobExecutionException
   {
      String jobKey = context.getJobDetail().getKey().toString();
      Runnable task = TASKS.get(jobKey);
      if (task == null)
      {
         throw new JobExecutionException("No scheduled task registered for job '" + jobKey + "'", false);
      }

      try
      {
         task.run();
      }
      catch (final SchedulingException ex)
      {
         LOGGER.error("Scheduled task for job '{}' failed.", jobKey, ex);
         throw new JobExecutionException("Scheduled task for job '" + jobKey + "' failed", ex, false);
      }
   }
}
