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

import io.github.jamoamo.honeycomb.telegram.adapter.ChatId;
import io.github.jamoamo.honeycomb.telegram.adapter.Command;
import io.github.jamoamo.honeycomb.telegram.adapter.CommandArgument;
import io.github.jamoamo.honeycomb.telegram.adapter.OnUpdate;
import io.github.jamoamo.honeycomb.telegram.adapter.TelegramBotAdapter;
import io.github.jamoamo.honeycomb.telegram.adapter.UserId;
import io.github.jamoamo.honeycomb.telegram.response.BotResponse;
import io.github.jamoamo.honeycomb.telegram.update.BotUpdate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link TelegramBotAdapterMethodInvoker}.
 *
 * @author James Amoore
 */
@DisplayName("TelegramBotAdapterMethodInvoker")
public class TelegramBotAdapterMethodInvokerTest
{
   @TelegramBotAdapter
   public static final class GreeterAdapter
   {
      private long handledChatId;
      private long handledUserId;
      private String handledArgument;

      @Command("greet")
      public BotResponse onGreet(
         @CommandArgument(index = 0) final String name, @ChatId final long chatId, @UserId final long userId)
      {
         this.handledChatId = chatId;
         this.handledUserId = userId;
         this.handledArgument = name;
         return BotResponse.reply("Hello, " + name + "!");
      }

      @OnUpdate(type = UpdateType.MESSAGE)
      public void onMessage(final BotUpdate update)
      {
         this.handledArgument = update.text();
      }

      @Command("broken")
      public BotResponse onBroken()
      {
         throw new IllegalStateException("handler failed");
      }

      @Command("wrongReturnType")
      public String onWrongReturnType()
      {
         return "not a BotResponse";
      }
   }

   private final BotUpdateScanner scanner = new BotUpdateScanner();
   private final TelegramBotAdapterMethodInvoker invoker = new TelegramBotAdapterMethodInvoker();
   private final GreeterAdapter adapter = new GreeterAdapter();
   private final BotAdapterRegistry registry = scanner.scan(adapter);

   private HandlerMethod commandHandler(final String name)
   {
      return registry.findCommand(name).orElseThrow().handler();
   }

   @Test
   @DisplayName("binds command arguments, chat id and user id, and returns the handler's response")
   public void testBindsCommandArguments()
   {
      BotUpdate update = new FakeBotUpdate(UpdateType.MESSAGE, "greet", List.of("World"), 1L, 2L, "/greet World");

      BotResponse response = invoker.invoke(commandHandler("greet"), update);

      assertThat(response.replyText()).contains("Hello, World!");
      assertThat(adapter.handledChatId).isEqualTo(1L);
      assertThat(adapter.handledUserId).isEqualTo(2L);
      assertThat(adapter.handledArgument).isEqualTo("World");
   }

   @Test
   @DisplayName("treats a void handler method as sending no reply")
   public void testVoidHandlerSendsNoReply()
   {
      HandlerMethod handler = registry.findUpdate(UpdateType.MESSAGE).orElseThrow().handler();
      BotUpdate update = new FakeBotUpdate(UpdateType.MESSAGE, 1L, 2L, "hello there");

      BotResponse response = invoker.invoke(handler, update);

      assertThat(response.replyText()).isEmpty();
      assertThat(adapter.handledArgument).isEqualTo("hello there");
   }

   @Test
   @DisplayName("reports a command delivered with too few arguments as unbindable")
   public void testRejectsMissingCommandArgument()
   {
      BotUpdate update = new FakeBotUpdate(UpdateType.MESSAGE, "greet", List.of(), 1L, 2L, "/greet");

      assertThatExceptionOfType(UpdateBindingException.class)
         .isThrownBy(() -> invoker.invoke(commandHandler("greet"), update))
         .withMessageContaining("Missing command argument");
   }

   @Test
   @DisplayName("wraps a failure thrown by the handler method")
   public void testWrapsHandlerFailure()
   {
      BotUpdate update = new FakeBotUpdate(UpdateType.MESSAGE, "broken", List.of(), 1L, 2L, "/broken");

      assertThatExceptionOfType(TelegramException.class)
         .isThrownBy(() -> invoker.invoke(commandHandler("broken"), update))
         .withMessageContaining("Error invoking handling method")
         .withRootCauseInstanceOf(IllegalStateException.class);
   }

   @Test
   @DisplayName("rejects a handler method returning neither void nor BotResponse")
   public void testRejectsUnsupportedReturnType()
   {
      BotUpdate update = new FakeBotUpdate(UpdateType.MESSAGE, "wrongReturnType", List.of(), 1L, 2L, "/x");

      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> invoker.invoke(commandHandler("wrongReturnType"), update))
         .withMessageContaining("must return void or BotResponse");
   }

   @Test
   @DisplayName("rejects a parameter no resolver supports")
   public void testRejectsUnsupportedParameter()
   {
      TelegramBotAdapterMethodInvoker emptyInvoker = new TelegramBotAdapterMethodInvoker(List.of());
      BotUpdate update = new FakeBotUpdate(UpdateType.MESSAGE, "greet", List.of("World"), 1L, 2L, "/greet World");

      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> emptyInvoker.invoke(commandHandler("greet"), update))
         .withMessageContaining("No argument resolver supports parameter");
   }
}
