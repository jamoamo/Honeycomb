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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Parses the dot-separated subject syntax common to brokers such as NATS.
 *
 * <p>
 * A template is a sequence of tokens separated by {@code .}. A token is either a literal, the single-token
 * wildcard {@code *}, the trailing multi-token wildcard {@code >}, or a capture of the form {@code {name}}
 * that matches a single token and binds its value to {@code name}.
 * </p>
 *
 * @author James Amoore
 * @since 1.0.0
 */
public final class DefaultSubjectTemplateParser implements SubjectTemplateParser
{
   private static final String SEPARATOR = "\\.";
   private static final String SINGLE_WILDCARD = "*";
   private static final String TAIL_WILDCARD = ">";
   private static final String IN_TEMPLATE = "' in subject template: ";

   /**
    * {@inheritDoc}
    */
   @Override
   public SubjectTemplate parse(final String template)
   {
      if (template == null || template.isEmpty())
      {
         throw new IllegalArgumentException("A subject template may not be empty.");
      }

      String[] tokens = template.split(SEPARATOR, -1);
      List<String> tokenList = List.of(tokens);
      validate(template, tokenList);

      return new CompiledSubjectTemplate(tokenList);
   }

   private static void validate(final String template, final List<String> tokens)
   {
      Set<String> captureNames = new LinkedHashSet<>();
      for (int i = 0; i < tokens.size(); i++)
      {
         String token = tokens.get(i);
         validateToken(template, token);
         if (TAIL_WILDCARD.equals(token) && i != tokens.size() - 1)
         {
            throw new IllegalArgumentException(
               "The '>' wildcard may only appear as the last token of subject template: " + template);
         }
         if (isCapture(token) && !captureNames.add(captureName(token)))
         {
            throw new IllegalArgumentException(
               "Duplicate token '" + captureName(token) + IN_TEMPLATE + template);
         }
      }
   }

   private static void validateToken(final String template, final String token)
   {
      if (token.isEmpty())
      {
         throw new IllegalArgumentException("Empty token in subject template: " + template);
      }
      if (isCapture(token) && captureName(token).isEmpty())
      {
         throw new IllegalArgumentException("Unnamed token in subject template: " + template);
      }
      if (!isCapture(token) && (token.indexOf('{') >= 0 || token.indexOf('}') >= 0))
      {
         throw new IllegalArgumentException("Malformed token '" + token + IN_TEMPLATE + template);
      }
   }

   private static boolean isCapture(final String token)
   {
      return token.length() >= 2 && token.charAt(0) == '{' && token.charAt(token.length() - 1) == '}';
   }

   private static String captureName(final String token)
   {
      return token.substring(1, token.length() - 1);
   }

   /**
    * A {@link SubjectTemplate} compiled from a list of template tokens.
    */
   private static final class CompiledSubjectTemplate implements SubjectTemplate
   {
      private final List<String> tokens;
      private final Set<String> tokenNames;
      private final String filterSubject;

      CompiledSubjectTemplate(final List<String> tokens)
      {
         this.tokens = tokens;
         this.tokenNames = captureNames(tokens);
         this.filterSubject = toFilterSubject(tokens);
      }

      @Override
      public Optional<Map<String, String>> match(final String subject)
      {
         if (subject == null)
         {
            return Optional.empty();
         }

         List<String> subjectTokens = List.of(subject.split(SEPARATOR, -1));
         if (!isMatchableLength(subjectTokens))
         {
            return Optional.empty();
         }

         return captureTokens(subjectTokens);
      }

      @Override
      public Set<String> tokenNames()
      {
         return tokenNames;
      }

      @Override
      public String filterSubject()
      {
         return filterSubject;
      }

      private boolean isMatchableLength(final List<String> subjectTokens)
      {
         if (endsWithTailWildcard())
         {
            return subjectTokens.size() >= tokens.size();
         }

         return subjectTokens.size() == tokens.size();
      }

      private boolean endsWithTailWildcard()
      {
         return TAIL_WILDCARD.equals(tokens.get(tokens.size() - 1));
      }

      private Optional<Map<String, String>> captureTokens(final List<String> subjectTokens)
      {
         Map<String, String> captured = new LinkedHashMap<>();
         for (int i = 0; i < tokens.size(); i++)
         {
            String token = tokens.get(i);
            if (TAIL_WILDCARD.equals(token))
            {
               break;
            }
            if (isCapture(token))
            {
               captured.put(captureName(token), subjectTokens.get(i));
            }
            else if (!SINGLE_WILDCARD.equals(token) && !token.equals(subjectTokens.get(i)))
            {
               return Optional.empty();
            }
         }

         return Optional.of(Map.copyOf(captured));
      }

      private static Set<String> captureNames(final List<String> tokens)
      {
         Set<String> names = new LinkedHashSet<>();
         for (String token : tokens)
         {
            if (isCapture(token))
            {
               names.add(captureName(token));
            }
         }

         return Set.copyOf(names);
      }

      private static String toFilterSubject(final List<String> tokens)
      {
         List<String> filterTokens = new ArrayList<>(tokens.size());
         for (String token : tokens)
         {
            filterTokens.add(isCapture(token) ? SINGLE_WILDCARD : token);
         }

         return String.join(".", filterTokens);
      }
   }
}
