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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A simple segment-based {@link PathTemplateParser} for tests.
 *
 * @author James Amoore
 */
public final class FakePathTemplates
{
   public static final PathTemplateParser PARSER = FakeTemplate::new;

   private FakePathTemplates()
   {
   }

   private static final class FakeTemplate implements PathTemplate
   {
      private final String template;
      private final String[] segments;

      private FakeTemplate(final String template)
      {
         this.template = template;
         this.segments = template.split("/");
      }

      @Override
      public Optional<Map<String, String>> match(final String requestPath)
      {
         String[] pathSegments = requestPath.split("/");
         if (pathSegments.length != segments.length)
         {
            return Optional.empty();
         }

         Map<String, String> variables = new HashMap<>();
         for (int i = 0; i < segments.length; i++)
         {
            if (segments[i].startsWith("{"))
            {
               variables.put(segments[i].substring(1, segments[i].length() - 1), pathSegments[i]);
            }
            else if (!segments[i].equals(pathSegments[i]))
            {
               return Optional.empty();
            }
         }

         return Optional.of(variables);
      }

      @Override
      public boolean isStatic()
      {
         return !template.contains("{");
      }

      @Override
      public int compareTo(final PathTemplate other)
      {
         FakeTemplate otherTemplate = (FakeTemplate) other;
         int byVariables = Integer.compare(variableCount(), otherTemplate.variableCount());
         if (byVariables != 0)
         {
            return byVariables;
         }

         return template.compareTo(otherTemplate.template);
      }

      private int variableCount()
      {
         int count = 0;
         for (String segment : segments)
         {
            if (segment.startsWith("{"))
            {
               count++;
            }
         }

         return count;
      }
   }
}
