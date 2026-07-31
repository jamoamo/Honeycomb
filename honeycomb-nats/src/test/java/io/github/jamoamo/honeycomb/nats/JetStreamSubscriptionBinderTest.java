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

import io.github.jamoamo.honeycomb.messaging.DefaultSubjectTemplateParser;
import io.github.jamoamo.honeycomb.messaging.HandlerMethod;
import io.github.jamoamo.honeycomb.messaging.MessageConsumerAdapterMethodInvoker;
import io.github.jamoamo.honeycomb.messaging.MessagingException;
import io.github.jamoamo.honeycomb.messaging.SubscriptionRegistration;

import io.nats.client.Connection;
import io.nats.client.ConsumerContext;
import io.nats.client.JetStreamApiException;
import io.nats.client.MessageConsumer;
import io.nats.client.MessageHandler;
import io.nats.client.StreamContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link JetStreamSubscriptionBinder}.
 *
 * @author James Amoore
 */
@DisplayName("JetStreamSubscriptionBinder")
public class JetStreamSubscriptionBinderTest
{
   private final Connection connection = mock(Connection.class);
   private final StreamContext streamContext = mock(StreamContext.class);
   private final ConsumerContext consumerContext = mock(ConsumerContext.class);
   private final MessageConsumer messageConsumer = mock(MessageConsumer.class);
   private final MessageConsumerAdapterMethodInvoker invoker =
      mock(MessageConsumerAdapterMethodInvoker.class);
   private final JetStreamSubscriptionBinder binder = new JetStreamSubscriptionBinder(connection, invoker);

   private static SubscriptionRegistration registration()
   {
      return new SubscriptionRegistration(
         "ORDERS",
         "order-created",
         new DefaultSubjectTemplateParser().parse("orders.{orderId}.created"),
         new HandlerMethod(new Object(), null));
   }

   @Test
   @DisplayName("consumes from the durable consumer the subscription names")
   public void testBindsToDurableConsumer() throws IOException, JetStreamApiException
   {
      when(connection.getStreamContext("ORDERS")).thenReturn(streamContext);
      when(streamContext.getConsumerContext("order-created")).thenReturn(consumerContext);
      when(consumerContext.consume(any(MessageHandler.class))).thenReturn(messageConsumer);

      binder.bind(List.of(registration()));

      verify(consumerContext).consume(any(MessageHandler.class));
   }

   @Test
   @DisplayName("stops consuming when closed")
   public void testStopsConsumersOnClose() throws IOException, JetStreamApiException
   {
      when(connection.getStreamContext("ORDERS")).thenReturn(streamContext);
      when(streamContext.getConsumerContext("order-created")).thenReturn(consumerContext);
      when(consumerContext.consume(any(MessageHandler.class))).thenReturn(messageConsumer);
      binder.bind(List.of(registration()));

      binder.close();

      verify(messageConsumer).stop();
   }

   @Test
   @DisplayName("fails when the consumer the subscription names does not exist")
   public void testFailsForUnknownConsumer() throws IOException, JetStreamApiException
   {
      when(connection.getStreamContext("ORDERS")).thenReturn(streamContext);
      when(streamContext.getConsumerContext("order-created"))
         .thenThrow(new IOException("no such consumer"));

      List<SubscriptionRegistration> registrations = List.of(registration());

      assertThatExceptionOfType(MessagingException.class)
         .isThrownBy(() -> binder.bind(registrations))
         .withMessageContaining("Could not bind to consumer 'order-created'");
   }
}
