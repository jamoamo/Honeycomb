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

import java.time.Duration;

/**
 * What should happen to a message once its handler method has run.
 *
 * <p>
 * Every handled message produces one of these, so that acknowledgement is decided in one place rather than
 * scattered through handler code. A handler method returning {@code void} or any other value acknowledges the
 * message; a handler method returning a {@code MessageOutcome} decides for itself.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class MessageOutcome
{
   /**
    * The action a broker should take for a handled message.
    */
   public enum Action
   {
      /**
       * The message was handled; it should not be delivered again.
       */
      ACKNOWLEDGE,

      /**
       * The message was not handled; it should be delivered again.
       */
      REDELIVER,

      /**
       * The message cannot be handled; it should not be delivered again despite not having been handled.
       */
      TERMINATE
   }

   private static final MessageOutcome ACKNOWLEDGED = new MessageOutcome(Action.ACKNOWLEDGE, null, null);

   private final Action action;
   private final Duration delay;
   private final String reason;

   private MessageOutcome(final Action action, final Duration delay, final String reason)
   {
      this.action = action;
      this.delay = delay;
      this.reason = reason;
   }

   /**
    * An outcome acknowledging the message.
    *
    * @return the outcome
    */
   public static MessageOutcome acknowledge()
   {
      return ACKNOWLEDGED;
   }

   /**
    * An outcome asking for the message to be delivered again after the given delay.
    *
    * @param delay how long the broker should wait before redelivering the message
    * @return the outcome
    */
   public static MessageOutcome redeliverAfter(final Duration delay)
   {
      return new MessageOutcome(Action.REDELIVER, delay, null);
   }

   /**
    * An outcome refusing the message permanently, for a message that could never be handled successfully.
    *
    * @param reason why the message cannot be handled
    * @return the outcome
    */
   public static MessageOutcome terminate(final String reason)
   {
      return new MessageOutcome(Action.TERMINATE, null, reason);
   }

   /**
    * The action the broker should take.
    *
    * @return the action
    */
   public Action action()
   {
      return action;
   }

   /**
    * How long the broker should wait before redelivering the message.
    *
    * @return the redelivery delay, or {@code null} when the action is not {@link Action#REDELIVER}
    */
   public Duration delay()
   {
      return delay;
   }

   /**
    * Why the message cannot be handled.
    *
    * @return the reason, or {@code null} when the action is not {@link Action#TERMINATE}
    */
   public String reason()
   {
      return reason;
   }
}
