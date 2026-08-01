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
import io.github.jamoamo.honeycomb.telegram.response.BotResponse;
import io.github.jamoamo.honeycomb.telegram.update.BotUpdate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BotUpdateDispatcher}.
 *
 * @author James Amoore
 */
@DisplayName("BotUpdateDispatcher")
public class BotUpdateDispatcherTest
{
   @TelegramBotAdapter
   public static final class SupportAdapter
   {
      @Command("start")
      public BotResponse onStart()
      {
         return BotResponse.reply("welcome");
      }

      @OnUpdate(type = UpdateType.CALLBACK_QUERY)
      public BotResponse onCallback()
      {
         return BotResponse.reply("acknowledged");
      }
   }

   private final BotAdapterRegistry registry = new BotUpdateScanner().scan(new SupportAdapter());
   private final BotUpdateDispatcher dispatcher =
      new BotUpdateDispatcher(registry, new TelegramBotAdapterMethodInvoker());

   @Test
   @DisplayName("routes an update carrying a command by its command name")
   public void testRoutesByCommand()
   {
      BotUpdate update = new FakeBotUpdate(UpdateType.MESSAGE, "start", List.of(), 1L, 2L, "/start");

      BotResponse response = dispatcher.dispatch(update);

      assertThat(response.replyText()).contains("welcome");
   }

   @Test
   @DisplayName("routes an update carrying no command by its update type")
   public void testRoutesByUpdateType()
   {
      BotUpdate update = new FakeBotUpdate(UpdateType.CALLBACK_QUERY, 1L, 2L, "data");

      BotResponse response = dispatcher.dispatch(update);

      assertThat(response.replyText()).contains("acknowledged");
   }

   @Test
   @DisplayName("silently ignores an update matching no registered handler")
   public void testIgnoresUnmatchedUpdate()
   {
      BotUpdate update = new FakeBotUpdate(UpdateType.EDITED_MESSAGE, 1L, 2L, "edited");

      BotResponse response = dispatcher.dispatch(update);

      assertThat(response.replyText()).isEmpty();
   }

   @Test
   @DisplayName("silently ignores an unknown command")
   public void testIgnoresUnknownCommand()
   {
      BotUpdate update = new FakeBotUpdate(UpdateType.MESSAGE, "unknown", List.of(), 1L, 2L, "/unknown");

      BotResponse response = dispatcher.dispatch(update);

      assertThat(response.replyText()).isEmpty();
   }
}
