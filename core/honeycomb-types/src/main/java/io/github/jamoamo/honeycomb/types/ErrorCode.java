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
package io.github.jamoamo.honeycomb.types;

import java.util.Objects;

/**
 * Identifies a failure in a way that an incoming boundary adapter can act on.
 *
 * <p>
 * The {@code code} is the stable, machine readable identifier of the failure and should be treated as part of the
 * contract of the use case that produces it. The {@code category} tells an adapter how to surface the failure without
 * knowing the code. The {@code description} is human readable diagnostic text and must never be relied on
 * programmatically.
 * </p>
 *
 * @author James Amoore
 * @param code        the stable machine readable identifier of the failure, for example {@code USER_NOT_FOUND}
 * @param category    the protocol-neutral classification of the failure
 * @param description human readable text describing the failure
 * @since 1.0.0
 */
public record ErrorCode(String code, ErrorCategory category, String description)
{
   /**
    * Constructor.
    *
    * @param code        the stable machine readable identifier of the failure
    * @param category    the protocol-neutral classification of the failure
    * @param description human readable text describing the failure
    */
   public ErrorCode(final String code, final ErrorCategory category, final String description)
   {
      this.code = requireNonBlank(code);
      this.category = Objects.requireNonNull(category, "category cannot be null");
      this.description = Objects.requireNonNull(description, "description cannot be null");
   }

   private static String requireNonBlank(final String code)
   {
      Objects.requireNonNull(code, "code cannot be null");
      if(code.isBlank())
      {
         throw new IllegalArgumentException("code cannot be blank");
      }
      return code;
   }
}
