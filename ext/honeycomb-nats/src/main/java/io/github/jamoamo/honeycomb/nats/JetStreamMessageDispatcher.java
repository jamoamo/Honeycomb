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

import io.github.jamoamo.honeycomb.messaging.MalformedMessageException;
import io.github.jamoamo.honeycomb.messaging.MessageConsumerAdapterMethodInvoker;
import io.github.jamoamo.honeycomb.messaging.MessageOutcome;
import io.github.jamoamo.honeycomb.messaging.MessagingException;
import io.github.jamoamo.honeycomb.messaging.SubscriptionRegistration;

import io.nats.client.Message;
import io.nats.client.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Dispatches the messages delivered for one subscription to its handler method and applies the resulting
 * {@link MessageOutcome} to the message.
 *
 * <p>
 * A message that could never be bound is terminated rather than redelivered; any other handling failure leaves
 * the message for redelivery after the configured delay. This is the messaging counterpart of turning a
 * handling failure into a response status.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class JetStreamMessageDispatcher implements MessageHandler
{
   private static final Logger LOGGER = LoggerFactory.getLogger(JetStreamMessageDispatcher.class);

   private final SubscriptionRegistration registration;
   private final MessageConsumerAdapterMethodInvoker invoker;
   private final Duration redeliveryDelay;

   /**
    * Constructor.
    *
    * @param registration    the subscription whose messages are dispatched
    * @param invoker         the method invoker
    * @param redeliveryDelay how long the broker should wait before redelivering a message whose handling
    *        failed
    */
   public JetStreamMessageDispatcher(
      final SubscriptionRegistration registration,
      final MessageConsumerAdapterMethodInvoker invoker,
      final Duration redeliveryDelay)
   {
      this.registration = registration;
      this.invoker = invoker;
      this.redeliveryDelay = redeliveryDelay;
   }

   /**
    * Handles a message delivered by JetStream.
    *
    * @param message the delivered message
    */
   @Override
   public void onMessage(final Message message)
   {
      try
      {
         MessageOutcome outcome = invoker.invokeMessageConsumerAdapterMethod(
            registration, new JetStreamConsumedMessage(message));

         apply(message, outcome);
      }
      catch (final MalformedMessageException ex)
      {
         LOGGER.error("Terminating unbindable message on subject {}.", message.getSubject(), ex);
         message.term();
      }
      catch (final MessagingException ex)
      {
         LOGGER.error("Redelivering unhandled message on subject {}.", message.getSubject(), ex);
         message.nakWithDelay(redeliveryDelay);
      }
   }

   private void apply(final Message message, final MessageOutcome outcome)
   {
      switch (outcome.action())
      {
         case REDELIVER -> message.nakWithDelay(delayOf(outcome));
         case TERMINATE ->
         {
            LOGGER.warn("Terminating message on subject {}: {}", message.getSubject(), outcome.reason());
            message.term();
         }
         default -> message.ack();
      }
   }

   private Duration delayOf(final MessageOutcome outcome)
   {
      return outcome.delay() == null ? redeliveryDelay : outcome.delay();
   }
}
