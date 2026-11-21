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
package io.github.jamoamo.honeycomb.rest.idempotency;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Makes an endpoint idempotent: a request that repeats an idempotency key already used against the endpoint, by the
 * same caller and with the same method, path and body, is answered with the stored response of the first request
 * instead of invoking the endpoint again.
 *
 * <p>
 * Only successful ({@code 2xx}) responses are stored; after any other response the key may be retried. Typically used
 * on {@code POST} and {@code PATCH} endpoints.
 * </p>
 *
 * @author James Amoore
 * @since 1.1.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent
{
   /**
    * Whether requests must carry an idempotency key; when {@code false} (the default) a request without one is
    * invoked normally.
    *
    * @return {@code true} to answer requests without a key with a {@code 400}
    */
   boolean required() default false;

   /**
    * How long a key is remembered, as an ISO-8601 duration; 24 hours by default.
    *
    * @return the key time-to-live
    */
   String ttl() default "PT24H";
}
