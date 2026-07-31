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
package io.github.jamoamo.honeycomb.rest.spring;

import io.github.jamoamo.honeycomb.rest.PathTemplate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PathPatternTemplate} and {@link PathPatternTemplateParser}.
 *
 * @author James Amoore
 */
@DisplayName("PathPatternTemplate")
public class PathPatternTemplateTest
{
   private final PathPatternTemplateParser parser = new PathPatternTemplateParser();

   @Test
   @DisplayName("matches a concrete path and captures its variables")
   public void testMatchCapturesVariables()
   {
      PathTemplate template = parser.parse("/users/{id}");

      assertThat(template.match("/users/42")).contains(Map.of("id", "42"));
   }

   @Test
   @DisplayName("does not match a different path")
   public void testNoMatch()
   {
      PathTemplate template = parser.parse("/users/{id}");

      assertThat(template.match("/games/42")).isEmpty();
   }

   @Test
   @DisplayName("reports whether the template is static")
   public void testIsStatic()
   {
      assertThat(parser.parse("/users").isStatic()).isTrue();
      assertThat(parser.parse("/users/{id}").isStatic()).isFalse();
      assertThat(parser.parse("/users/*").isStatic()).isFalse();
   }

   @Test
   @DisplayName("orders more specific templates before less specific ones")
   public void testSpecificityOrdering()
   {
      PathTemplate literal = parser.parse("/users/me/games");
      PathTemplate templated = parser.parse("/users/{id}/{section}");

      assertThat(literal.compareTo(templated)).isNegative();
      assertThat(templated.compareTo(literal)).isPositive();
   }

   @Test
   @DisplayName("falls back to the pattern string for equally specific templates")
   public void testTieBreakByPatternString()
   {
      PathTemplate first = parser.parse("/a/{x}");
      PathTemplate second = parser.parse("/b/{x}");

      assertThat(first.compareTo(second)).isNegative();
      assertThat(first.compareTo(parser.parse("/a/{x}"))).isZero();
   }

   @Test
   @DisplayName("treats a foreign template type as equally specific")
   public void testForeignTemplateType()
   {
      PathTemplate foreign = new PathTemplate()
      {
         @Override
         public Optional<Map<String, String>> match(final String requestPath)
         {
            return Optional.empty();
         }

         @Override
         public boolean isStatic()
         {
            return true;
         }

         @Override
         public int compareTo(final PathTemplate other)
         {
            return 0;
         }
      };

      assertThat(parser.parse("/users").compareTo(foreign)).isZero();
   }

   @Test
   @DisplayName("bases equality on the pattern string")
   public void testEqualsAndHashCode()
   {
      PathTemplate template = parser.parse("/users/{id}");
      PathTemplate same = parser.parse("/users/{id}");
      PathTemplate different = parser.parse("/games/{id}");

      assertThat(template).isEqualTo(same).hasSameHashCodeAs(same);
      assertThat(template).isNotEqualTo(different);
      assertThat(template).isNotEqualTo("not a template");
   }
}
