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
package io.github.jamoamo.honeycomb.telegram.update;

import io.github.jamoamo.honeycomb.telegram.UpdateType;

import java.util.List;
import java.util.Optional;

/**
 * A bot-library-neutral view of an update delivered to a bot.
 *
 * <p>
 * This is the integration seam that keeps {@code honeycomb-telegram} free of any particular bot client. A
 * hosting module (for example a binding onto the {@code telegrambots} library) adapts its native update type to
 * this interface, and everything in the core - command routing, argument resolution - works against it.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public interface BotUpdate
{
   /**
    * The kind of update this is.
    *
    * @return the update type
    */
   UpdateType type();

   /**
    * The bot command the update carries, when its text begins with {@code /}.
    *
    * @return the command text, without the leading {@code /} and without any following arguments, or empty
    *         when the update does not carry a command
    */
   Optional<String> command();

   /**
    * The whitespace-separated arguments following the command, when the update carries one.
    *
    * @return the command arguments in order, or an empty list when the update carries no command or the
    *         command has no arguments
    */
   List<String> commandArguments();

   /**
    * The id of the chat the update was delivered for.
    *
    * @return the chat id
    */
   long chatId();

   /**
    * The id of the user the update was sent by.
    *
    * @return the user id
    */
   long userId();

   /**
    * The text carried by the update: a message's text, or a callback query's data.
    *
    * @return the update text
    */
   String text();
}
