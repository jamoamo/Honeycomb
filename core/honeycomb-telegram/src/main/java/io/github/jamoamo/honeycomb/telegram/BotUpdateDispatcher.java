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

import io.github.jamoamo.honeycomb.telegram.response.BotResponse;
import io.github.jamoamo.honeycomb.telegram.update.BotUpdate;

import java.util.Optional;

/**
 * Routes an incoming update to the handler registered for it and invokes it.
 *
 * <p>
 * An update carrying a bot command is routed by command name; any other update is routed by its
 * {@link UpdateType}. An update matching no registered handler is silently ignored, since a bot commonly
 * receives updates - and commands - it declares no interest in.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class BotUpdateDispatcher
{
   private final BotAdapterRegistry registry;
   private final TelegramBotAdapterMethodInvoker invoker;

   /**
    * Constructor.
    *
    * @param registry the commands and update handlers to dispatch to
    * @param invoker  the method invoker
    */
   public BotUpdateDispatcher(final BotAdapterRegistry registry, final TelegramBotAdapterMethodInvoker invoker)
   {
      this.registry = registry;
      this.invoker = invoker;
   }

   /**
    * Dispatches the given update to its handler.
    *
    * @param update the update to dispatch
    * @return the response the handler decided on, or {@link BotResponse#none()} when no handler matched
    */
   public BotResponse dispatch(final BotUpdate update)
   {
      Optional<HandlerMethod> handler = update.command()
         .flatMap(registry::findCommand)
         .map(CommandRegistration::handler)
         .or(() -> registry.findUpdate(update.type()).map(UpdateRegistration::handler));

      return handler.map(matched -> invoker.invoke(matched, update)).orElseGet(BotResponse::none);
   }
}
