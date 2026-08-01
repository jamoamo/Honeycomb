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

import io.github.jamoamo.honeycomb.telegram.BotUpdateDispatcher;
import io.github.jamoamo.honeycomb.telegram.response.BotResponse;
import io.github.jamoamo.honeycomb.telegram.update.BotUpdate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

/**
 * Dispatches every update delivered by long polling to its handler method and sends back the reply the handler
 * decided on, if any.
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class HoneycombLongPollingConsumer implements LongPollingUpdateConsumer
{
   private static final Logger LOGGER = LoggerFactory.getLogger(HoneycombLongPollingConsumer.class);

   private final BotUpdateDispatcher dispatcher;
   private final TelegramClient telegramClient;

   /**
    * Constructor.
    *
    * @param dispatcher     the dispatcher routing updates to their handler methods
    * @param telegramClient the client used to send replies
    */
   public HoneycombLongPollingConsumer(final BotUpdateDispatcher dispatcher, final TelegramClient telegramClient)
   {
      this.dispatcher = dispatcher;
      this.telegramClient = telegramClient;
   }

   /**
    * Handles the updates delivered by a long polling round.
    *
    * @param updates the delivered updates
    */
   @Override
   public void consume(final List<Update> updates)
   {
      for (Update update : updates)
      {
         consumeOne(update);
      }
   }

   private void consumeOne(final Update update)
   {
      BotUpdate botUpdate = new TelegramBotsUpdate(update);
      BotResponse response = dispatcher.dispatch(botUpdate);

      response.replyText().ifPresent(text -> sendReply(botUpdate.chatId(), text));
   }

   private void sendReply(final long chatId, final String text)
   {
      try
      {
         telegramClient.execute(new SendMessage(String.valueOf(chatId), text));
      }
      catch (final TelegramApiException ex)
      {
         LOGGER.error("Failed to send reply to chat {}.", chatId, ex);
      }
   }
}
