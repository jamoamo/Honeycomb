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
package io.github.jamoamo.honeycomb.rest.intercept;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Mutable, per-request values shared between {@link EndpointInterceptor}s and argument resolvers, for example
 * the authenticated principal.
 *
 * <p>
 * An instance belongs to a single request and is not thread safe.
 * </p>
 *
 * @author James Amoore
 * @since 1.1.0
 */
public final class Attributes
{
   private static final String KEY_NULL = "key cannot be null";

   private final Map<AttributeKey<?>, Object> values = new HashMap<>();

   /**
    * The value stored under the key.
    *
    * @param key the key
    * @param <T> the value type
    * @return the value, or empty when none is stored
    */
   public <T> Optional<T> get(final AttributeKey<T> key)
   {
      Objects.requireNonNull(key, KEY_NULL);
      return Optional.ofNullable(values.get(key)).map(key.type()::cast);
   }

   /**
    * Stores a value under the key, replacing any value already stored.
    *
    * @param key   the key
    * @param value the value
    * @param <T>   the value type
    */
   public <T> void put(final AttributeKey<T> key, final T value)
   {
      Objects.requireNonNull(key, KEY_NULL);
      values.put(key, key.type().cast(Objects.requireNonNull(value, "value cannot be null")));
   }

   /**
    * Removes the value stored under the key, if any.
    *
    * @param key the key
    */
   public void remove(final AttributeKey<?> key)
   {
      values.remove(Objects.requireNonNull(key, KEY_NULL));
   }
}
