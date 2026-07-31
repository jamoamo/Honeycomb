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

import io.nats.client.Message;
import io.nats.client.impl.Headers;
import io.nats.client.impl.NatsJetStreamMetaData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link JetStreamConsumedMessage}.
 *
 * @author James Amoore
 */
@DisplayName("JetStreamConsumedMessage")
public class JetStreamConsumedMessageTest
{
   private final Message nativeMessage = mock(Message.class);
   private final JetStreamConsumedMessage message = new JetStreamConsumedMessage(nativeMessage);

   @Test
   @DisplayName("exposes the subject of the message")
   public void testSubject()
   {
      when(nativeMessage.getSubject()).thenReturn("orders.42.created");

      assertThat(message.subject()).isEqualTo("orders.42.created");
   }

   @Test
   @DisplayName("exposes the values supplied for a header")
   public void testHeaderValues()
   {
      Headers headers = new Headers();
      headers.add("carrier", "DHL", "UPS");
      when(nativeMessage.getHeaders()).thenReturn(headers);

      assertThat(message.headerValues("carrier")).containsExactly("DHL", "UPS");
      assertThat(message.headerValues("absent")).isEmpty();
   }

   @Test
   @DisplayName("reports no header values when the message carries no headers")
   public void testAbsentHeaders()
   {
      when(nativeMessage.getHeaders()).thenReturn(null);

      assertThat(message.headerValues("carrier")).isEqualTo(List.of());
   }

   @Test
   @DisplayName("streams the payload")
   public void testPayload() throws IOException
   {
      when(nativeMessage.getData()).thenReturn("{}".getBytes(StandardCharsets.UTF_8));

      assertThat(message.payload().readAllBytes()).asString(StandardCharsets.UTF_8).isEqualTo("{}");
   }

   @Test
   @DisplayName("streams an empty payload when the message carries no data")
   public void testAbsentPayload() throws IOException
   {
      when(nativeMessage.getData()).thenReturn(null);

      assertThat(message.payload().readAllBytes()).isEmpty();
   }

   @Test
   @DisplayName("reports the delivery attempt of a stream message")
   public void testDeliveryAttempt()
   {
      NatsJetStreamMetaData metaData = mock(NatsJetStreamMetaData.class);
      when(metaData.deliveredCount()).thenReturn(3L);
      when(nativeMessage.isJetStream()).thenReturn(true);
      when(nativeMessage.metaData()).thenReturn(metaData);

      assertThat(message.deliveryAttempt()).isEqualTo(3L);
   }

   @Test
   @DisplayName("reports a first delivery for a message carrying no stream metadata")
   public void testDeliveryAttemptWithoutMetadata()
   {
      when(nativeMessage.isJetStream()).thenReturn(false);

      assertThat(message.deliveryAttempt()).isEqualTo(1L);
   }

   @Test
   @DisplayName("exposes the message it was adapted from")
   public void testNativeMessage()
   {
      assertThat(message.nativeMessage()).isSameAs(nativeMessage);
   }
}
