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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TelegramBotsUpdate}.
 *
 * @author James Amoore
 */
@DisplayName("TelegramBotsUpdate")
public class TelegramBotsUpdateTest
{
   private final Update update = mock(Update.class);
   private final Message message = mock(Message.class);
   private final User user = mock(User.class);

   @Test
   @DisplayName("parses the command and arguments from a command message")
   public void testParsesCommand()
   {
      when(update.getMessage()).thenReturn(message);
      when(message.getText()).thenReturn("/book 42 window");
      when(message.getChatId()).thenReturn(100L);
      when(message.getFrom()).thenReturn(user);
      when(user.getId()).thenReturn(7L);

      TelegramBotsUpdate botUpdate = new TelegramBotsUpdate(update);

      assertThat(botUpdate.type()).isEqualTo(UpdateType.MESSAGE);
      assertThat(botUpdate.command()).contains("book");
      assertThat(botUpdate.commandArguments()).containsExactly("42", "window");
      assertThat(botUpdate.chatId()).isEqualTo(100L);
      assertThat(botUpdate.userId()).isEqualTo(7L);
      assertThat(botUpdate.text()).isEqualTo("/book 42 window");
   }

   @Test
   @DisplayName("strips the bot username suffix from a command")
   public void testStripsBotUsername()
   {
      when(update.getMessage()).thenReturn(message);
      when(message.getText()).thenReturn("/book@MyBot 42");

      TelegramBotsUpdate botUpdate = new TelegramBotsUpdate(update);

      assertThat(botUpdate.command()).contains("book");
      assertThat(botUpdate.commandArguments()).containsExactly("42");
   }

   @Test
   @DisplayName("reports no command for plain text")
   public void testNoCommandForPlainText()
   {
      when(update.getMessage()).thenReturn(message);
      when(message.getText()).thenReturn("hello there");

      TelegramBotsUpdate botUpdate = new TelegramBotsUpdate(update);

      assertThat(botUpdate.command()).isEmpty();
      assertThat(botUpdate.commandArguments()).isEmpty();
   }

   @Test
   @DisplayName("reports no arguments for a command with none")
   public void testNoArguments()
   {
      when(update.getMessage()).thenReturn(message);
      when(message.getText()).thenReturn("/start");

      TelegramBotsUpdate botUpdate = new TelegramBotsUpdate(update);

      assertThat(botUpdate.commandArguments()).isEmpty();
   }

   @Test
   @DisplayName("reports an edited message as EDITED_MESSAGE")
   public void testEditedMessage()
   {
      when(update.hasEditedMessage()).thenReturn(true);
      when(update.getEditedMessage()).thenReturn(message);
      when(message.getText()).thenReturn("edited text");

      TelegramBotsUpdate botUpdate = new TelegramBotsUpdate(update);

      assertThat(botUpdate.type()).isEqualTo(UpdateType.EDITED_MESSAGE);
      assertThat(botUpdate.text()).isEqualTo("edited text");
   }

   @Test
   @DisplayName("reads chat id, user id and data from a callback query with an accessible message")
   public void testCallbackQueryWithAccessibleMessage()
   {
      CallbackQuery callbackQuery = mock(CallbackQuery.class);
      when(update.hasCallbackQuery()).thenReturn(true);
      when(update.getCallbackQuery()).thenReturn(callbackQuery);
      when(callbackQuery.getData()).thenReturn("choice:1");
      when(callbackQuery.getFrom()).thenReturn(user);
      when(user.getId()).thenReturn(9L);
      when(callbackQuery.getMessage()).thenReturn(message);
      when(message.getChatId()).thenReturn(55L);

      TelegramBotsUpdate botUpdate = new TelegramBotsUpdate(update);

      assertThat(botUpdate.type()).isEqualTo(UpdateType.CALLBACK_QUERY);
      assertThat(botUpdate.text()).isEqualTo("choice:1");
      assertThat(botUpdate.userId()).isEqualTo(9L);
      assertThat(botUpdate.chatId()).isEqualTo(55L);
   }

   @Test
   @DisplayName("reports chat id zero for a callback query whose message is inaccessible")
   public void testCallbackQueryWithInaccessibleMessage()
   {
      CallbackQuery callbackQuery = mock(CallbackQuery.class);
      when(update.hasCallbackQuery()).thenReturn(true);
      when(update.getCallbackQuery()).thenReturn(callbackQuery);
      when(callbackQuery.getMessage()).thenReturn(null);

      TelegramBotsUpdate botUpdate = new TelegramBotsUpdate(update);

      assertThat(botUpdate.chatId()).isEqualTo(0L);
   }
}
