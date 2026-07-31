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
package io.github.jamoamo.honeycomb.adapter.argument;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link DefaultValueConverter}.
 *
 * @author James Amoore
 */
@DisplayName("DefaultValueConverter")
public class DefaultValueConverterTest
{
   private enum Colour
   {
      RED,
      GREEN
   }

   private final DefaultValueConverter converter = new DefaultValueConverter();

   @Test
   @DisplayName("converts the supported scalar types")
   public void testConvertsScalars()
   {
      assertThat(converter.convert("text", String.class)).isEqualTo("text");
      assertThat(converter.convert("42", int.class)).isEqualTo(42);
      assertThat(converter.convert("42", Integer.class)).isEqualTo(42);
      assertThat(converter.convert("42", long.class)).isEqualTo(42L);
      assertThat(converter.convert("42", Long.class)).isEqualTo(42L);
      assertThat(converter.convert("42", short.class)).isEqualTo((short) 42);
      assertThat(converter.convert("42", Short.class)).isEqualTo((short) 42);
      assertThat(converter.convert("4.2", double.class)).isEqualTo(4.2d);
      assertThat(converter.convert("4.2", Double.class)).isEqualTo(4.2d);
      assertThat(converter.convert("4.2", float.class)).isEqualTo(4.2f);
      assertThat(converter.convert("4.2", Float.class)).isEqualTo(4.2f);
   }

   @Test
   @DisplayName("converts booleans case-insensitively")
   public void testConvertsBooleans()
   {
      assertThat(converter.convert("true", boolean.class)).isEqualTo(Boolean.TRUE);
      assertThat(converter.convert("FALSE", Boolean.class)).isEqualTo(Boolean.FALSE);
   }

   @Test
   @DisplayName("rejects a boolean value that is neither true nor false")
   public void testRejectsInvalidBoolean()
   {
      assertThatExceptionOfType(ValueConversionException.class)
         .isThrownBy(() -> converter.convert("yes", boolean.class));
   }

   @Test
   @DisplayName("converts UUIDs")
   public void testConvertsUuid()
   {
      UUID uuid = UUID.randomUUID();

      assertThat(converter.convert(uuid.toString(), UUID.class)).isEqualTo(uuid);
   }

   @Test
   @DisplayName("converts enums by constant name")
   public void testConvertsEnums()
   {
      assertThat(converter.convert("RED", Colour.class)).isEqualTo(Colour.RED);
   }

   @Test
   @DisplayName("rejects an unknown enum constant")
   public void testRejectsUnknownEnumConstant()
   {
      assertThatExceptionOfType(ValueConversionException.class)
         .isThrownBy(() -> converter.convert("BLUE", Colour.class))
         .withMessageContaining("Invalid value");
   }

   @Test
   @DisplayName("rejects an unparseable number")
   public void testRejectsUnparseableNumber()
   {
      assertThatExceptionOfType(ValueConversionException.class)
         .isThrownBy(() -> converter.convert("forty-two", int.class))
         .withMessageContaining("Invalid value");
   }

   @Test
   @DisplayName("rejects unsupported target types")
   public void testRejectsUnsupportedType()
   {
      assertThatExceptionOfType(ValueConversionException.class)
         .isThrownBy(() -> converter.convert("anything", Thread.class))
         .withMessageContaining("Unsupported parameter type");
   }
}
