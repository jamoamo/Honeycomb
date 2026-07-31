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
package io.github.jamoamo.honeycomb.rest;

import java.util.Map;
import java.util.Optional;

/**
 * A compiled path template that can be matched against a concrete request path.
 *
 * <p>
 * Implementations are produced by a {@link PathTemplateParser} and are expected to be parsed once and matched
 * many times. Matching either fails or yields the values captured for any variables declared in the template
 * (for example {@code {id}} in {@code /users/{id}}).
 * </p>
 *
 * <p>
 * The natural ordering is by decreasing specificity: a more specific template (for example one with a literal
 * segment) sorts before a less specific one (for example a template capturing that segment as a variable). This
 * lets a matcher holding several candidate templates prefer the most specific match.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public interface PathTemplate extends Comparable<PathTemplate>
{
   /**
    * Matches the template against a concrete request path.
    *
    * @param requestPath the request path to match
    * @return the captured path variables if the path matches, otherwise {@link Optional#empty()}
    */
   Optional<Map<String, String>> match(String requestPath);

   /**
    * Indicates whether the template is static, i.e. it declares no variables or wildcards and therefore only
    * ever matches a single, literal path.
    *
    * @return {@code true} if the template is static
    */
   boolean isStatic();
}
