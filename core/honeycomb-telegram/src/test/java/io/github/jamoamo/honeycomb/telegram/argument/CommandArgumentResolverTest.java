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
package io.github.jamoamo.honeycomb.telegram.argument;

import io.github.jamoamo.honeycomb.adapter.argument.DefaultValueConverter;
import io.github.jamoamo.honeycomb.telegram.FakeBotUpdate;
import io.github.jamoamo.honeycomb.telegram.UpdateBindingException;
import io.github.jamoamo.honeycomb.telegram.UpdateType;
import io.github.jamoamo.honeycomb.telegram.adapter.CommandArgument;
import io.github.jamoamo.honeycomb.telegram.update.BotUpdate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link CommandArgumentResolver}.
 *
 * @author James Amoore
 */
@DisplayName("CommandArgumentResolver")
public class CommandArgumentResolverTest
{
   public void handle(@CommandArgument(index = 1) final int seats)
   {
      // used only for its parameter metadata
   }

   private static Parameter parameter() throws NoSuchMethodException
   {
      Method method = CommandArgumentResolverTest.class.getMethod("handle", int.class);
      return method.getParameters()[0];
   }

   private final CommandArgumentResolver resolver = new CommandArgumentResolver(new DefaultValueConverter());

   @Test
   @DisplayName("converts the argument at the declared index")
   public void testResolvesArgument() throws NoSuchMethodException
   {
      BotUpdate update = new FakeBotUpdate(UpdateType.MESSAGE, "book", List.of("42", "3"), 1L, 2L, "/book 42 3");

      Object value = resolver.resolve(parameter(), update);

      assertThat(value).isEqualTo(3);
   }

   @Test
   @DisplayName("reports a command delivered with too few arguments as unbindable")
   public void testRejectsMissingArgument() throws NoSuchMethodException
   {
      BotUpdate update = new FakeBotUpdate(UpdateType.MESSAGE, "book", List.of("42"), 1L, 2L, "/book 42");

      assertThatExceptionOfType(UpdateBindingException.class)
         .isThrownBy(() -> resolver.resolve(parameter(), update))
         .withMessageContaining("Missing command argument");
   }

   @Test
   @DisplayName("reports an argument that cannot be converted as unbindable")
   public void testRejectsUnconvertibleArgument() throws NoSuchMethodException
   {
      BotUpdate update =
         new FakeBotUpdate(UpdateType.MESSAGE, "book", List.of("42", "many"), 1L, 2L, "/book 42 many");

      assertThatExceptionOfType(UpdateBindingException.class)
         .isThrownBy(() -> resolver.resolve(parameter(), update));
   }
}
