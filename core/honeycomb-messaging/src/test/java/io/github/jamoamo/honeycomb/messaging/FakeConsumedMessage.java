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
package io.github.jamoamo.honeycomb.messaging;

import io.github.jamoamo.honeycomb.messaging.message.ConsumedMessage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * A simple in-memory {@link ConsumedMessage} for tests.
 *
 * @author James Amoore
 */
public final class FakeConsumedMessage implements ConsumedMessage
{
   private final String subject;
   private final Map<String, List<String>> headers;
   private final String payload;
   private final long deliveryAttempt;

   public FakeConsumedMessage(final String subject)
   {
      this(subject, Map.of(), "", 1L);
   }

   public FakeConsumedMessage(final String subject, final String payload)
   {
      this(subject, Map.of(), payload, 1L);
   }

   public FakeConsumedMessage(
      final String subject,
      final Map<String, List<String>> headers,
      final String payload,
      final long deliveryAttempt)
   {
      this.subject = subject;
      this.headers = headers;
      this.payload = payload;
      this.deliveryAttempt = deliveryAttempt;
   }

   @Override
   public String subject()
   {
      return subject;
   }

   @Override
   public List<String> headerValues(final String name)
   {
      return headers.getOrDefault(name, List.of());
   }

   @Override
   public InputStream payload()
   {
      return new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8));
   }

   @Override
   public long deliveryAttempt()
   {
      return deliveryAttempt;
   }
}
