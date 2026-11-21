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

import java.util.Objects;

/**
 * A typed key for a value held in the per-request {@link Attributes}.
 *
 * <p>
 * Keys are compared by identity, so two keys with the same name are distinct; declare a key once as a constant
 * and share it between the code that writes the value and the code that reads it.
 * </p>
 *
 * @author James Amoore
 * @param <T> the type of the value stored under the key
 * @since 1.1.0
 */
public final class AttributeKey<T>
{
   private final String name;
   private final Class<T> type;

   private AttributeKey(final String name, final Class<T> type)
   {
      this.name = Objects.requireNonNull(name, "name cannot be null");
      this.type = Objects.requireNonNull(type, "type cannot be null");
   }

   /**
    * Creates a key.
    *
    * @param name a descriptive name, used only in diagnostics
    * @param type the type of the value stored under the key
    * @param <T>  the type of the value stored under the key
    * @return a new key, distinct from every other key
    */
   public static <T> AttributeKey<T> of(final String name, final Class<T> type)
   {
      return new AttributeKey<>(name, type);
   }

   /**
    * The descriptive name of the key.
    *
    * @return the key name
    */
   public String name()
   {
      return name;
   }

   /**
    * The type of the value stored under the key.
    *
    * @return the value type
    */
   public Class<T> type()
   {
      return type;
   }

   @Override
   public String toString()
   {
      return "AttributeKey[" + name + ": " + type.getName() + "]";
   }
}
