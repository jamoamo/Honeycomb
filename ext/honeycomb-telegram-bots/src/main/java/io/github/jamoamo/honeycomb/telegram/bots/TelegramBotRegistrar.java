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

import io.github.jamoamo.honeycomb.telegram.BotAdapterRegistry;
import io.github.jamoamo.honeycomb.telegram.BotUpdateDispatcher;
import io.github.jamoamo.honeycomb.telegram.TelegramBotAdapterMethodInvoker;
import io.github.jamoamo.honeycomb.telegram.TelegramException;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Registers Telegram bot adapters with the Telegram Bot API over long polling.
 *
 * <p>
 * Nothing about the bot's commands is provisioned anywhere else: registering starts long polling for the given
 * token immediately, and every update it delivers is routed to the given registry until this registrar is
 * closed.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class TelegramBotRegistrar implements AutoCloseable
{
   private final TelegramBotsLongPollingApplication application;
   private final TelegramBotAdapterMethodInvoker invoker;

   /**
    * Constructor using a new long polling application.
    *
    * @param invoker the method invoker used to dispatch delivered updates
    */
   public TelegramBotRegistrar(final TelegramBotAdapterMethodInvoker invoker)
   {
      this(new TelegramBotsLongPollingApplication(), invoker);
   }

   /**
    * Constructor.
    *
    * @param application the long polling application bots are registered with
    * @param invoker     the method invoker used to dispatch delivered updates
    */
   public TelegramBotRegistrar(
      final TelegramBotsLongPollingApplication application, final TelegramBotAdapterMethodInvoker invoker)
   {
      this.application = application;
      this.invoker = invoker;
   }

   /**
    * Registers a bot and begins long polling for its updates.
    *
    * @param botToken the bot's API token
    * @param registry the commands and update handlers to dispatch to
    * @throws TelegramException if the bot cannot be registered with Telegram
    */
   public void register(final String botToken, final BotAdapterRegistry registry)
   {
      register(botToken, registry, new OkHttpTelegramClient(botToken));
   }

   /**
    * Registers a bot and begins long polling for its updates, replying through the given client.
    *
    * @param botToken       the bot's API token
    * @param registry       the commands and update handlers to dispatch to
    * @param telegramClient the client used to send replies
    * @throws TelegramException if the bot cannot be registered with Telegram
    */
   public void register(
      final String botToken, final BotAdapterRegistry registry, final TelegramClient telegramClient)
   {
      BotUpdateDispatcher dispatcher = new BotUpdateDispatcher(registry, invoker);
      HoneycombLongPollingConsumer consumer = new HoneycombLongPollingConsumer(dispatcher, telegramClient);

      try
      {
         application.registerBot(botToken, consumer);
      }
      catch (final TelegramApiException ex)
      {
         throw new TelegramException("Could not register bot with Telegram.", ex);
      }
   }

   /**
    * Stops long polling for every registered bot.
    *
    * @throws Exception if the underlying long polling application fails to close
    */
   @Override
   public void close() throws Exception
   {
      application.close();
   }
}
