/*
 * Copyright Hyperledger Besu Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.hyperledger.besu.ethereum.trie.verkle.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ArgumentConverter;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class SuffixTreeEncoderTest {

  private static class StringToBytes32Converter implements ArgumentConverter {
    @Override
    public Object convert(Object source, ParameterContext context)
        throws ArgumentConversionException {
      if (!(source instanceof CharSequence)) {
        throw new ArgumentConversionException("source must be a String");
      }
      String hexString = (String) source;
      return Bytes32.fromHexString(hexString);
    }
  }

  public static Stream<Arguments> valuesEnd() {
    return Stream.of(
        Arguments.of(Bytes32.ZERO, Bytes32.ZERO),
        Arguments.of(
            Bytes.of(0xff),
            Bytes32.fromHexString(
                "0x00000000000000000000000000000000000000000000000000000000000000FF")),
        Arguments.of(
            Bytes32.leftPad(Bytes.of(0xff)),
            Bytes32.fromHexString(
                "0x00000000000000000000000000000000000000000000000000000000000000FF")),
        Arguments.of(
            Bytes.repeat((byte) 0xff, 12),
            Bytes32.fromHexString(
                "0x0000000000000000000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF")),
        Arguments.of(
            Bytes.fromHexString("0xadef"),
            Bytes32.fromHexString(
                "0x000000000000000000000000000000000000000000000000000000000000ADEF")),
        Arguments.of(
            Bytes.fromHexString("0x1123d3"),
            Bytes32.fromHexString(
                "0x00000000000000000000000000000000000000000000000000000000001123D3")));
  }

  public static Stream<Arguments> valuesMiddle() {
    return Stream.of(
        Arguments.of(Bytes32.ZERO, Bytes32.ZERO),
        Arguments.of(
            Bytes.of(0xff),
            Bytes32.fromHexString(
                "0x000000000000000000000000000000FF00000000000000000000000000000000")),
        Arguments.of(
            Bytes.repeat((byte) 0xff, 12),
            Bytes32.fromHexString(
                "0x00000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF00000000000000000000")),
        Arguments.of(
            Bytes.fromHexString("0xadef"),
            Bytes32.fromHexString(
                "0x000000000000000000000000000000ADEF000000000000000000000000000000")),
        Arguments.of(
            Bytes.fromHexString("0x1123d3"),
            Bytes32.fromHexString(
                "0x00000000000000000000000000001123D3000000000000000000000000000000")));
  }

  public static Stream<Arguments> valuesStart() {
    return Stream.of(
        Arguments.of(Bytes32.ZERO, Bytes32.ZERO),
        Arguments.of(
            Bytes.repeat((byte) 0xff, 12),
            Bytes32.fromHexString(
                "0xFFFFFFFFFFFFFFFFFFFFFFFF0000000000000000000000000000000000000000")),
        Arguments.of(
            Bytes.of(0xff),
            Bytes32.fromHexString(
                "0xFF00000000000000000000000000000000000000000000000000000000000000")),
        Arguments.of(
            Bytes.fromHexString("0xadef"),
            Bytes32.fromHexString(
                "0xADEF000000000000000000000000000000000000000000000000000000000000")),
        Arguments.of(
            Bytes.fromHexString("0x1123d3"),
            Bytes32.fromHexString(
                "0x1123D30000000000000000000000000000000000000000000000000000000000")));
  }

  public static Stream<Arguments> valuesOutOfRange() {
    return Stream.of(
        Arguments.of(Bytes.repeat((byte) 0xff, 12), 25),
        Arguments.of(Bytes.of(0xff), 32),
        Arguments.of(Bytes.fromHexString("0xadef"), 31),
        Arguments.of(Bytes.fromHexString("0x1123d3"), 30));
  }

  @ParameterizedTest
  @MethodSource("valuesStart")
  void encodeBytesStart(final Bytes value, final Bytes32 expected) {
    assertEquals(expected, SuffixTreeEncoder.encodeIntoBasicDataLeaf(value, 0));
  }

  @ParameterizedTest
  @MethodSource("valuesMiddle")
  void encodeBytesMiddle(final Bytes value, final Bytes32 expected) {
    assertEquals(
        expected, SuffixTreeEncoder.encodeIntoBasicDataLeaf(value, (32 - value.size()) / 2));
  }

  @ParameterizedTest
  @MethodSource("valuesEnd")
  void encodeBytesEnd(final Bytes value, final Bytes32 expected) {
    assertEquals(expected, SuffixTreeEncoder.encodeIntoBasicDataLeaf(value, 32 - value.size()));
  }

  @ParameterizedTest
  @MethodSource("valuesOutOfRange")
  void encodeBytesOutsideRange(final Bytes value, final int byteShift) {
    assertThrows(
        IllegalArgumentException.class,
        () -> SuffixTreeEncoder.encodeIntoBasicDataLeaf(value, byteShift));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "0x0000000000000000000000000000000000000000000000000000000000000000",
        "0x0100000000000000000000000000000000000000000000000000000000000000"
      })
  void setVersionInValue(
      @ConvertWith(StringToBytes32Converter.class) final Bytes32 basicDataLeafValue) {
    Bytes version = Bytes.of(10);
    assertEquals(
        Bytes.fromHexString("0x0A00000000000000000000000000000000000000000000000000000000000000"),
        SuffixTreeEncoder.setVersionInValue(basicDataLeafValue, version));
  }

  @Test
  void setVersionInValue_differentSizeThrows() {
    Bytes version = Bytes.ofUnsignedShort(10);
    assertThrows(
        IllegalArgumentException.class,
        () -> SuffixTreeEncoder.setVersionInValue(Bytes32.ZERO, version));
  }

  @Test
  void setVersionInValue_fullLeaf() {
    final Bytes32 basicDataLeafValue =
        Bytes32.fromHexString("0xAB343123BD1231214BC13213EF23434FF213124423247124FF12312EE12AC234");
    Bytes version = Bytes.of(10);
    assertEquals(
        Bytes.fromHexString("0x0A343123BD1231214BC13213EF23434FF213124423247124FF12312EE12AC234"),
        SuffixTreeEncoder.setVersionInValue(basicDataLeafValue, version));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "0x0000000000000000000000000000000000000000000000000000000000000000",
        "0x00000000000000AC000000000000000000000000000000000000000000000000"
      })
  void setCodeSizeInValue(
      @ConvertWith(StringToBytes32Converter.class) final Bytes32 basicDataLeafValue) {
    Bytes codeSize = Bytes.repeat((byte) 0, 3).or(Bytes.of(54));
    assertEquals(
        Bytes.fromHexString("0x0000000000000036000000000000000000000000000000000000000000000000"),
        SuffixTreeEncoder.setCodeSizeInValue(basicDataLeafValue, codeSize));
  }

  @Test
  void setCodeSizeInValue_differentSizeThrows() {
    Bytes codeSize = Bytes.ofUnsignedLong(54);
    assertThrows(
        IllegalArgumentException.class,
        () -> SuffixTreeEncoder.setCodeSizeInValue(Bytes32.ZERO, codeSize));
  }

  @Test
  void setCodeSizeInValue_fullLeaf() {
    final Bytes32 basicDataLeafValue =
        Bytes32.fromHexString("0xAB343123BD1231214BC13213EF23434FF213124423247124FF12312EE12AC234");
    Bytes codeSize = Bytes.repeat((byte) 0, 3).or(Bytes.of(54));
    assertEquals(
        Bytes.fromHexString("0xAB343123BD0000364BC13213EF23434FF213124423247124FF12312EE12AC234"),
        SuffixTreeEncoder.setCodeSizeInValue(basicDataLeafValue, codeSize));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "0x0000000000000000000000000000000000000000000000000000000000000000",
        "0x0000000000000000000000000000000100000000000000000000000000000000"
      })
  void setNonceInValue(
      @ConvertWith(StringToBytes32Converter.class) final Bytes32 basicDataLeafValue) {
    Bytes nonce = Bytes.repeat((byte) 0, 8).or(Bytes.of(20));
    assertEquals(
        Bytes.fromHexString("0x0000000000000000000000000000001400000000000000000000000000000000"),
        SuffixTreeEncoder.setNonceInValue(basicDataLeafValue, nonce));
  }

  @Test
  void setNonceInValue_differentSizeThrows() {
    Bytes nonce = UInt256.valueOf(54);
    assertThrows(
        IllegalArgumentException.class,
        () -> SuffixTreeEncoder.setNonceInValue(Bytes32.ZERO, nonce));
  }

  @Test
  void setNonceInValue_fullLeaf() {
    final Bytes32 basicDataLeafValue =
        Bytes32.fromHexString("0xAB343123BD1231214BC13213EF23434FF213124423247124FF12312EE12AC234");
    Bytes nonce = Bytes.repeat((byte) 0, 8).or(Bytes.of(20));
    assertEquals(
        Bytes.fromHexString("0xAB343123BD1231210000000000000014F213124423247124FF12312EE12AC234"),
        SuffixTreeEncoder.setNonceInValue(basicDataLeafValue, nonce));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "0x0000000000000000000000000000000000000000000000000000000000000000",
        "0x00000000000000000000000000000000000000000000000000000000000000F2"
      })
  void setBalanceInValue(
      @ConvertWith(StringToBytes32Converter.class) final Bytes32 basicDataLeafValue) {
    Bytes balance = Bytes.repeat((byte) 0, 16).or(Bytes.fromHexString("0x3635C9ADC5DEA00000"));
    assertEquals(
        Bytes.fromHexString("0x00000000000000000000000000000000000000000000003635C9ADC5DEA00000"),
        SuffixTreeEncoder.setBalanceInValue(basicDataLeafValue, balance));
  }

  @Test
  void setBalanceInValue_differentSizeThrows() {
    Bytes balance = Bytes.repeat((byte) 0, 32).or(Bytes.fromHexString("0x3635C9ADC5DEA00000"));
    assertThrows(
        IllegalArgumentException.class,
        () -> SuffixTreeEncoder.setBalanceInValue(Bytes32.ZERO, balance));
  }

  @Test
  void setBalanceInValue_fullLeaf() {
    final Bytes32 basicDataLeafValue =
        Bytes32.fromHexString("0xAB343123BD1231214BC13213EF23434FF213124423247124FF12312EE12AC234");
    Bytes balance = Bytes.repeat((byte) 0, 16).or(Bytes.fromHexString("0x3635C9ADC5DEA00000"));
    assertEquals(
        Bytes.fromHexString("0xAB343123BD1231214BC13213EF23434f000000000000003635C9ADC5DEA00000"),
        SuffixTreeEncoder.setBalanceInValue(basicDataLeafValue, balance));
  }
}
