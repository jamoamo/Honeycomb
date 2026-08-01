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

import io.github.jamoamo.honeycomb.adapter.argument.AdapterArgumentResolver;
import io.github.jamoamo.honeycomb.adapter.argument.DefaultValueConverter;
import io.github.jamoamo.honeycomb.adapter.argument.ValueConverter;
import io.github.jamoamo.honeycomb.telegram.argument.BotUpdateArgumentResolver;
import io.github.jamoamo.honeycomb.telegram.argument.ChatIdArgumentResolver;
import io.github.jamoamo.honeycomb.telegram.argument.CommandArgumentResolver;
import io.github.jamoamo.honeycomb.telegram.argument.UserIdArgumentResolver;
import io.github.jamoamo.honeycomb.telegram.response.BotResponse;
import io.github.jamoamo.honeycomb.telegram.update.BotUpdate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

/**
 * Invoker for the handler methods of a Telegram bot adapter.
 *
 * <p>
 * Arguments are bound from the update by consulting the configured {@link AdapterArgumentResolver}s in order
 * for each parameter, the handler method is invoked, and its return value is resolved into a
 * {@link BotResponse}. A handler method returning {@code void} sends no reply; one returning a
 * {@code BotResponse} decides for itself; any other return type is unsupported.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class TelegramBotAdapterMethodInvoker
{
   private static final Logger LOGGER = LoggerFactory.getLogger(TelegramBotAdapterMethodInvoker.class);

   private final List<AdapterArgumentResolver<BotUpdate>> argumentResolvers;

   /**
    * Constructor using the default argument resolvers.
    */
   public TelegramBotAdapterMethodInvoker()
   {
      this(defaultResolvers());
   }

   /**
    * Constructor.
    *
    * @param argumentResolvers the ordered resolvers used to bind handler-method parameters; the first
    *        resolver that supports a parameter resolves it
    */
   public TelegramBotAdapterMethodInvoker(final List<AdapterArgumentResolver<BotUpdate>> argumentResolvers)
   {
      this.argumentResolvers = List.copyOf(argumentResolvers);
   }

   /**
    * Invokes the given handler method for the given update.
    *
    * @param handler the method handling the update
    * @param update  the update being handled
    * @return the response the handler method decided on
    * @throws UpdateBindingException if the update cannot be bound to the handler method
    * @throws TelegramException      if the handler method throws or cannot be executed
    */
   public BotResponse invoke(final HandlerMethod handler, final BotUpdate update)
   {
      Method handlerMethod = handler.method();

      try
      {
         Object[] args = resolveArguments(handlerMethod, update);
         Object result = handlerMethod.invoke(handler.bean(), args);

         return responseOf(result);
      }
      catch (final InvocationTargetException ex)
      {
         LOGGER.error("Exception invoking handling method.", ex);
         throw new TelegramException("Error invoking handling method", ex.getCause());
      }
      catch (final IOException | IllegalAccessException ex)
      {
         LOGGER.error("Telegram bot adapter method error.", ex);
         throw new TelegramException("Method couldn't be executed", ex);
      }
   }

   private static BotResponse responseOf(final Object result)
   {
      if (result instanceof BotResponse response)
      {
         return response;
      }
      if (result == null)
      {
         return BotResponse.none();
      }

      throw new IllegalStateException(
         "Handler methods must return void or BotResponse, but returned: " + result.getClass().getName());
   }

   private Object[] resolveArguments(final Method handlerMethod, final BotUpdate update) throws IOException
   {
      Parameter[] parameters = handlerMethod.getParameters();
      Object[] args = new Object[parameters.length];

      for (int i = 0; i < parameters.length; i++)
      {
         args[i] = resolveArgument(parameters[i], update);
      }

      return args;
   }

   private Object resolveArgument(final Parameter parameter, final BotUpdate update) throws IOException
   {
      for (AdapterArgumentResolver<BotUpdate> resolver : argumentResolvers)
      {
         if (resolver.supports(parameter))
         {
            return resolver.resolve(parameter, update);
         }
      }

      throw new IllegalStateException("No argument resolver supports parameter " + parameter + ".");
   }

   private static List<AdapterArgumentResolver<BotUpdate>> defaultResolvers()
   {
      ValueConverter valueConverter = new DefaultValueConverter();
      return List.of(
         new CommandArgumentResolver(valueConverter),
         new ChatIdArgumentResolver(),
         new UserIdArgumentResolver(),
         new BotUpdateArgumentResolver());
   }
}
