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
package io.github.jamoamo.honeycomb.messaging.message;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * A broker-neutral view of a message delivered to a consumer.
 *
 * <p>
 * This is the integration seam that keeps {@code honeycomb-messaging} free of any particular broker client. A
 * hosting module (for example the NATS JetStream integration) adapts its native message type to this
 * interface, and everything in the core - subject matching, argument resolution, payload deserialization -
 * works against it.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public interface ConsumedMessage
{
   /**
    * The subject the message was published to.
    *
    * @return the message subject
    */
   String subject();

   /**
    * The values supplied for the named header.
    *
    * @param name the header name
    * @return the values in the order supplied, or an empty list when the header is absent
    */
   List<String> headerValues(String name);

   /**
    * The message payload.
    *
    * @return a stream over the payload
    * @throws IOException if the payload cannot be opened
    */
   InputStream payload() throws IOException;

   /**
    * The number of times the message has been delivered, counting the current delivery. A message being
    * delivered for the first time reports {@code 1}.
    *
    * @return the delivery attempt
    */
   long deliveryAttempt();
}
