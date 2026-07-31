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

import java.io.IOException;
import java.lang.reflect.Parameter;

/**
 * Resolves the value for a single handler-method parameter from the request.
 *
 * <p>
 * Resolvers are consulted in order; the first whose {@link #supports(Parameter)} returns {@code true} resolves
 * the parameter. This is the extension point through which new sources of request context (path variables,
 * query parameters, and so on) are added without modifying the invoker.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public interface AdapterArgumentResolver
{
   /**
    * Indicates whether this resolver can resolve the given parameter.
    *
    * @param parameter the handler-method parameter
    * @return {@code true} if this resolver handles the parameter
    */
   boolean supports(Parameter parameter);

   /**
    * Resolves the value for the given parameter.
    *
    * @param parameter the handler-method parameter
    * @param context   the resolution context for the current request
    * @return the resolved value
    * @throws IOException if the request cannot be read
    */
   Object resolve(Parameter parameter, ArgumentResolutionContext context) throws IOException;
}
