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
package io.github.jamoamo.honeycomb.telegram;

import io.github.jamoamo.honeycomb.telegram.adapter.Command;
import io.github.jamoamo.honeycomb.telegram.adapter.OnUpdate;
import io.github.jamoamo.honeycomb.telegram.adapter.TelegramBotAdapter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Discovers the {@link Command} and {@link OnUpdate} methods of {@link TelegramBotAdapter} instances and
 * resolves them into a {@link BotAdapterRegistry}.
 *
 * <p>
 * Scanning is where a mis-declared adapter is rejected: a method annotated with both {@link Command} and
 * {@link OnUpdate}, or two handlers registered for the same command name or update type, fail here rather than
 * on the first update delivered.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class BotUpdateScanner
{
   /**
    * Scans a single adapter.
    *
    * @param adapterBean the Telegram bot adapter instance to scan
    * @return the registry of commands and update handlers declared by the adapter
    */
   public BotAdapterRegistry scan(final Object adapterBean)
   {
      return scanAll(List.of(adapterBean));
   }

   /**
    * Scans the given adapters.
    *
    * @param adapterBeans the Telegram bot adapter instances to scan
    * @return the registry of commands and update handlers declared across all of the adapters
    */
   public BotAdapterRegistry scanAll(final Collection<?> adapterBeans)
   {
      List<CommandRegistration> commands = new ArrayList<>();
      List<UpdateRegistration> updates = new ArrayList<>();
      Set<String> commandNames = new HashSet<>();
      Set<UpdateType> updateTypes = new HashSet<>();

      for (Object adapterBean : adapterBeans)
      {
         scanBean(adapterBean, commands, updates, commandNames, updateTypes);
      }

      return new BotAdapterRegistry(List.copyOf(commands), List.copyOf(updates));
   }

   private void scanBean(
      final Object adapterBean,
      final List<CommandRegistration> commands,
      final List<UpdateRegistration> updates,
      final Set<String> commandNames,
      final Set<UpdateType> updateTypes)
   {
      if (adapterBean.getClass().getAnnotation(TelegramBotAdapter.class) == null)
      {
         throw new IllegalStateException("Not a telegram bot adapter: " + adapterBean.getClass().getName());
      }

      for (Method method : adapterBean.getClass().getMethods())
      {
         registerCommand(adapterBean, method, commands, commandNames);
         registerUpdate(adapterBean, method, updates, updateTypes);
      }
   }

   private void registerCommand(
      final Object adapterBean,
      final Method method,
      final List<CommandRegistration> commands,
      final Set<String> commandNames)
   {
      Command command = method.getAnnotation(Command.class);
      if (command == null)
      {
         return;
      }

      requireNotBothAnnotated(method);
      if (!commandNames.add(command.value()))
      {
         throw new IllegalStateException("Duplicate command registered: " + command.value());
      }

      commands.add(new CommandRegistration(command.value(), new HandlerMethod(adapterBean, method)));
   }

   private void registerUpdate(
      final Object adapterBean,
      final Method method,
      final List<UpdateRegistration> updates,
      final Set<UpdateType> updateTypes)
   {
      OnUpdate onUpdate = method.getAnnotation(OnUpdate.class);
      if (onUpdate == null)
      {
         return;
      }

      requireNotBothAnnotated(method);
      if (!updateTypes.add(onUpdate.type()))
      {
         throw new IllegalStateException("Duplicate update handler registered for type: " + onUpdate.type());
      }

      updates.add(new UpdateRegistration(onUpdate.type(), new HandlerMethod(adapterBean, method)));
   }

   private static void requireNotBothAnnotated(final Method method)
   {
      if (method.isAnnotationPresent(Command.class) && method.isAnnotationPresent(OnUpdate.class))
      {
         throw new IllegalStateException(
            "A handler method may be annotated with either @Command or @OnUpdate, not both: " + method);
      }
   }
}
