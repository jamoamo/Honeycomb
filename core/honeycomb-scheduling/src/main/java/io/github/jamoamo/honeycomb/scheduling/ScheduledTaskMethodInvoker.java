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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Invoker for the handler method of a scheduled task.
 *
 * <p>
 * A scheduled task takes no input and produces no output the scheduler needs to act on: unlike a message or a
 * request, there is nothing to bind arguments from and nothing in the outcome for the caller to interpret. This
 * invoker exists so that reflective invocation, and the exceptions it can throw, are handled in one place.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class ScheduledTaskMethodInvoker
{
   private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledTaskMethodInvoker.class);
   private static final String TASK_NAME_FORMAT = "'%s'";

   /**
    * Invokes the handler method of the given scheduled task.
    *
    * @param registration the scheduled task to invoke
    * @throws SchedulingException if the handler method throws or cannot be executed
    */
   public void invokeScheduledTaskMethod(final ScheduledTaskRegistration registration)
   {
      Method handlerMethod = registration.handler().method();

      try
      {
         handlerMethod.invoke(registration.handler().bean());
      }
      catch (final InvocationTargetException ex)
      {
         String taskName = TASK_NAME_FORMAT.formatted(registration.name());
         LOGGER.error("Exception invoking scheduled task {}.", taskName, ex);
         throw new SchedulingException("Error invoking scheduled task " + taskName, ex.getCause());
      }
      catch (final IllegalAccessException ex)
      {
         String taskName = TASK_NAME_FORMAT.formatted(registration.name());
         LOGGER.error("Scheduled task {} method error.", taskName, ex);
         throw new SchedulingException("Method couldn't be executed for task " + taskName, ex);
      }
   }
}
