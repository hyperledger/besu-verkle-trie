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

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;

public class SimpleVerkleTrieTest {

  @Test
  public void testEmptyTrie() {
    SimpleVerkleTrie<Bytes32, Bytes32> trie = new SimpleVerkleTrie<Bytes32, Bytes32>();
    assertThat(trie.getRootHash()).as("Retrieve root hash").isEqualByComparingTo(Bytes32.ZERO);
  }

  @Test
  public void testOneValue() {
    SimpleVerkleTrie<Bytes32, Bytes32> trie = new SimpleVerkleTrie<Bytes32, Bytes32>();
    Bytes32 key =
        Bytes32.fromHexString("0x00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    Bytes32 value =
        Bytes32.fromHexString("0x1000000000000000000000000000000000000000000000000000000000000000");
    trie.put(key, value);
    assertThat(trie.get(key))
        .as("Get one value should be the inserted value")
        .isEqualTo(Optional.of(value));
    Bytes32 expectedRootHash =
        Bytes32.fromHexString("afceaacfd8f1d62ceff7d2bbfc733e42fdb40cef6f7c3c870a5bdd9203c30a16");
    assertThat(trie.getRootHash()).as("Retrieve root hash").isEqualByComparingTo(expectedRootHash);
  }

  @Test
  public void testTwoValuesAtSameStem() throws Exception {
    SimpleVerkleTrie<Bytes32, Bytes32> trie = new SimpleVerkleTrie<Bytes32, Bytes32>();
    Bytes32 key1 =
        Bytes32.fromHexString("0x00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    Bytes32 value1 =
        Bytes32.fromHexString("0x1000000000000000000000000000000000000000000000000000000000000000");
    Bytes32 key2 =
        Bytes32.fromHexString("0x00112233445566778899aabbccddeeff00112233445566778899aabbccddee00");
    Bytes32 value2 =
        Bytes32.fromHexString("0x0100000000000000000000000000000000000000000000000000000000000000");
    Bytes32 key3 =
        Bytes32.fromHexString("0xde112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    trie.put(key1, value1);
    trie.put(key2, value2);
    assertThat(trie.get(key1).get()).as("Get first value").isEqualByComparingTo(value1);
    assertThat(trie.get(key2).get()).as("Get second value").isEqualByComparingTo(value2);
    assertThat(trie.get(key3)).as("Get non-key returns empty").isEmpty();

    Bytes32 expectedRootHash =
        Bytes32.fromHexString("1defb89c793eb6cf89a90fe7e9bff4b96b5c9774ad21433adb959466a7669602");
    assertThat(trie.getRootHash()).as("Get root hash").isEqualByComparingTo(expectedRootHash);
  }

  @Test
  public void testTwoValuesAtDifferentIndex() throws Exception {
    SimpleVerkleTrie<Bytes32, Bytes32> trie = new SimpleVerkleTrie<Bytes32, Bytes32>();
    Bytes32 key1 =
        Bytes32.fromHexString("0x00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    Bytes32 value1 =
        Bytes32.fromHexString("0x1000000000000000000000000000000000000000000000000000000000000000");
    Bytes32 key2 =
        Bytes32.fromHexString("0xff112233445566778899aabbccddeeff00112233445566778899aabbccddee00");
    Bytes32 value2 =
        Bytes32.fromHexString("0x0100000000000000000000000000000000000000000000000000000000000000");
    trie.put(key1, value1);
    trie.put(key2, value2);
    assertThat(trie.get(key1).get()).as("Get first value").isEqualByComparingTo(value1);
    assertThat(trie.get(key2).get()).as("Get second value").isEqualByComparingTo(value2);
    Bytes32 expectedRootHash =
        Bytes32.fromHexString("1758925a729ae085d4a2e32139f47c647f70495a6a38053bc0056996dd34b60e");
    assertThat(trie.getRootHash()).as("Retrieve root hash").isEqualByComparingTo(expectedRootHash);
  }

  @Test
  public void testTwoValuesWithDivergentStemsAtDepth2() throws Exception {
    SimpleVerkleTrie<Bytes32, Bytes32> trie = new SimpleVerkleTrie<Bytes32, Bytes32>();
    Bytes32 key1 =
        Bytes32.fromHexString("0x00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    Bytes32 value1 =
        Bytes32.fromHexString("0x1000000000000000000000000000000000000000000000000000000000000000");
    Bytes32 key2 =
        Bytes32.fromHexString("0x00ff112233445566778899aabbccddeeff00112233445566778899aabbccddee");
    Bytes32 value2 =
        Bytes32.fromHexString("0x0100000000000000000000000000000000000000000000000000000000000000");
    trie.put(key1, value1);
    trie.put(key2, value2);
    assertThat(trie.get(key1)).as("Retrieve first value").isEqualTo(Optional.of(value1));
    assertThat(trie.get(key2)).as("Retrieve second value").isEqualTo(Optional.of(value2));
    Bytes32 expectedRootHash =
        Bytes32.fromHexString("88028cbafb20137dba8b42d243cfcac81f6ac635cf984c7a89e54ef006bf750d");
    assertThat(trie.getRootHash()).as("Retrieve root hash").isEqualByComparingTo(expectedRootHash);
  }

  @Test
  public void testDeleteTwoValuesAtSameStem() throws Exception {
    SimpleVerkleTrie<Bytes32, Bytes32> trie = new SimpleVerkleTrie<Bytes32, Bytes32>();
    Bytes32 key1 =
        Bytes32.fromHexString("0x00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    Bytes32 value1 =
        Bytes32.fromHexString("0x1000000000000000000000000000000000000000000000000000000000000001");
    Bytes32 key2 =
        Bytes32.fromHexString("0x00112233445566778899aabbccddeeff00112233445566778899aabbccddee00");
    Bytes32 value2 =
        Bytes32.fromHexString("0x0100000000000000000000000000000000000000000000000000000000000002");
    trie.put(key1, value1);
    trie.put(key2, value2);
    trie.remove(key1);
    assertThat(trie.get(key1)).as("Make sure value is deleted").isEqualTo(Optional.empty());
    trie.remove(key2);
    assertThat(trie.get(key2)).as("Make sure value is deleted").isEqualTo(Optional.empty());
  }

  @Test
  public void testDeleteTwoValuesAtDifferentIndex() throws Exception {
    SimpleVerkleTrie<Bytes32, Bytes32> trie = new SimpleVerkleTrie<Bytes32, Bytes32>();
    Bytes32 key1 =
        Bytes32.fromHexString("0x00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    Bytes32 value1 =
        Bytes32.fromHexString("0x1000000000000000000000000000000000000000000000000000000000000000");
    Bytes32 key2 =
        Bytes32.fromHexString("0xff112233445566778899aabbccddeeff00112233445566778899aabbccddee00");
    Bytes32 value2 =
        Bytes32.fromHexString("0x0100000000000000000000000000000000000000000000000000000000000000");
    trie.put(key1, value1);
    trie.put(key2, value2);
    trie.remove(key1);
    assertThat(trie.get(key1)).as("Make sure value is deleted").isEqualTo(Optional.empty());
    trie.remove(key2);
    assertThat(trie.get(key2)).as("Make sure value is deleted").isEqualTo(Optional.empty());
  }

  @Test
  public void testDeleteTwoValuesWithDivergentStemsAtDepth2() throws Exception {
    SimpleVerkleTrie<Bytes32, Bytes32> trie = new SimpleVerkleTrie<Bytes32, Bytes32>();
    Bytes32 key1 =
        Bytes32.fromHexString("0x00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    Bytes32 value1 =
        Bytes32.fromHexString("0x1000000000000000000000000000000000000000000000000000000000000000");
    Bytes32 key2 =
        Bytes32.fromHexString("0x00ff112233445566778899aabbccddeeff00112233445566778899aabbccddee");
    Bytes32 value2 =
        Bytes32.fromHexString("0x0100000000000000000000000000000000000000000000000000000000000000");
    trie.put(key1, value1);
    trie.put(key2, value2);
    trie.remove(key1);
    assertThat(trie.get(key1)).as("Make sure value is deleted").isEqualTo(Optional.empty());
    trie.remove(key2);
    assertThat(trie.get(key2)).as("Make sure value is deleted").isEqualTo(Optional.empty());
  }

  @Test
  public void testDeleteThreeValues() throws Exception {
    SimpleVerkleTrie<Bytes32, Bytes32> trie = new SimpleVerkleTrie<Bytes32, Bytes32>();
    Bytes32 key1 =
        Bytes32.fromHexString("0x00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    Bytes32 value1 =
        Bytes32.fromHexString("0x1000000000000000000000000000000000000000000000000000000000000000");
    Bytes32 key2 =
        Bytes32.fromHexString("0x00ff112233445566778899aabbccddeeff00112233445566778899aabbccddee");
    Bytes32 value2 =
        Bytes32.fromHexString("0x0200000000000000000000000000000000000000000000000000000000000000");
    Bytes32 key3 =
        Bytes32.fromHexString("0x00ff112233445566778899aabbccddeeff00112233445566778899aabbccddff");
    Bytes32 value3 =
        Bytes32.fromHexString("0x0300000000000000000000000000000000000000000000000000000000000000");
    trie.put(key1, value1);
    trie.put(key2, value2);
    trie.put(key3, value3);
    trie.remove(key3);
    assertThat(trie.get(key3)).as("Make sure value is deleted").isEqualTo(Optional.empty());
    assertThat(trie.get(key2)).as("Retrieve second value").isEqualTo(Optional.of(value2));
    trie.remove(key2);
    assertThat(trie.get(key2)).as("Make sure value is deleted").isEqualTo(Optional.empty());
    assertThat(trie.get(key1)).as("Retrieve first value").isEqualTo(Optional.of(value1));
    trie.remove(key1);
    assertThat(trie.get(key1)).as("Make sure value is deleted").isEqualTo(Optional.empty());
  }

  @Test
  public void testDeleteThreeValuesWithFlattening() throws Exception {
    SimpleVerkleTrie<Bytes32, Bytes32> trie = new SimpleVerkleTrie<Bytes32, Bytes32>();
    Bytes32 key1 =
        Bytes32.fromHexString("0x00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    Bytes32 value1 =
        Bytes32.fromHexString("0x1000000000000000000000000000000000000000000000000000000000000000");
    Bytes32 key2 =
        Bytes32.fromHexString("0x00ff112233445566778899aabbccddeeff00112233445566778899aabbccddee");
    Bytes32 value2 =
        Bytes32.fromHexString("0x0200000000000000000000000000000000000000000000000000000000000000");
    Bytes32 key3 =
        Bytes32.fromHexString("0x00ff112233445566778899aabbccddeeff00112233445566778899aabbccddff");
    Bytes32 value3 =
        Bytes32.fromHexString("0x0300000000000000000000000000000000000000000000000000000000000000");
    trie.put(key1, value1);
    trie.put(key2, value2);
    trie.put(key3, value3);
    trie.remove(key1);
    assertThat(trie.get(key1)).as("Make sure value is deleted").isEqualTo(Optional.empty());
    assertThat(trie.get(key2)).as("Retrieve second value").isEqualTo(Optional.of(value2));
    trie.remove(key2);
    assertThat(trie.get(key2)).as("Make sure value is deleted").isEqualTo(Optional.empty());
    assertThat(trie.get(key3)).as("Retrieve first value").isEqualTo(Optional.of(value3));
    trie.remove(key3);
    assertThat(trie.get(key3)).as("Make sure value is deleted").isEqualTo(Optional.empty());
  }
}
