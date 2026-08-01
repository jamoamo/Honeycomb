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

import io.github.jamoamo.honeycomb.telegram.update.BotUpdate;

import java.util.List;
import java.util.Optional;

/**
 * A simple in-memory {@link BotUpdate} for tests.
 *
 * @author James Amoore
 */
public final class FakeBotUpdate implements BotUpdate
{
   private final UpdateType type;
   private final String command;
   private final List<String> commandArguments;
   private final long chatId;
   private final long userId;
   private final String text;

   public FakeBotUpdate(final UpdateType type, final long chatId, final long userId, final String text)
   {
      this(type, null, List.of(), chatId, userId, text);
   }

   public FakeBotUpdate(
      final UpdateType type,
      final String command,
      final List<String> commandArguments,
      final long chatId,
      final long userId,
      final String text)
   {
      this.type = type;
      this.command = command;
      this.commandArguments = commandArguments;
      this.chatId = chatId;
      this.userId = userId;
      this.text = text;
   }

   @Override
   public UpdateType type()
   {
      return type;
   }

   @Override
   public Optional<String> command()
   {
      return Optional.ofNullable(command);
   }

   @Override
   public List<String> commandArguments()
   {
      return commandArguments;
   }

   @Override
   public long chatId()
   {
      return chatId;
   }

   @Override
   public long userId()
   {
      return userId;
   }

   @Override
   public String text()
   {
      return text;
   }
}
