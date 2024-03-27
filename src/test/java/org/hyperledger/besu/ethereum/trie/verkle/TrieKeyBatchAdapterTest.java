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
package org.hyperledger.besu.ethereum.trie.verkle;

import static org.assertj.core.api.Assertions.assertThat;

import org.hyperledger.besu.ethereum.trie.verkle.adapter.TrieKeyBatchAdapter;
import org.hyperledger.besu.ethereum.trie.verkle.hasher.PedersenHasher;
import org.hyperledger.besu.ethereum.trie.verkle.util.Parameters;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class TrieKeyBatchAdapterTest {
  Bytes address = Bytes.fromHexString("0x00112233445566778899aabbccddeeff00112233");
  TrieKeyBatchAdapter adapter = new TrieKeyBatchAdapter(new PedersenHasher());

  @Test
  public void testAccountKeys() {
    final List<Bytes32> expectedKeys = new ArrayList<>();
    expectedKeys.add(Parameters.VERSION_LEAF_KEY);
    expectedKeys.add(Parameters.BALANCE_LEAF_KEY);
    expectedKeys.add(Parameters.NONCE_LEAF_KEY);
    expectedKeys.add(Parameters.CODE_KECCAK_LEAF_KEY);
    expectedKeys.add(Parameters.CODE_SIZE_LEAF_KEY);
    final List<Bytes32> expectedValues =
        List.of(
            Bytes32.fromHexString(
                "0x46b95e4e504b92d984c91d6f17eba4b60b904fb370818f0b6e74bc3ae5034400"),
            Bytes32.fromHexString(
                "0x46b95e4e504b92d984c91d6f17eba4b60b904fb370818f0b6e74bc3ae5034401"),
            Bytes32.fromHexString(
                "0x46b95e4e504b92d984c91d6f17eba4b60b904fb370818f0b6e74bc3ae5034402"),
            Bytes32.fromHexString(
                "0x46b95e4e504b92d984c91d6f17eba4b60b904fb370818f0b6e74bc3ae5034403"),
            Bytes32.fromHexString(
                "0x46b95e4e504b92d984c91d6f17eba4b60b904fb370818f0b6e74bc3ae5034404"));
    final Map<Bytes32, Bytes32> map =
        IntStream.range(0, expectedKeys.size())
            .boxed()
            .collect(Collectors.toMap(expectedKeys::get, expectedValues::get));
    final Map<Bytes32, Bytes32> generatedHashes =
        adapter.manyAccountTrieKeys(address, expectedKeys, new ArrayList<>(), new ArrayList<>());
    assertThat(generatedHashes).containsExactlyEntriesOf(map);
  }

  @Test
  public void testAccountKeysWithStorage() {
    final List<Bytes32> expectedKeys = new ArrayList<>();
    expectedKeys.add(Parameters.VERSION_LEAF_KEY);
    expectedKeys.add(Parameters.BALANCE_LEAF_KEY);
    expectedKeys.add(Parameters.NONCE_LEAF_KEY);
    expectedKeys.add(Parameters.CODE_KECCAK_LEAF_KEY);
    expectedKeys.add(Parameters.CODE_SIZE_LEAF_KEY);

    final UInt256 storage = UInt256.valueOf(64);
    expectedKeys.add(storage);

    final List<Bytes32> expectedValues =
        List.of(
            Bytes32.fromHexString(
                "0x46b95e4e504b92d984c91d6f17eba4b60b904fb370818f0b6e74bc3ae5034400"),
            Bytes32.fromHexString(
                "0x46b95e4e504b92d984c91d6f17eba4b60b904fb370818f0b6e74bc3ae5034401"),
            Bytes32.fromHexString(
                "0x46b95e4e504b92d984c91d6f17eba4b60b904fb370818f0b6e74bc3ae5034402"),
            Bytes32.fromHexString(
                "0x46b95e4e504b92d984c91d6f17eba4b60b904fb370818f0b6e74bc3ae5034403"),
            Bytes32.fromHexString(
                "0x46b95e4e504b92d984c91d6f17eba4b60b904fb370818f0b6e74bc3ae5034404"),
            Bytes32.fromHexString(
                "0x6127e4b0c266bee72914ce7261d0e4595c414c1ef439d9b0eb7d13cda5dc7640"));
    final Map<Bytes32, Bytes32> map =
        IntStream.range(0, expectedKeys.size())
            .boxed()
            .collect(Collectors.toMap(expectedKeys::get, expectedValues::get));
    final Map<Bytes32, Bytes32> generatedHashes =
        adapter.manyAccountTrieKeys(address, expectedKeys, List.of(storage), new ArrayList<>());
    assertThat(generatedHashes).containsExactlyEntriesOf(map);
  }

  @Test
  public void testAccountKeysWithCode() {
    final List<Bytes32> expectedKeys = new ArrayList<>();
    expectedKeys.add(Parameters.VERSION_LEAF_KEY);
    expectedKeys.add(Parameters.BALANCE_LEAF_KEY);
    expectedKeys.add(Parameters.NONCE_LEAF_KEY);
    expectedKeys.add(Parameters.CODE_KECCAK_LEAF_KEY);
    expectedKeys.add(Parameters.CODE_SIZE_LEAF_KEY);

    final UInt256 chunkId = UInt256.valueOf(24);
    expectedKeys.add(chunkId);

    final List<Bytes32> expectedValues =
        List.of(
            Bytes32.fromHexString(
                "0x46b95e4e504b92d984c91d6f17eba4b60b904fb370818f0b6e74bc3ae5034400"),
            Bytes32.fromHexString(
                "0x46b95e4e504b92d984c91d6f17eba4b60b904fb370818f0b6e74bc3ae5034401"),
            Bytes32.fromHexString(
                "0x46b95e4e504b92d984c91d6f17eba4b60b904fb370818f0b6e74bc3ae5034402"),
            Bytes32.fromHexString(
                "0x46b95e4e504b92d984c91d6f17eba4b60b904fb370818f0b6e74bc3ae5034403"),
            Bytes32.fromHexString(
                "0x46b95e4e504b92d984c91d6f17eba4b60b904fb370818f0b6e74bc3ae5034404"),
            Bytes32.fromHexString(
                "0x46b95e4e504b92d984c91d6f17eba4b60b904fb370818f0b6e74bc3ae5034498"));
    final Map<Bytes32, Bytes32> map =
        IntStream.range(0, expectedKeys.size())
            .boxed()
            .collect(Collectors.toMap(expectedKeys::get, expectedValues::get));
    final Map<Bytes32, Bytes32> generatedHashes =
        adapter.manyAccountTrieKeys(address, expectedKeys, new ArrayList<>(), List.of(chunkId));
    assertThat(generatedHashes).containsExactlyEntriesOf(map);
  }

  private static final ObjectMapper objectMapper = new ObjectMapper();

  static class KeyValueData {
    public String key;
    public String value;
  }

  static class TestCodeData {
    public String address;
    public String bytecode;
    public ArrayList<KeyValueData> chunks;
  }

  public static List<TestCodeData> JsonContractCodeData() throws IOException {
    InputStream inputStream =
        TrieKeyBatchAdapterTest.class.getResourceAsStream("/contractCode.json");
    return objectMapper.readValue(inputStream, new TypeReference<List<TestCodeData>>() {});
  }

  @ParameterizedTest
  @MethodSource("JsonContractCodeData")
  public void TestContractCode(TestCodeData testData) {
    Bytes addr = Bytes.fromHexString(testData.address);
    Bytes bytecode = Bytes.fromHexString(testData.bytecode);
    List<Bytes32> chunks = new ArrayList<>(adapter.chunkifyCode(bytecode));
    List<Bytes32> chunkIds =
        LongStream.rangeClosed(0, chunks.size())
            .mapToObj(value -> Bytes32.wrap(UInt256.valueOf(value)))
            .toList();
    assertThat(chunks.size()).as("Same number of chunks").isEqualTo(testData.chunks.size());

    final Map<Bytes32, Bytes32> generatedKeys =
        adapter.manyAccountTrieKeys(addr, new ArrayList<>(), new ArrayList<>(), chunkIds);
    for (int i = 0; i < chunks.size(); ++i) {
      Bytes32 key = generatedKeys.get(UInt256.valueOf(i));
      Bytes32 expectedKey = Bytes32.fromHexString(testData.chunks.get(i).key);
      assertThat(key).as(String.format("Key %s", i)).isEqualTo(expectedKey);
      Bytes32 value = chunks.get(i);
      Bytes32 expectedValue = Bytes32.fromHexString(testData.chunks.get(i).value);
      assertThat(value).as(String.format("Value %s", i)).isEqualTo(expectedValue);
    }
  }
}
