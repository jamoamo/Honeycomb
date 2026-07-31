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
import io.github.jamoamo.honeycomb.messaging.MalformedMessageException;
import io.github.jamoamo.honeycomb.messaging.MessageConsumerAdapterMethodInvoker;
import io.github.jamoamo.honeycomb.messaging.MessageOutcome;
import io.github.jamoamo.honeycomb.messaging.MessagingException;
import io.github.jamoamo.honeycomb.messaging.SubscriptionRegistration;

import io.nats.client.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link JetStreamMessageDispatcher}.
 *
 * @author James Amoore
 */
@DisplayName("JetStreamMessageDispatcher")
public class JetStreamMessageDispatcherTest
{
   private static final Duration REDELIVERY_DELAY = Duration.ofSeconds(5);

   private final MessageConsumerAdapterMethodInvoker invoker =
      mock(MessageConsumerAdapterMethodInvoker.class);
   private final SubscriptionRegistration registration = new SubscriptionRegistration(
      "ORDERS",
      "order-created",
      new DefaultSubjectTemplateParser().parse("orders.created"),
      new HandlerMethod(new Object(), null));
   private final Message message = mock(Message.class);
   private final JetStreamMessageDispatcher dispatcher =
      new JetStreamMessageDispatcher(registration, invoker, REDELIVERY_DELAY);

   @Test
   @DisplayName("acknowledges a handled message")
   public void testAcknowledges()
   {
      when(invoker.invokeMessageConsumerAdapterMethod(any(), any())).thenReturn(MessageOutcome.acknowledge());

      dispatcher.onMessage(message);

      verify(message).ack();
   }

   @Test
   @DisplayName("redelivers after the delay the handler asked for")
   public void testRedeliversAfterRequestedDelay()
   {
      when(invoker.invokeMessageConsumerAdapterMethod(any(), any()))
         .thenReturn(MessageOutcome.redeliverAfter(Duration.ofSeconds(30)));

      dispatcher.onMessage(message);

      verify(message).nakWithDelay(Duration.ofSeconds(30));
   }

   @Test
   @DisplayName("redelivers after the configured delay when the handler asked for none")
   public void testRedeliversAfterConfiguredDelay()
   {
      when(invoker.invokeMessageConsumerAdapterMethod(any(), any()))
         .thenReturn(MessageOutcome.redeliverAfter(null));

      dispatcher.onMessage(message);

      verify(message).nakWithDelay(REDELIVERY_DELAY);
   }

   @Test
   @DisplayName("terminates a message the handler refused")
   public void testTerminatesRefusedMessage()
   {
      when(invoker.invokeMessageConsumerAdapterMethod(any(), any()))
         .thenReturn(MessageOutcome.terminate("unknown order"));

      dispatcher.onMessage(message);

      verify(message).term();
   }

   @Test
   @DisplayName("terminates an unbindable message rather than redelivering it")
   public void testTerminatesUnbindableMessage()
   {
      when(invoker.invokeMessageConsumerAdapterMethod(any(), any()))
         .thenThrow(new MalformedMessageException("malformed"));

      dispatcher.onMessage(message);

      verify(message).term();
   }

   @Test
   @DisplayName("redelivers a message whose handling failed")
   public void testRedeliversFailedMessage()
   {
      when(invoker.invokeMessageConsumerAdapterMethod(any(), any()))
         .thenThrow(new MessagingException("handler failed"));

      dispatcher.onMessage(message);

      verify(message).nakWithDelay(REDELIVERY_DELAY);
   }
}
