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
package io.github.jamoamo.honeycomb.starter.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HoneycombRestProperties}.
 *
 * @author James Amoore
 */
class HoneycombRestPropertiesTest
{
   @Test
   void exposesTheConfiguredValue()
   {
      assertThat(new HoneycombRestProperties(false, 1).enabled()).isFalse();
      assertThat(new HoneycombRestProperties(true, 1).enabled()).isTrue();
      assertThat(new HoneycombRestProperties(true, -20).order()).isEqualTo(-20);
   }

   @Test
   void hasAValueBasedIdentity()
   {
      HoneycombRestProperties properties = new HoneycombRestProperties(true, 1);

      assertThat(properties)
         .isEqualTo(new HoneycombRestProperties(true, 1))
         .hasSameHashCodeAs(new HoneycombRestProperties(true, 1))
         .isNotEqualTo(new HoneycombRestProperties(false, 1))
         .isNotEqualTo(new HoneycombRestProperties(true, 2));
      assertThat(properties.toString()).contains("enabled=true");
      assertThat(properties.toString()).contains("order=1");
   }

   @Test
   void namesThePropertyPrefix()
   {
      assertThat(HoneycombRestProperties.PREFIX).isEqualTo("honeycomb.rest");
   }
}
