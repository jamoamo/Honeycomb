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
package io.github.jamoamo.honeycomb.nats;

import io.github.jamoamo.honeycomb.messaging.message.ConsumedMessage;

import io.nats.client.Message;
import io.nats.client.impl.Headers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Adapts a NATS {@link Message} to the broker-neutral {@link ConsumedMessage} the messaging core works
 * against.
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class JetStreamConsumedMessage implements ConsumedMessage
{
   private static final long FIRST_DELIVERY = 1L;

   private final Message message;

   /**
    * Constructor.
    *
    * @param message the message delivered by JetStream
    */
   public JetStreamConsumedMessage(final Message message)
   {
      this.message = message;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String subject()
   {
      return message.getSubject();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<String> headerValues(final String name)
   {
      Headers headers = message.getHeaders();
      if (headers == null)
      {
         return List.of();
      }

      List<String> values = headers.get(name);
      if (values == null)
      {
         return List.of();
      }

      return List.copyOf(values);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public InputStream payload()
   {
      byte[] data = message.getData();
      return new ByteArrayInputStream(data == null ? new byte[0] : data);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public long deliveryAttempt()
   {
      if (!message.isJetStream())
      {
         return FIRST_DELIVERY;
      }

      return message.metaData().deliveredCount();
   }

   /**
    * The message this was adapted from.
    *
    * @return the underlying NATS message
    */
   public Message nativeMessage()
   {
      return message;
   }
}
