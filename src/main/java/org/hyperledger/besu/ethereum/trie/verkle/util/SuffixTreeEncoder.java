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

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class SuffixTreeEncoder {
  private static final int VERSION_OFFSET = 0;
  private static final int VERSION_BYTE_SIZE = 1;
  private static final int CODE_SIZE_OFFSET = 5;
  private static final int CODE_SIZE_BYTE_SIZE = 3;
  private static final int NONCE_OFFSET = 8;
  private static final int NONCE_BYTE_SIZE = 8;
  private static final int BALANCE_OFFSET = 16;
  private static final int BALANCE_BYTE_SIZE = 16;

  private static final Bytes32 VERSION_VALUE_MASK;
  private static final Bytes32 CODE_SIZE_VALUE_MASK;
  private static final Bytes32 NONCE_VALUE_MASK;
  private static final Bytes32 BALANCE_VALUE_MASK;

  static {
    VERSION_VALUE_MASK = createMask(VERSION_OFFSET, VERSION_BYTE_SIZE);
    CODE_SIZE_VALUE_MASK = createMask(CODE_SIZE_OFFSET, CODE_SIZE_BYTE_SIZE);
    NONCE_VALUE_MASK = createMask(NONCE_OFFSET, NONCE_BYTE_SIZE);
    BALANCE_VALUE_MASK = createMask(BALANCE_OFFSET, BALANCE_BYTE_SIZE);
  }

  private static Bytes32 createMask(final int offset, final int size) {
    Bytes value = Bytes.repeat((byte) 0xff, size);
    return encodeIntoBasicDataLeaf(value, offset).not();
  }

  public static Bytes32 eraseVersion(final Bytes32 value) {
    return value.and(VERSION_VALUE_MASK);
  }

  private static Bytes32 eraseCodeSize(final Bytes32 value) {
    return value.and(CODE_SIZE_VALUE_MASK);
  }

  private static Bytes32 eraseNonce(final Bytes32 value) {
    return value.and(NONCE_VALUE_MASK);
  }

  private static Bytes32 eraseBalance(final Bytes32 value) {
    return value.and(BALANCE_VALUE_MASK);
  }

  /**
   * Sets the new version and encodes it inside `value`.
   *
   * @param value on which to set the version part
   * @param version value for the new version
   * @return updated value with the new version set
   */
  public static Bytes32 setVersionInValue(Bytes32 value, final Bytes version) {
    checkSize(version, VERSION_BYTE_SIZE);
    value = eraseVersion(value);
    return value.or(encodeIntoBasicDataLeaf(version, VERSION_OFFSET));
  }

  /**
   * Sets the new code size and encodes it inside `value`.
   *
   * @param value on which to set the code size part
   * @param codeSize value for the new code size
   * @return updated value with the new code size set
   */
  public static Bytes32 setCodeSizeInValue(Bytes32 value, final Bytes codeSize) {
    checkSize(codeSize, CODE_SIZE_BYTE_SIZE);
    value = eraseCodeSize(value);
    return value.or(encodeIntoBasicDataLeaf(codeSize, CODE_SIZE_OFFSET));
  }

  /**
   * Sets the new nonce and encodes it inside `value`.
   *
   * @param value on which to set the nonce part
   * @param nonce value for the new nonce
   * @return updated value with the new nonce set
   */
  public static Bytes32 setNonceInValue(Bytes32 value, final Bytes nonce) {
    checkSize(nonce, NONCE_BYTE_SIZE);
    value = eraseNonce(value);
    return value.or(encodeIntoBasicDataLeaf(nonce, NONCE_OFFSET));
  }

  /**
   * Sets the new balance and encodes it inside `value`.
   *
   * @param value on which to set the balance part
   * @param balance value for the new balance
   * @return updated value with the new balance set
   */
  public static Bytes32 setBalanceInValue(Bytes32 value, final Bytes balance) {
    checkSize(balance, BALANCE_BYTE_SIZE);
    value = eraseBalance(value);
    return value.or(encodeIntoBasicDataLeaf(balance, BALANCE_OFFSET));
  }

  private static void checkSize(final Bytes value, final int requiredSize) {
    if (value.size() != requiredSize) {
      throw new IllegalArgumentException("value should have size=" + requiredSize);
    }
  }

  /**
   * Encoding of a field into the BasicDataLeaf 32 byte value, using Little-Endian order.
   *
   * @param value to encode into a 32 byte value
   * @param byteShift byte position of `value` within the final 32 byte value
   * @throws IllegalArgumentException if `value` does not fit within 32 bytes after being encoded
   * @return encoded BasicDataLeaf value
   */
  static Bytes32 encodeIntoBasicDataLeaf(final Bytes value, final int byteShift) {
    Bytes32 value32Bytes = Bytes32.rightPad(value);
    if (byteShift == 0) {
      return value32Bytes;
    } else if (byteShift < 0) {
      throw new IllegalArgumentException(
          "invalid byteShift " + byteShift + " must be greater than zero");
    } else if (value32Bytes.numberOfTrailingZeroBytes() < byteShift) {
      int valueSizeBytes = 32 - value32Bytes.numberOfTrailingZeroBytes() + byteShift;
      throw new IllegalArgumentException(
          "value must be 32 bytes but becomes "
              + valueSizeBytes
              + " bytes with byteShift "
              + byteShift);
    }
    return value32Bytes.shiftRight(byteShift * 8);
  }
}
