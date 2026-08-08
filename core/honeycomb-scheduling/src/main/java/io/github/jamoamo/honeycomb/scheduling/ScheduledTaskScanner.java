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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Discovers the {@link Scheduled} methods of {@link ScheduledTaskAdapter} instances and resolves each one into
 * a {@link ScheduledTaskRegistration}.
 *
 * <p>
 * Scanning is where a mis-declared adapter is rejected: a scheduled method declaring parameters, a blank cron
 * expression, or two tasks sharing a name all fail here rather than when a scheduler tries to bind them.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class ScheduledTaskScanner
{
   /**
    * Scans the given adapters.
    *
    * @param adapterBeans the scheduled task adapter instances to scan
    * @return the registrations declared across all of the adapters
    */
   public List<ScheduledTaskRegistration> scanAll(final Collection<?> adapterBeans)
   {
      List<ScheduledTaskRegistration> registrations = new ArrayList<>();
      Set<String> names = new HashSet<>();
      for (Object adapterBean : adapterBeans)
      {
         for (ScheduledTaskRegistration registration : scan(adapterBean))
         {
            requireUnique(names, registration);
            registrations.add(registration);
         }
      }

      return List.copyOf(registrations);
   }

   /**
    * Scans a single adapter.
    *
    * @param adapterBean the scheduled task adapter instance to scan
    * @return the registrations declared by the adapter
    */
   public List<ScheduledTaskRegistration> scan(final Object adapterBean)
   {
      requireAdapter(adapterBean);

      List<ScheduledTaskRegistration> registrations = new ArrayList<>();
      Set<String> names = new HashSet<>();
      for (Method method : adapterBean.getClass().getMethods())
      {
         Scheduled scheduled = method.getAnnotation(Scheduled.class);
         if (scheduled != null)
         {
            ScheduledTaskRegistration registration = register(adapterBean, method, scheduled);
            requireUnique(names, registration);
            registrations.add(registration);
         }
      }

      return List.copyOf(registrations);
   }

   private static void requireUnique(final Set<String> names, final ScheduledTaskRegistration registration)
   {
      if (!names.add(registration.name()))
      {
         throw new IllegalStateException("Duplicate scheduled task name registered: " + registration.name());
      }
   }

   private static void requireAdapter(final Object adapterBean)
   {
      if (adapterBean.getClass().getAnnotation(ScheduledTaskAdapter.class) == null)
      {
         throw new IllegalStateException("Not a scheduled task adapter: " + adapterBean.getClass().getName());
      }
   }

   private static ScheduledTaskRegistration register(
      final Object adapterBean, final Method method, final Scheduled scheduled)
   {
      if (scheduled.cron().isBlank())
      {
         throw new IllegalStateException("@Scheduled methods must declare a cron expression: " + method);
      }
      if (method.getParameterCount() > 0)
      {
         throw new IllegalStateException("@Scheduled methods must declare no parameters: " + method);
      }

      String name = scheduled.name().isEmpty() ? method.getName() : scheduled.name();
      return new ScheduledTaskRegistration(name, scheduled.cron(), new HandlerMethod(adapterBean, method));
   }
}
