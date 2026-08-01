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
import io.github.jamoamo.honeycomb.telegram.BotUpdateScanner;
import io.github.jamoamo.honeycomb.telegram.TelegramBotAdapterMethodInvoker;
import io.github.jamoamo.honeycomb.telegram.adapter.Command;
import io.github.jamoamo.honeycomb.telegram.adapter.TelegramBotAdapter;
import io.github.jamoamo.honeycomb.telegram.response.BotResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link HoneycombLongPollingConsumer}.
 *
 * @author James Amoore
 */
@DisplayName("HoneycombLongPollingConsumer")
public class HoneycombLongPollingConsumerTest
{
   @TelegramBotAdapter
   public static final class GreeterAdapter
   {
      @Command("start")
      public BotResponse onStart()
      {
         return BotResponse.reply("welcome");
      }

      @Command("silent")
      public BotResponse onSilent()
      {
         return BotResponse.none();
      }
   }

   private final BotAdapterRegistry registry = new BotUpdateScanner().scan(new GreeterAdapter());
   private final BotUpdateDispatcher dispatcher =
      new BotUpdateDispatcher(registry, new TelegramBotAdapterMethodInvoker());
   private final TelegramClient telegramClient = mock(TelegramClient.class);
   private final HoneycombLongPollingConsumer consumer =
      new HoneycombLongPollingConsumer(dispatcher, telegramClient);
   private final Update update = mock(Update.class);
   private final Message message = mock(Message.class);

   @Test
   @DisplayName("sends the handler's reply back to the chat")
   public void testSendsReply() throws TelegramApiException
   {
      when(update.getMessage()).thenReturn(message);
      when(message.getText()).thenReturn("/start");
      when(message.getChatId()).thenReturn(42L);

      consumer.consume(List.of(update));

      verify(telegramClient).execute(any(SendMessage.class));
   }

   @Test
   @DisplayName("sends nothing when the handler asks for no reply")
   public void testSendsNothingForNoneResponse() throws TelegramApiException
   {
      when(update.getMessage()).thenReturn(message);
      when(message.getText()).thenReturn("/silent");

      consumer.consume(List.of(update));

      verify(telegramClient, never()).execute(any(SendMessage.class));
   }

   @Test
   @DisplayName("logs rather than throws when sending the reply fails")
   public void testLogsSendFailure() throws TelegramApiException
   {
      when(update.getMessage()).thenReturn(message);
      when(message.getText()).thenReturn("/start");
      when(message.getChatId()).thenReturn(42L);
      when(telegramClient.execute(any(SendMessage.class))).thenThrow(new TelegramApiException("boom"));

      consumer.consume(List.of(update));

      verify(telegramClient).execute(any(SendMessage.class));
   }
}
