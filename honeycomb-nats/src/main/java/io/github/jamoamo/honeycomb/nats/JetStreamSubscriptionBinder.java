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

import io.github.jamoamo.honeycomb.messaging.MessageConsumerAdapterMethodInvoker;
import io.github.jamoamo.honeycomb.messaging.MessagingException;
import io.github.jamoamo.honeycomb.messaging.SubscriptionRegistration;

import io.nats.client.Connection;
import io.nats.client.ConsumerContext;
import io.nats.client.JetStreamApiException;
import io.nats.client.MessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Binds message consumer adapter subscriptions to the durable JetStream consumers that deliver their
 * messages.
 *
 * <p>
 * Each subscription binds to a consumer that already exists on its stream: a subscription naming a consumer
 * the broker does not have fails here, at startup, rather than silently consuming nothing. Nothing about the
 * stream or consumer topology is created or altered, so it stays owned by whatever provisions the broker.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class JetStreamSubscriptionBinder implements AutoCloseable
{
   /**
    * The delay applied before a message whose handling failed is redelivered, when no other delay is given.
    */
   public static final Duration DEFAULT_REDELIVERY_DELAY = Duration.ofSeconds(5);

   private static final Logger LOGGER = LoggerFactory.getLogger(JetStreamSubscriptionBinder.class);

   private final Connection connection;
   private final MessageConsumerAdapterMethodInvoker invoker;
   private final Duration redeliveryDelay;
   private final List<MessageConsumer> consumers = new ArrayList<>();

   /**
    * Constructor using the default redelivery delay.
    *
    * @param connection the connection to the broker
    * @param invoker    the method invoker used to dispatch delivered messages
    */
   public JetStreamSubscriptionBinder(
      final Connection connection, final MessageConsumerAdapterMethodInvoker invoker)
   {
      this(connection, invoker, DEFAULT_REDELIVERY_DELAY);
   }

   /**
    * Constructor.
    *
    * @param connection      the connection to the broker
    * @param invoker         the method invoker used to dispatch delivered messages
    * @param redeliveryDelay how long the broker should wait before redelivering a message whose handling
    *        failed
    */
   public JetStreamSubscriptionBinder(
      final Connection connection,
      final MessageConsumerAdapterMethodInvoker invoker,
      final Duration redeliveryDelay)
   {
      this.connection = connection;
      this.invoker = invoker;
      this.redeliveryDelay = redeliveryDelay;
   }

   /**
    * Binds the given subscriptions and begins consuming.
    *
    * @param registrations the subscriptions to bind
    * @throws MessagingException if a subscription names a consumer that cannot be bound to
    */
   public void bind(final Collection<SubscriptionRegistration> registrations)
   {
      for (SubscriptionRegistration registration : registrations)
      {
         consumers.add(consume(registration));
      }
   }

   /**
    * Stops consuming from every bound consumer.
    */
   @Override
   public void close()
   {
      for (MessageConsumer consumer : consumers)
      {
         consumer.stop();
      }

      consumers.clear();
   }

   private MessageConsumer consume(final SubscriptionRegistration registration)
   {
      try
      {
         ConsumerContext consumerContext = connection
            .getStreamContext(registration.stream())
            .getConsumerContext(registration.durable());

         LOGGER.info(
            "Binding subscription for subject {} to consumer {} on stream {}.",
            registration.template().filterSubject(),
            registration.durable(),
            registration.stream());

         return consumerContext.consume(
            new JetStreamMessageDispatcher(registration, invoker, redeliveryDelay));
      }
      catch (final IOException | JetStreamApiException ex)
      {
         throw new MessagingException(
            "Could not bind to consumer '" + registration.durable()
               + "' on stream '" + registration.stream() + "'.", ex);
      }
   }
}
