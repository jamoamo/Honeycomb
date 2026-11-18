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
package io.github.jamoamo.honeycomb.rest.response;

/**
 * The outcome of invoking a rest controller adapter method: either a successful {@link ApiResponse} or a
 * {@link ProblemDetail} describing an error.
 *
 * <p>
 * The set of response types is deliberately closed so that hosting modules can switch over it exhaustively.
 * Hosts should write the response with the HTTP status given by {@link #status()}: {@code 200} for an
 * {@link ApiResponse} and the problem's own status for a {@link ProblemDetail}, using the
 * {@code application/problem+json} media type for the latter.
 * </p>
 *
 * @author James Amoore
 * @since 1.1.0
 */
public sealed interface RestResponse permits ApiResponse, ProblemDetail
{
   /**
    * The HTTP status code the response should be written with.
    *
    * @return the HTTP status code
    */
   int status();
}
