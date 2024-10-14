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

import static org.hyperledger.besu.ethereum.trie.verkle.util.SuffixTreeDescriptor.BALANCE_BYTE_SIZE;
import static org.hyperledger.besu.ethereum.trie.verkle.util.SuffixTreeDescriptor.BALANCE_OFFSET;
import static org.hyperledger.besu.ethereum.trie.verkle.util.SuffixTreeDescriptor.CODE_SIZE_BYTE_SIZE;
import static org.hyperledger.besu.ethereum.trie.verkle.util.SuffixTreeDescriptor.CODE_SIZE_OFFSET;
import static org.hyperledger.besu.ethereum.trie.verkle.util.SuffixTreeDescriptor.NONCE_BYTE_SIZE;
import static org.hyperledger.besu.ethereum.trie.verkle.util.SuffixTreeDescriptor.NONCE_OFFSET;
import static org.hyperledger.besu.ethereum.trie.verkle.util.SuffixTreeDescriptor.VERSION_BYTE_SIZE;
import static org.hyperledger.besu.ethereum.trie.verkle.util.SuffixTreeDescriptor.VERSION_OFFSET;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.bytes.MutableBytes32;

public class SuffixTreeDecoder {

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
    final MutableBytes32 value = MutableBytes32.create();
    value.set(offset, Bytes.repeat((byte) 0xff, size));
    return value;
  }

  public static Bytes[] decodeBasicDataLeaf(final Bytes32 value) {
    Bytes[] decodedFields = new Bytes[4];

    decodedFields[0] =
        extractField(value, VERSION_OFFSET, VERSION_VALUE_MASK).slice(0, VERSION_BYTE_SIZE);
    decodedFields[1] =
        extractField(value, CODE_SIZE_OFFSET, CODE_SIZE_VALUE_MASK).slice(0, CODE_SIZE_BYTE_SIZE);
    decodedFields[2] =
        extractField(value, NONCE_OFFSET, NONCE_VALUE_MASK).slice(0, NONCE_BYTE_SIZE);
    decodedFields[3] =
        extractField(value, BALANCE_OFFSET, BALANCE_VALUE_MASK).slice(0, BALANCE_BYTE_SIZE);

    return decodedFields;
  }

  private static Bytes extractField(final Bytes32 value, int offset, Bytes32 mask) {
    return value.and(mask).shiftLeft(offset * 8);
  }
}
