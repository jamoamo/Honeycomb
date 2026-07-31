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
package io.github.jamoamo.honeycomb.messaging.argument;

import io.github.jamoamo.honeycomb.adapter.argument.AdapterArgumentResolver;
import io.github.jamoamo.honeycomb.messaging.message.ConsumedMessage;

import java.util.Map;

/**
 * Per-message context passed to each {@link AdapterArgumentResolver}. Exposes the message together with the
 * tokens captured from its subject.
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class MessageResolutionContext
{
   private final ConsumedMessage message;
   private final Map<String, String> subjectTokens;

   /**
    * Constructor.
    *
    * @param message       the message being handled
    * @param subjectTokens the tokens captured from the message subject, keyed by token name
    */
   public MessageResolutionContext(final ConsumedMessage message, final Map<String, String> subjectTokens)
   {
      this.message = message;
      this.subjectTokens = Map.copyOf(subjectTokens);
   }

   /**
    * The message being handled.
    *
    * @return the message
    */
   public ConsumedMessage message()
   {
      return message;
   }

   /**
    * The tokens captured from the message subject, keyed by token name.
    *
    * @return the captured tokens, or an empty map when none were captured
    */
   public Map<String, String> subjectTokens()
   {
      return subjectTokens;
   }
}
