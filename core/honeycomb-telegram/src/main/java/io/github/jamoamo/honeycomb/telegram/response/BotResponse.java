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
package io.github.jamoamo.honeycomb.telegram.response;

import java.util.Objects;
import java.util.Optional;

/**
 * What a handler method asks the bot to reply with, if anything.
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class BotResponse
{
   private static final BotResponse NONE = new BotResponse(null);

   private final String replyText;

   private BotResponse(final String replyText)
   {
      this.replyText = replyText;
   }

   /**
    * A response asking the bot to reply with the given text.
    *
    * @param text the reply text
    * @return the response
    */
   public static BotResponse reply(final String text)
   {
      return new BotResponse(Objects.requireNonNull(text));
   }

   /**
    * A response asking the bot to send no reply.
    *
    * @return the response
    */
   public static BotResponse none()
   {
      return NONE;
   }

   /**
    * The text the bot should reply with.
    *
    * @return the reply text, or empty when no reply should be sent
    */
   public Optional<String> replyText()
   {
      return Optional.ofNullable(replyText);
   }
}
