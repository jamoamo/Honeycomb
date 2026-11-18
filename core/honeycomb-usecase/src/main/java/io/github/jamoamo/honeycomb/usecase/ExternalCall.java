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
package io.github.jamoamo.honeycomb.usecase;

/**
 * A call from a use case to an external system, such as a database, message queue or 3rd party service.
 *
 * <p>
 * Unlike {@link java.util.function.Function}, the call may throw checked exceptions. {@link UseCase} converts any
 * exception it throws into a {@link UseCaseException}.
 * </p>
 *
 * @author James Amoore
 * @param <P> the input type of the call
 * @param <R> the result type of the call
 *
 * @since 1.0.1
 */
@FunctionalInterface
public interface ExternalCall<P, R>
{
   /**
    * Makes the call.
    *
    * @param input the input to the call
    *
    * @return the result of the call
    *
    * @throws Exception if the call fails
    */
   @SuppressWarnings("checkstyle:IllegalThrows")
   R apply(P input) throws Exception;
}
