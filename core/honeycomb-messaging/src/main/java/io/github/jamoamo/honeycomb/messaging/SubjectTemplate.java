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
package io.github.jamoamo.honeycomb.messaging;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A compiled subject template that can be matched against the concrete subject of a message.
 *
 * <p>
 * Implementations are produced by a {@link SubjectTemplateParser} and are expected to be parsed once and
 * matched many times. Matching either fails or yields the values captured for any tokens declared in the
 * template (for example {@code {orderId}} in {@code orders.{orderId}.created}).
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public interface SubjectTemplate
{
   /**
    * Matches the template against the concrete subject of a message.
    *
    * @param subject the subject to match
    * @return the captured tokens if the subject matches, otherwise {@link Optional#empty()}
    */
   Optional<Map<String, String>> match(String subject);

   /**
    * The names of the tokens declared by the template.
    *
    * @return the declared token names, or an empty set when the template declares none
    */
   Set<String> tokenNames();

   /**
    * The template expressed as a subject a broker understands, with each declared token replaced by the
    * single-token wildcard. For example {@code orders.{orderId}.created} yields {@code orders.*.created}.
    *
    * <p>
    * This is what a consumer filters on: the broker delivers everything matching the wildcard subject, and
    * {@link #match(String)} then recovers the token values from each delivered subject.
    * </p>
    *
    * @return the filter subject
    */
   String filterSubject();
}
