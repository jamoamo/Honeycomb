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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The commands and update handlers discovered across a set of Telegram bot adapters, indexed for lookup by an
 * incoming update.
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class BotAdapterRegistry
{
   private final Map<String, CommandRegistration> commandsByName;
   private final Map<UpdateType, UpdateRegistration> updatesByType;

   /**
    * Constructor.
    *
    * @param commands the discovered command registrations
    * @param updates  the discovered update registrations
    */
   public BotAdapterRegistry(final List<CommandRegistration> commands, final List<UpdateRegistration> updates)
   {
      this.commandsByName = new HashMap<>();
      for (CommandRegistration command : commands)
      {
         this.commandsByName.put(command.name(), command);
      }

      this.updatesByType = new HashMap<>();
      for (UpdateRegistration update : updates)
      {
         this.updatesByType.put(update.type(), update);
      }
   }

   /**
    * The command registered under the given name.
    *
    * @param name the command text, without the leading {@code /}
    * @return the registration, or empty when no command is registered under that name
    */
   public Optional<CommandRegistration> findCommand(final String name)
   {
      return Optional.ofNullable(commandsByName.get(name));
   }

   /**
    * The update handler registered for the given update type.
    *
    * @param type the update type
    * @return the registration, or empty when no handler is registered for that type
    */
   public Optional<UpdateRegistration> findUpdate(final UpdateType type)
   {
      return Optional.ofNullable(updatesByType.get(type));
   }
}
