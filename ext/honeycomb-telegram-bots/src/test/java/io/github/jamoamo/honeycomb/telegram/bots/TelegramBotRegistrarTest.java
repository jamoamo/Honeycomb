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
import io.github.jamoamo.honeycomb.telegram.TelegramBotAdapterMethodInvoker;
import io.github.jamoamo.honeycomb.telegram.TelegramException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TelegramBotRegistrar}.
 *
 * @author James Amoore
 */
@DisplayName("TelegramBotRegistrar")
public class TelegramBotRegistrarTest
{
   private final TelegramBotsLongPollingApplication application = mock(TelegramBotsLongPollingApplication.class);
   private final TelegramClient telegramClient = mock(TelegramClient.class);
   private final BotAdapterRegistry registry = new BotAdapterRegistry(List.of(), List.of());
   private final TelegramBotRegistrar registrar =
      new TelegramBotRegistrar(application, new TelegramBotAdapterMethodInvoker());

   @Test
   @DisplayName("registers a consumer that dispatches to the given registry")
   public void testRegistersConsumer() throws TelegramApiException
   {
      registrar.register("token", registry, telegramClient);

      verify(application).registerBot(eq("token"), any(LongPollingUpdateConsumer.class));
   }

   @Test
   @DisplayName("wraps a registration failure")
   public void testWrapsRegistrationFailure() throws TelegramApiException
   {
      when(application.registerBot(any(), any())).thenThrow(new TelegramApiException("boom"));

      assertThatExceptionOfType(TelegramException.class)
         .isThrownBy(() -> registrar.register("token", registry, telegramClient))
         .withMessageContaining("Could not register bot");
   }

   @Test
   @DisplayName("closes the underlying long polling application")
   public void testClosesApplication() throws Exception
   {
      registrar.close();

      verify(application).close();
   }
}
