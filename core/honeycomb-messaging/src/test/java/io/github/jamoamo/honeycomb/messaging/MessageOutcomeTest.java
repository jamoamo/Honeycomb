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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MessageOutcome} and the messaging exceptions.
 *
 * @author James Amoore
 */
@DisplayName("MessageOutcome")
public class MessageOutcomeTest
{
   @Test
   @DisplayName("acknowledges without a delay or a reason")
   public void testAcknowledge()
   {
      MessageOutcome outcome = MessageOutcome.acknowledge();

      assertThat(outcome.action()).isEqualTo(MessageOutcome.Action.ACKNOWLEDGE);
      assertThat(outcome.delay()).isNull();
      assertThat(outcome.reason()).isNull();
   }

   @Test
   @DisplayName("carries the delay it should be redelivered after")
   public void testRedeliverAfter()
   {
      MessageOutcome outcome = MessageOutcome.redeliverAfter(Duration.ofSeconds(30));

      assertThat(outcome.action()).isEqualTo(MessageOutcome.Action.REDELIVER);
      assertThat(outcome.delay()).isEqualTo(Duration.ofSeconds(30));
   }

   @Test
   @DisplayName("carries the reason it cannot be handled")
   public void testTerminate()
   {
      MessageOutcome outcome = MessageOutcome.terminate("unknown order");

      assertThat(outcome.action()).isEqualTo(MessageOutcome.Action.TERMINATE);
      assertThat(outcome.reason()).isEqualTo("unknown order");
   }

   @Test
   @DisplayName("reports an unbindable message as a messaging failure")
   public void testExceptionHierarchy()
   {
      Throwable cause = new IllegalStateException("cause");

      assertThat(new MalformedMessageException("message"))
         .isInstanceOf(MessagingException.class)
         .hasMessage("message");
      assertThat(new MalformedMessageException("message", cause)).hasCause(cause);
      assertThat(new MessagingException("message", cause)).hasCause(cause);
   }
}
