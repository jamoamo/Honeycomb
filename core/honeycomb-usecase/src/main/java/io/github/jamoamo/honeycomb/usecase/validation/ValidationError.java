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
package io.github.jamoamo.honeycomb.usecase.validation;

import java.util.Objects;

/**
 * A single problem found while validating the input to a use case.
 *
 * <p>
 * The {@code field} names the part of the input at fault, using the naming of the use case input rather than of any
 * transport representation. An incoming boundary adapter is responsible for translating it if the shape it accepted
 * differs from the use case input.
 * </p>
 *
 * @author James Amoore
 * @param field   the name of the input field at fault
 * @param message human readable text describing what is wrong with the field
 * @since 1.0.0
 */
public record ValidationError(String field, String message)
{
   /**
    * Constructor.
    *
    * @param field   the name of the input field at fault
    * @param message human readable text describing what is wrong with the field
    */
   public ValidationError(final String field, final String message)
   {
      this.field = Objects.requireNonNull(field, "field cannot be null");
      this.message = Objects.requireNonNull(message, "message cannot be null");
   }
}
