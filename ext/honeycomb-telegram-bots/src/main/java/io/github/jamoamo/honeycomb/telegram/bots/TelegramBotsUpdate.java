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
package io.github.jamoamo.honeycomb.telegram.bots;

import io.github.jamoamo.honeycomb.telegram.UpdateType;
import io.github.jamoamo.honeycomb.telegram.update.BotUpdate;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Adapts a {@code telegrambots} {@link Update} to the bot-library-neutral {@link BotUpdate} the Telegram core
 * works against.
 *
 * <p>
 * Only messages, edited messages and callback queries are supported; an update of any other kind is rejected
 * by {@link #type()} being unrepresentable, so a bot adapter registering for such an update simply never
 * matches one.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class TelegramBotsUpdate implements BotUpdate
{
   private static final String COMMAND_PREFIX = "/";
   private static final String WHITESPACE = "\\s+";

   private final Update update;

   /**
    * Constructor.
    *
    * @param update the update delivered by long polling
    */
   public TelegramBotsUpdate(final Update update)
   {
      this.update = update;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public UpdateType type()
   {
      if (update.hasCallbackQuery())
      {
         return UpdateType.CALLBACK_QUERY;
      }
      if (update.hasEditedMessage())
      {
         return UpdateType.EDITED_MESSAGE;
      }

      return UpdateType.MESSAGE;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Optional<String> command()
   {
      String text = text();
      if (text == null || !text.startsWith(COMMAND_PREFIX))
      {
         return Optional.empty();
      }

      String firstToken = text.trim().split(WHITESPACE, 2)[0].substring(1);
      int at = firstToken.indexOf('@');
      return Optional.of(at < 0 ? firstToken : firstToken.substring(0, at));
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<String> commandArguments()
   {
      String text = text();
      if (text == null || !text.startsWith(COMMAND_PREFIX))
      {
         return List.of();
      }

      String[] tokens = text.trim().split(WHITESPACE);
      if (tokens.length <= 1)
      {
         return List.of();
      }

      return List.of(Arrays.copyOfRange(tokens, 1, tokens.length));
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public long chatId()
   {
      if (update.hasCallbackQuery())
      {
         MaybeInaccessibleMessage message = update.getCallbackQuery().getMessage();
         return message instanceof Message accessible ? accessible.getChatId() : 0L;
      }

      return message().getChatId();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public long userId()
   {
      if (update.hasCallbackQuery())
      {
         return update.getCallbackQuery().getFrom().getId();
      }

      return message().getFrom().getId();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String text()
   {
      if (update.hasCallbackQuery())
      {
         return update.getCallbackQuery().getData();
      }

      return message().getText();
   }

   private Message message()
   {
      return update.hasEditedMessage() ? update.getEditedMessage() : update.getMessage();
   }
}
