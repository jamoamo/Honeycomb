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
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link MessageConsumerAdapterMethodInvoker}.
 *
 * @author James Amoore
 */
@DisplayName("MessageConsumerAdapterMethodInvoker")
public class MessageConsumerAdapterMethodInvokerTest
{
   public record OrderPlaced(String reference)
   {

   }

   @MessageConsumerAdapter(stream = "ORDERS")
   public static final class OrderAdapter
   {
      private String handledToken;
      private String handledCarrier;
      private OrderPlaced handledPayload;
      private long handledAttempt;

      @Subscription(subject = "orders.{orderId}.created", durable = "order-created")
      public void onCreated(
         @SubjectToken("orderId") final String orderId,
         @MessageHeader(value = "carrier", required = false) final String carrier,
         final ConsumedMessage message,
         final OrderPlaced payload)
      {
         this.handledToken = orderId;
         this.handledCarrier = carrier;
         this.handledPayload = payload;
         this.handledAttempt = message.deliveryAttempt();
      }

      @Subscription(subject = "orders.cancelled", durable = "order-cancelled")
      public MessageOutcome onCancelled()
      {
         return MessageOutcome.redeliverAfter(Duration.ofSeconds(30));
      }

      @Subscription(subject = "orders.shipped", durable = "order-shipped")
      public void onShipped()
      {
         throw new IllegalStateException("handler failed");
      }
   }

   private final SubscriptionScanner scanner = new SubscriptionScanner();
   private final MessageConsumerAdapterMethodInvoker invoker =
      new MessageConsumerAdapterMethodInvoker(new ObjectMapper());
   private final OrderAdapter adapter = new OrderAdapter();

   private SubscriptionRegistration registration(final String durable)
   {
      return scanner.scan(adapter).stream()
         .filter(candidate -> durable.equals(candidate.durable()))
         .findFirst()
         .orElseThrow();
   }

   @Test
   @DisplayName("binds every parameter and acknowledges the message")
   public void testBindsParametersAndAcknowledges()
   {
      ConsumedMessage message = new FakeConsumedMessage(
         "orders.42.created",
         Map.of("carrier", List.of("DHL")),
         "{\"reference\":\"REF-1\"}",
         3L);

      MessageOutcome outcome =
         invoker.invokeMessageConsumerAdapterMethod(registration("order-created"), message);

      assertThat(outcome.action()).isEqualTo(MessageOutcome.Action.ACKNOWLEDGE);
      assertThat(adapter.handledToken).isEqualTo("42");
      assertThat(adapter.handledCarrier).isEqualTo("DHL");
      assertThat(adapter.handledPayload).isEqualTo(new OrderPlaced("REF-1"));
      assertThat(adapter.handledAttempt).isEqualTo(3L);
   }

   @Test
   @DisplayName("returns the outcome the handler method decided on")
   public void testReturnsHandlerOutcome()
   {
      MessageOutcome outcome = invoker.invokeMessageConsumerAdapterMethod(
         registration("order-cancelled"), new FakeConsumedMessage("orders.cancelled"));

      assertThat(outcome.action()).isEqualTo(MessageOutcome.Action.REDELIVER);
      assertThat(outcome.delay()).isEqualTo(Duration.ofSeconds(30));
   }

   @Test
   @DisplayName("reports a message whose subject does not match the subscription as unbindable")
   public void testRejectsUnmatchedSubject()
   {
      SubscriptionRegistration registration = registration("order-created");
      ConsumedMessage message = new FakeConsumedMessage("orders.42.cancelled");

      assertThatExceptionOfType(MalformedMessageException.class)
         .isThrownBy(() -> invoker.invokeMessageConsumerAdapterMethod(registration, message))
         .withMessageContaining("does not match the subject");
   }

   @Test
   @DisplayName("reports an undeserializable payload as unbindable")
   public void testRejectsMalformedPayload()
   {
      SubscriptionRegistration registration = registration("order-created");
      ConsumedMessage message = new FakeConsumedMessage("orders.42.created", "not json");

      assertThatExceptionOfType(MalformedMessageException.class)
         .isThrownBy(() -> invoker.invokeMessageConsumerAdapterMethod(registration, message))
         .withMessageContaining("Malformed message payload");
   }

   @Test
   @DisplayName("wraps a failure thrown by the handler method")
   public void testWrapsHandlerFailure()
   {
      SubscriptionRegistration registration = registration("order-shipped");
      ConsumedMessage message = new FakeConsumedMessage("orders.shipped");

      assertThatExceptionOfType(MessagingException.class)
         .isThrownBy(() -> invoker.invokeMessageConsumerAdapterMethod(registration, message))
         .withMessageContaining("Error invoking handling method")
         .withRootCauseInstanceOf(IllegalStateException.class);
   }

   @Test
   @DisplayName("rejects a parameter no resolver supports")
   public void testRejectsUnsupportedParameter()
   {
      MessageConsumerAdapterMethodInvoker emptyInvoker =
         new MessageConsumerAdapterMethodInvoker(List.of());
      SubscriptionRegistration registration = registration("order-created");
      ConsumedMessage message = new FakeConsumedMessage("orders.42.created");

      assertThatExceptionOfType(IllegalStateException.class)
         .isThrownBy(() -> emptyInvoker.invokeMessageConsumerAdapterMethod(registration, message))
         .withMessageContaining("No argument resolver supports parameter");
   }
}
