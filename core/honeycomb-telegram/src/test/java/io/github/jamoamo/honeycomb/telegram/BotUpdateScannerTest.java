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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link BotUpdateScanner}.
 *
 * @author James Amoore
 */
@DisplayName("BotUpdateScanner")
public class BotUpdateScannerTest
{
   @TelegramBotAdapter(botName = "support")
   public static final class SupportAdapter
   {
      @Command("start")
      public void onStart()
      {
         // handled
      }

      @OnUpdate(type = UpdateType.CALLBACK_QUERY)
      public void onCallback()
      {
         // handled
      }

      public void notAHandler()
      {
         // ignored
      }
   }

   @TelegramBotAdapter
   public static final class DuplicateCommandAdapter
   {
      @Command("start")
      public void onStart()
      {
         // handled
      }
   }

   @TelegramBotAdapter
   public static final class DuplicateUpdateAdapter
   {
      @OnUpdate(type = UpdateType.CALLBACK_QUERY)
      public void onCallback()
      {
         // handled
      }
   }

   @TelegramBotAdapter
   public static final class EditedMessageAdapter
   {
      @OnUpdate(type = UpdateType.EDITED_MESSAGE)
      public void onEdited()
      {
         // handled
      }
   }

   @TelegramBotAdapter
   public static final class BothAnnotationsAdapter
   {
      @Command("start")
      @OnUpdate(type = UpdateType.MESSAGE)
      public void onBoth()
      {
         // handled
      }
   }

   public static final class NotAnAdapter
   {
   }

   private final BotUpdateScanner scanner = new BotUpdateScanner();

   @Test
   @DisplayName("resolves commands and update handlers declared by an adapter")
   public void testScansAdapter()
   {
      BotAdapterRegistry registry = scanner.scan(new SupportAdapter());

      assertThat(registry.findCommand("start")).isPresent();
      assertThat(registry.findUpdate(UpdateType.CALLBACK_QUERY)).isPresent();
      assertThat(registry.findCommand("missing")).isEmpty();
   }

   @Test
   @DisplayName("scans several adapters at once")
   public void testScansAllAdapters()
   {
      BotAdapterRegistry registry = scanner.scanAll(List.of(new SupportAdapter(), new EditedMessageAdapter()));

      assertThat(registry.findCommand("start")).isPresent();
      assertThat(registry.findUpdate(UpdateType.CALLBACK_QUERY)).isPresent();
      assertThat(registry.findUpdate(UpdateType.EDITED_MESSAGE)).isPresent();
   }

   @Test
   @DisplayName("rejects a class that is not a Telegram bot adapter")
   public void testRejectsNonAdapter()
   {
      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> scanner.scan(new NotAnAdapter()))
         .withMessageContaining("Not a telegram bot adapter");
   }

   @Test
   @DisplayName("rejects two commands sharing a name")
   public void testRejectsDuplicateCommand()
   {
      List<Object> adapters = List.of(new SupportAdapter(), new DuplicateCommandAdapter());

      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> scanner.scanAll(adapters))
         .withMessageContaining("Duplicate command registered");
   }

   @Test
   @DisplayName("rejects two update handlers sharing a type")
   public void testRejectsDuplicateUpdateType()
   {
      List<Object> adapters = List.of(new SupportAdapter(), new DuplicateUpdateAdapter());

      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> scanner.scanAll(adapters))
         .withMessageContaining("Duplicate update handler registered");
   }

   @Test
   @DisplayName("rejects a method annotated with both @Command and @OnUpdate")
   public void testRejectsBothAnnotations()
   {
      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> scanner.scan(new BothAnnotationsAdapter()))
         .withMessageContaining("either @Command or @OnUpdate");
   }
}
