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

import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;

import java.util.Map;
import java.util.Optional;

/**
 * A {@link PathTemplate} backed by a Spring {@link PathPattern}.
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class PathPatternTemplate implements PathTemplate
{
   private final PathPattern pattern;
   private final boolean staticTemplate;

   /**
    * Constructor.
    *
    * @param pattern the compiled Spring path pattern
    */
   public PathPatternTemplate(final PathPattern pattern)
   {
      this.pattern = pattern;
      this.staticTemplate = computeStatic(pattern.getPatternString());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Optional<Map<String, String>> match(final String requestPath)
   {
      PathContainer pathContainer = PathContainer.parsePath(requestPath);
      PathPattern.PathMatchInfo matchInfo = pattern.matchAndExtract(pathContainer);
      if (matchInfo == null)
      {
         return Optional.empty();
      }

      return Optional.of(matchInfo.getUriVariables());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isStatic()
   {
      return staticTemplate;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int compareTo(final PathTemplate other)
   {
      if (!(other instanceof PathPatternTemplate otherTemplate))
      {
         return 0;
      }

      int bySpecificity = pattern.compareTo(otherTemplate.pattern);
      if (bySpecificity != 0)
      {
         return bySpecificity;
      }

      return pattern.getPatternString().compareTo(otherTemplate.pattern.getPatternString());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean equals(final Object other)
   {
      return other instanceof PathPatternTemplate template
         && pattern.getPatternString().equals(template.pattern.getPatternString());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int hashCode()
   {
      return pattern.getPatternString().hashCode();
   }

   private static boolean computeStatic(final String patternString)
   {
      return patternString.indexOf('{') < 0
         && patternString.indexOf('*') < 0
         && patternString.indexOf('?') < 0;
   }
}
