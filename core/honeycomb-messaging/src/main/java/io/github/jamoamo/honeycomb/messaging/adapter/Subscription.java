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
package io.github.jamoamo.honeycomb.messaging.adapter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A subscription on a {@link MessageConsumerAdapter} class: the method handles the messages delivered by a
 * durable consumer.
 *
 * <p>
 * The consumer named by {@link #durable()} is bound to, never created: it is expected to already exist on the
 * stream, so that the topology of streams and consumers stays owned by whatever provisions the broker rather
 * than by the deployment of an application.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Subscription
{
   /**
    * The subject the method handles. May declare subject tokens, for example
    * {@code orders.{orderId}.created}, whose values are bound to {@link SubjectToken} parameters.
    *
    * @return the subject template
    */
   String subject();

   /**
    * The name of the durable consumer delivering the messages.
    *
    * @return the durable consumer name
    */
   String durable();

   /**
    * The name of the stream to consume from, overriding the stream declared by the adapter.
    *
    * @return the stream name, or an empty string to use the adapter's stream
    */
   String stream() default "";
}
