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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link DefaultSubjectTemplateParser}.
 *
 * @author James Amoore
 */
@DisplayName("DefaultSubjectTemplateParser")
public class DefaultSubjectTemplateParserTest
{
   private final DefaultSubjectTemplateParser parser = new DefaultSubjectTemplateParser();

   @Test
   @DisplayName("captures the tokens declared by the template")
   public void testCapturesDeclaredTokens()
   {
      SubjectTemplate template = parser.parse("orders.{orderId}.created");

      assertThat(template.match("orders.42.created")).contains(Map.of("orderId", "42"));
      assertThat(template.tokenNames()).containsExactly("orderId");
   }

   @Test
   @DisplayName("matches a template declaring no tokens")
   public void testMatchesStaticTemplate()
   {
      SubjectTemplate template = parser.parse("orders.created");

      assertThat(template.match("orders.created")).contains(Map.of());
      assertThat(template.tokenNames()).isEmpty();
   }

   @Test
   @DisplayName("does not match a subject whose literal tokens differ")
   public void testRejectsDifferingLiteral()
   {
      SubjectTemplate template = parser.parse("orders.{orderId}.created");

      assertThat(template.match("orders.42.cancelled")).isEmpty();
   }

   @Test
   @DisplayName("does not match a subject of a different length")
   public void testRejectsDifferingLength()
   {
      SubjectTemplate template = parser.parse("orders.{orderId}.created");

      assertThat(template.match("orders.42")).isEmpty();
      assertThat(template.match("orders.42.created.late")).isEmpty();
   }

   @Test
   @DisplayName("does not match a null subject")
   public void testRejectsNullSubject()
   {
      assertThat(parser.parse("orders.created").match(null)).isEmpty();
   }

   @Test
   @DisplayName("matches the single-token wildcard without capturing it")
   public void testMatchesSingleWildcard()
   {
      SubjectTemplate template = parser.parse("orders.*.created");

      assertThat(template.match("orders.42.created")).contains(Map.of());
      assertThat(template.match("orders.42.cancelled")).isEmpty();
   }

   @Test
   @DisplayName("matches every remaining token of a template ending in the tail wildcard")
   public void testMatchesTailWildcard()
   {
      SubjectTemplate template = parser.parse("orders.{orderId}.>");

      assertThat(template.match("orders.42.created")).contains(Map.of("orderId", "42"));
      assertThat(template.match("orders.42.line.added")).contains(Map.of("orderId", "42"));
      assertThat(template.match("orders.42")).isEmpty();
   }

   @Test
   @DisplayName("replaces each declared token with the single-token wildcard in the filter subject")
   public void testFilterSubject()
   {
      assertThat(parser.parse("orders.{orderId}.created").filterSubject()).isEqualTo("orders.*.created");
      assertThat(parser.parse("orders.>").filterSubject()).isEqualTo("orders.>");
   }

   @Test
   @DisplayName("rejects an empty template")
   public void testRejectsEmptyTemplate()
   {
      assertThatExceptionOfType(IllegalArgumentException.class)
         .isThrownBy(() -> parser.parse(""))
         .withMessageContaining("may not be empty");
      assertThatExceptionOfType(IllegalArgumentException.class)
         .isThrownBy(() -> parser.parse(null));
   }

   @Test
   @DisplayName("rejects an empty token")
   public void testRejectsEmptyToken()
   {
      assertThatExceptionOfType(IllegalArgumentException.class)
         .isThrownBy(() -> parser.parse("orders..created"))
         .withMessageContaining("Empty token");
   }

   @Test
   @DisplayName("rejects an unnamed token")
   public void testRejectsUnnamedToken()
   {
      assertThatExceptionOfType(IllegalArgumentException.class)
         .isThrownBy(() -> parser.parse("orders.{}.created"))
         .withMessageContaining("Unnamed token");
   }

   @Test
   @DisplayName("rejects a malformed token")
   public void testRejectsMalformedToken()
   {
      assertThatExceptionOfType(IllegalArgumentException.class)
         .isThrownBy(() -> parser.parse("orders.{orderId.created"))
         .withMessageContaining("Malformed token");
   }

   @Test
   @DisplayName("rejects a duplicated token name")
   public void testRejectsDuplicateToken()
   {
      assertThatExceptionOfType(IllegalArgumentException.class)
         .isThrownBy(() -> parser.parse("orders.{orderId}.{orderId}"))
         .withMessageContaining("Duplicate token");
   }

   @Test
   @DisplayName("rejects a tail wildcard that is not the last token")
   public void testRejectsMisplacedTailWildcard()
   {
      assertThatExceptionOfType(IllegalArgumentException.class)
         .isThrownBy(() -> parser.parse("orders.>.created"))
         .withMessageContaining("only appear as the last token");
   }
}
