/*
 * Copyright Besu Contributors
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
package org.hyperledger.besu.ethereum.trie.verkle;

import static org.assertj.core.api.Assertions.assertThat;

import org.hyperledger.besu.ethereum.trie.verkle.adapter.TrieKeyAdapter;
import org.hyperledger.besu.ethereum.trie.verkle.hasher.SHA256Hasher;

import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.junit.jupiter.api.Test;

public class TrieKeyAdapterTest {
  Bytes32 address =
      Bytes32.fromHexString("0x000000000000000000000000112233445566778899aabbccddeeff00112233");
  TrieKeyAdapter adapter = new TrieKeyAdapter(new SHA256Hasher());

  @Test
  public void testStorageKey() {
    UInt256 storageKey = UInt256.valueOf(32);
    // Need to change this once commit is fixed
    Bytes32 expected =
        Bytes32.fromHexString("0xc3552556138109254d3fb498d2364e85ed427986e389dff3fa5a514e2a3e5460");
    assertThat(adapter.storageKey(address, storageKey)).isEqualTo(expected);
  }

  @Test
  public void testCodeChunkKey() {
    UInt256 chunkId = UInt256.valueOf(24);
    // Need to change this once commit is fixed
    Bytes32 expected =
        Bytes32.fromHexString("0xc3552556138109254d3fb498d2364e85ed427986e389dff3fa5a514e2a3e5498");
    assertThat(adapter.codeChunkKey(address, chunkId)).isEqualTo(expected);
  }

  @Test
  public void testVersionKey() {
    // Need to change this once commit is fixed
    Bytes32 expected =
        Bytes32.fromHexString("0xc3552556138109254d3fb498d2364e85ed427986e389dff3fa5a514e2a3e5400");
    assertThat(adapter.versionKey(address)).isEqualTo(expected);
  }

  @Test
  public void testBalanceKey() {
    // Need to change this once commit is fixed
    Bytes32 expected =
        Bytes32.fromHexString("0xc3552556138109254d3fb498d2364e85ed427986e389dff3fa5a514e2a3e5401");
    assertThat(adapter.balanceKey(address)).isEqualTo(expected);
  }

  @Test
  public void testNonceKey() {
    // Need to change this once commit is fixed
    Bytes32 expected =
        Bytes32.fromHexString("0xc3552556138109254d3fb498d2364e85ed427986e389dff3fa5a514e2a3e5402");
    assertThat(adapter.nonceKey(address)).isEqualTo(expected);
  }

  @Test
  public void testCodeKeccakKey() {
    // Need to change this once commit is fixed
    Bytes32 expected =
        Bytes32.fromHexString("0xc3552556138109254d3fb498d2364e85ed427986e389dff3fa5a514e2a3e5403");
    assertThat(adapter.codeKeccakKey(address)).isEqualTo(expected);
  }

  @Test
  public void testCodeSizeKey() {
    // Need to change this once commit is fixed
    Bytes32 expected =
        Bytes32.fromHexString("0xc3552556138109254d3fb498d2364e85ed427986e389dff3fa5a514e2a3e5404");
    assertThat(adapter.codeSizeKey(address)).isEqualTo(expected);
  }
}
