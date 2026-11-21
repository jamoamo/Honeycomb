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
package io.github.jamoamo.honeycomb.rest.intercept;

import io.github.jamoamo.honeycomb.rest.FakeRestRequest;
import io.github.jamoamo.honeycomb.rest.HandlerMethod;
import io.github.jamoamo.honeycomb.rest.RouteMatch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link EndpointInvocation}, {@link Attributes} and {@link AttributeKey}.
 *
 * @author James Amoore
 */
@DisplayName("EndpointInvocation")
public class EndpointInvocationTest
{
   private static final AttributeKey<String> NAME = AttributeKey.of("name", String.class);

   @Target({ElementType.METHOD, ElementType.TYPE})
   @Retention(RetentionPolicy.RUNTIME)
   public @interface Marker
   {
      String value();
   }

   @Marker("class")
   public static final class Annotated
   {
      @Marker("method")
      public void own()
      {
      }

      public void inherited()
      {
      }
   }

   public static final class Plain
   {
      public void none()
      {
      }
   }

   @Test
   @DisplayName("finds an annotation on the handler method first")
   public void testMethodAnnotationWins() throws Exception
   {
      assertThat(invocation(Annotated.class, "own").findAnnotation(Marker.class))
         .map(Marker::value).contains("method");
   }

   @Test
   @DisplayName("falls back to the annotation on the declaring class")
   public void testClassAnnotationFallback() throws Exception
   {
      EndpointInvocation invocation = invocation(Annotated.class, "inherited");

      assertThat(invocation.findAnnotation(Marker.class)).map(Marker::value).contains("class");
      assertThat(invocation.method().getName()).isEqualTo("inherited");
   }

   @Test
   @DisplayName("is empty when neither method nor class is annotated")
   public void testNoAnnotation() throws Exception
   {
      assertThat(invocation(Plain.class, "none").findAnnotation(Marker.class)).isEmpty();
   }

   @Test
   @DisplayName("rejects null components")
   public void testRejectsNulls() throws Exception
   {
      RouteMatch match = invocation(Plain.class, "none").match();
      FakeRestRequest request = new FakeRestRequest("GET", "/");

      assertThatExceptionOfType(NullPointerException.class)
         .isThrownBy(() -> new EndpointInvocation(null, match, new Attributes()));
      assertThatExceptionOfType(NullPointerException.class)
         .isThrownBy(() -> new EndpointInvocation(request, null, new Attributes()));
      assertThatExceptionOfType(NullPointerException.class)
         .isThrownBy(() -> new EndpointInvocation(request, match, null));
   }

   @Test
   @DisplayName("stores, replaces and removes attributes by key")
   public void testAttributes()
   {
      Attributes attributes = new Attributes();
      assertThat(attributes.get(NAME)).isEmpty();

      attributes.put(NAME, "a");
      attributes.put(NAME, "b");
      assertThat(attributes.get(NAME)).contains("b");

      attributes.remove(NAME);
      assertThat(attributes.get(NAME)).isEmpty();
   }

   @Test
   @DisplayName("treats keys with the same name as distinct")
   public void testKeysAreIdentityBased()
   {
      Attributes attributes = new Attributes();
      attributes.put(NAME, "a");

      assertThat(attributes.get(AttributeKey.of("name", String.class))).isEmpty();
      assertThat(NAME.name()).isEqualTo("name");
      assertThat(NAME.type()).isEqualTo(String.class);
      assertThat(NAME).hasToString("AttributeKey[name: java.lang.String]");
   }

   @Test
   @DisplayName("rejects null keys and values")
   public void testAttributesRejectNulls()
   {
      Attributes attributes = new Attributes();

      assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> attributes.get(null));
      assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> attributes.put(null, "a"));
      assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> attributes.put(NAME, null));
      assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> attributes.remove(null));
      assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> AttributeKey.of(null, String.class));
      assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> AttributeKey.of("x", null));
   }

   private static EndpointInvocation invocation(final Class<?> type, final String methodName) throws Exception
   {
      Object bean = type.getDeclaredConstructor().newInstance();
      RouteMatch match = new RouteMatch(new HandlerMethod(bean, type.getMethod(methodName)), Map.of());
      return new EndpointInvocation(new FakeRestRequest("GET", "/"), match, new Attributes());
   }
}
