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

import io.github.jamoamo.honeycomb.messaging.adapter.MessageConsumerAdapter;
import io.github.jamoamo.honeycomb.messaging.adapter.MessageHeader;
import io.github.jamoamo.honeycomb.messaging.adapter.SubjectToken;
import io.github.jamoamo.honeycomb.messaging.adapter.Subscription;
import io.github.jamoamo.honeycomb.messaging.message.ConsumedMessage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link SubscriptionScanner}.
 *
 * @author James Amoore
 */
@DisplayName("SubscriptionScanner")
public class SubscriptionScannerTest
{
   @MessageConsumerAdapter(stream = "ORDERS")
   public static final class OrderAdapter
   {
      @Subscription(subject = "orders.{orderId}.created", durable = "order-created")
      public void onCreated(@SubjectToken("orderId") final String orderId, final ConsumedMessage message)
      {
         // handled
      }

      @Subscription(subject = "orders.shipped", durable = "order-shipped", stream = "SHIPPING")
      public void onShipped(@MessageHeader("carrier") final String carrier)
      {
         // handled
      }

      public void notASubscription()
      {
         // ignored
      }
   }

   @MessageConsumerAdapter
   public static final class StreamlessAdapter
   {
      @Subscription(subject = "orders.created", durable = "order-created")
      public void onCreated()
      {
         // handled
      }
   }

   @MessageConsumerAdapter(stream = "ORDERS")
   public static final class UndeclaredTokenAdapter
   {
      @Subscription(subject = "orders.created", durable = "order-created")
      public void onCreated(@SubjectToken("orderId") final String orderId)
      {
         // handled
      }
   }

   @MessageConsumerAdapter(stream = "ORDERS")
   public static final class TwoPayloadAdapter
   {
      @Subscription(subject = "orders.created", durable = "order-created")
      public void onCreated(final String first, final String second)
      {
         // handled
      }
   }

   @MessageConsumerAdapter(stream = "ORDERS")
   public static final class DuplicateDurableAdapter
   {
      @Subscription(subject = "orders.created", durable = "order-created")
      public void onCreated()
      {
         // handled
      }
   }

   public static final class NotAnAdapter
   {
   }

   private final SubscriptionScanner scanner = new SubscriptionScanner();

   @Test
   @DisplayName("resolves each subscription to its stream, durable consumer and subject template")
   public void testScansSubscriptions()
   {
      List<SubscriptionRegistration> registrations = scanner.scan(new OrderAdapter());

      assertThat(registrations).hasSize(2);
      assertThat(registrations)
         .extracting(SubscriptionRegistration::durable)
         .containsExactlyInAnyOrder("order-created", "order-shipped");
      assertThat(registrations)
         .filteredOn(registration -> "order-created".equals(registration.durable()))
         .singleElement()
         .satisfies(registration ->
         {
            assertThat(registration.stream()).isEqualTo("ORDERS");
            assertThat(registration.template().filterSubject()).isEqualTo("orders.*.created");
            assertThat(registration.handler().method().getName()).isEqualTo("onCreated");
         });
   }

   @Test
   @DisplayName("prefers the stream declared by the subscription over the adapter's")
   public void testSubscriptionStreamWins()
   {
      List<SubscriptionRegistration> registrations = scanner.scan(new OrderAdapter());

      assertThat(registrations)
         .filteredOn(registration -> "order-shipped".equals(registration.durable()))
         .singleElement()
         .satisfies(registration -> assertThat(registration.stream()).isEqualTo("SHIPPING"));
   }

   @Test
   @DisplayName("scans several adapters at once")
   public void testScansAllAdapters()
   {
      List<SubscriptionRegistration> registrations =
         scanner.scanAll(List.of(new OrderAdapter(), new CancellationAdapter()));

      assertThat(registrations).hasSize(3);
   }

   @Test
   @DisplayName("rejects a class that is not a message consumer adapter")
   public void testRejectsNonAdapter()
   {
      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> scanner.scan(new NotAnAdapter()))
         .withMessageContaining("Not a message consumer adapter");
   }

   @Test
   @DisplayName("rejects a subscription naming no stream")
   public void testRejectsStreamlessSubscription()
   {
      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> scanner.scan(new StreamlessAdapter()))
         .withMessageContaining("must name a stream");
   }

   @Test
   @DisplayName("rejects a subject token the subject does not declare")
   public void testRejectsUndeclaredToken()
   {
      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> scanner.scan(new UndeclaredTokenAdapter()))
         .withMessageContaining("is not declared by the subject");
   }

   @Test
   @DisplayName("rejects a subscription declaring more than one payload parameter")
   public void testRejectsTwoPayloadParameters()
   {
      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> scanner.scan(new TwoPayloadAdapter()))
         .withMessageContaining("at most one payload parameter");
   }

   @Test
   @DisplayName("rejects two subscriptions sharing a durable consumer")
   public void testRejectsDuplicateDurable()
   {
      List<Object> adapters = List.of(new OrderAdapter(), new DuplicateDurableAdapter());

      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> scanner.scanAll(adapters))
         .withMessageContaining("Duplicate durable consumer");
   }

   @MessageConsumerAdapter(stream = "ORDERS")
   public static final class CancellationAdapter
   {
      @Subscription(subject = "orders.cancelled", durable = "order-cancelled")
      public void onCancelled()
      {
         // handled
      }
   }
}
