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
package io.github.jamoamo.honeycomb.rest.argument;

/**
 * Converts the raw string value of a request parameter into the type expected by a handler method.
 *
 * <p>
 * This is the integration seam for value conversion: {@code honeycomb-rest} ships a lightweight default
 * implementation, and a richer implementation (for example one backed by Spring's conversion service) may be
 * supplied by the hosting module.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
@FunctionalInterface
public interface ValueConverter
{
   /**
    * Converts the given raw value to the target type.
    *
    * @param value      the raw string value
    * @param targetType the type to convert to
    * @return the converted value
    * @throws io.github.jamoamo.honeycomb.rest.BadRequestException if the value cannot be converted
    */
   Object convert(String value, Class<?> targetType);
}
