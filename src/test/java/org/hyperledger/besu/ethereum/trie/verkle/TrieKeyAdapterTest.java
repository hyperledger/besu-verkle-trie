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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class TrieKeyAdapterTest {
  Bytes address = Bytes.fromHexString("0x00112233445566778899aabbccddeeff00112233");
  TrieKeyAdapter adapter = new TrieKeyAdapter(new SHA256Hasher());

  @Test
  public void testStorageKey() {
    UInt256 storageKey = UInt256.valueOf(32);
    // Need to change this once commit is fixed
    Bytes32 expected =
        Bytes32.fromHexString("0x1719aec0fd8358bc50c95799bd3cd38da48f6519f78d64ccb2546f554d80a060");
    assertThat(adapter.storageKey(address, storageKey)).isEqualTo(expected);
  }

  @Test
  public void testCodeChunkKey() {
    UInt256 chunkId = UInt256.valueOf(24);
    // Need to change this once commit is fixed
    Bytes32 expected =
        Bytes32.fromHexString("0x1719aec0fd8358bc50c95799bd3cd38da48f6519f78d64ccb2546f554d80a098");
    assertThat(adapter.codeChunkKey(address, chunkId)).isEqualTo(expected);
  }

  @Test
  public void testVersionKey() {
    // Need to change this once commit is fixed
    Bytes32 expected =
        Bytes32.fromHexString("0x1719aec0fd8358bc50c95799bd3cd38da48f6519f78d64ccb2546f554d80a000");
    assertThat(adapter.versionKey(address)).isEqualTo(expected);
  }

  @Test
  public void testBalanceKey() {
    // Need to change this once commit is fixed
    Bytes32 expected =
        Bytes32.fromHexString("0x1719aec0fd8358bc50c95799bd3cd38da48f6519f78d64ccb2546f554d80a001");
    assertThat(adapter.balanceKey(address)).isEqualTo(expected);
  }

  @Test
  public void testNonceKey() {
    // Need to change this once commit is fixed
    Bytes32 expected =
        Bytes32.fromHexString("0x1719aec0fd8358bc50c95799bd3cd38da48f6519f78d64ccb2546f554d80a002");
    assertThat(adapter.nonceKey(address)).isEqualTo(expected);
  }

  @Test
  public void testCodeKeccakKey() {
    // Need to change this once commit is fixed
    Bytes32 expected =
        Bytes32.fromHexString("0x1719aec0fd8358bc50c95799bd3cd38da48f6519f78d64ccb2546f554d80a003");
    assertThat(adapter.codeKeccakKey(address)).isEqualTo(expected);
  }

  @Test
  public void testCodeSizeKey() {
    // Need to change this once commit is fixed
    Bytes32 expected =
        Bytes32.fromHexString("0x1719aec0fd8358bc50c95799bd3cd38da48f6519f78d64ccb2546f554d80a004");
    assertThat(adapter.codeSizeKey(address)).isEqualTo(expected);
  }

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static List<TestData> JsonData() throws IOException {
    InputStream inputStream = TrieKeyAdapterTest.class.getResourceAsStream("/erc20CodeChunks.json");
    return objectMapper.readValue(inputStream, new TypeReference<List<TestData>>() {});
  }

  static class TestData {
    public String bytecode;
    public ArrayList<String> chunks;
  }

  @ParameterizedTest
  @MethodSource("JsonData")
  public void TestChunkifyCode(TestData testData) {
    Bytes bytecode = Bytes.fromHexString(testData.bytecode);
    List<Bytes32> result = adapter.chunkifyCode(bytecode);
    assertThat(testData.chunks.size()).isEqualTo(result.size());
    Bytes32 value;
    // @SuppressWarnings("ModifiedButNotUsed")
    // List<Bytes32> expected = new ArrayList<Bytes32>(testData.chunks.size());
    for (int i = 0; i < testData.chunks.size(); ++i) {
      value = Bytes32.fromHexString(testData.chunks.get(i));
      assertThat(value).isEqualByComparingTo(result.get(i));
      // expected.add(value);
    }
  }
}
