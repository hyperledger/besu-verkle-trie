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

import org.hyperledger.besu.ethereum.trie.verkle.factory.StoredNodeFactory;
import org.hyperledger.besu.ethereum.trie.verkle.node.Node;
import org.hyperledger.besu.ethereum.trie.verkle.node.NullNode;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;

public class StoredVerkleTrieTest {

  @Test
  public void testEmptyTrie() {
    NodeUpdaterMock nodeUpdater = new NodeUpdaterMock();
    NodeLoaderMock nodeLoader = new NodeLoaderMock(nodeUpdater.storage);
    StoredNodeFactory<Bytes32> nodeFactory =
        new StoredNodeFactory<>(nodeLoader, value -> (Bytes32) value);
    SimpleVerkleTrie<Bytes32, Bytes32> trie = new SimpleVerkleTrie<Bytes32, Bytes32>();
    trie.commit(nodeUpdater);
    assertThat(nodeUpdater.storage).isEmpty();
    Node<Bytes32> storedRoot =
        nodeFactory.retrieve(Bytes.EMPTY, Bytes32.ZERO).orElse(NullNode.instance());
    assertThat(storedRoot).isInstanceOf(NullNode.class);
  }

  @Test
  public void testOneValue() {
    NodeUpdaterMock nodeUpdater = new NodeUpdaterMock();
    NodeLoaderMock nodeLoader = new NodeLoaderMock(nodeUpdater.storage);
    StoredNodeFactory<Bytes32> nodeFactory =
        new StoredNodeFactory<>(nodeLoader, value -> (Bytes32) value);
    SimpleVerkleTrie<Bytes32, Bytes32> trie = new SimpleVerkleTrie<Bytes32, Bytes32>();
    Bytes32 key =
        Bytes32.fromHexString("0x00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    Bytes32 value =
        Bytes32.fromHexString("0x1000000000000000000000000000000000000000000000000000000000000000");
    trie.put(key, value);
    trie.commit(nodeUpdater);

    Node<Bytes32> storedRoot = nodeFactory.retrieve(Bytes.EMPTY, Bytes32.ZERO).get();
    SimpleVerkleTrie<Bytes32, Bytes32> storedTrie =
        new SimpleVerkleTrie<Bytes32, Bytes32>(storedRoot);
    assertThat(storedTrie.get(key).orElse(null)).as("Retrieved value").isEqualTo(value);
  }

  @Test
  public void testTwoValuesAtSameStem() throws Exception {
    NodeUpdaterMock nodeUpdater = new NodeUpdaterMock();
    NodeLoaderMock nodeLoader = new NodeLoaderMock(nodeUpdater.storage);
    StoredNodeFactory<Bytes32> nodeFactory =
        new StoredNodeFactory<>(nodeLoader, value -> (Bytes32) value);
    SimpleVerkleTrie<Bytes32, Bytes32> trie = new SimpleVerkleTrie<Bytes32, Bytes32>();
    Bytes32 key1 =
        Bytes32.fromHexString("0x00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    Bytes32 value1 =
        Bytes32.fromHexString("0x1000000000000000000000000000000000000000000000000000000000000000");
    Bytes32 key2 =
        Bytes32.fromHexString("0x00112233445566778899aabbccddeeff00112233445566778899aabbccddee00");
    Bytes32 value2 =
        Bytes32.fromHexString("0x0100000000000000000000000000000000000000000000000000000000000000");
    trie.put(key1, value1);
    trie.put(key2, value2);
    trie.commit(nodeUpdater);

    Node<Bytes32> storedRoot = nodeFactory.retrieve(Bytes.EMPTY, Bytes32.ZERO).get();
    SimpleVerkleTrie<Bytes32, Bytes32> storedTrie =
        new SimpleVerkleTrie<Bytes32, Bytes32>(storedRoot);
    assertThat(storedTrie.get(key1).orElse(null)).isEqualTo(value1);
    assertThat(storedTrie.get(key2).orElse(null)).isEqualTo(value2);
  }

  @Test
  public void testTwoValuesAtDifferentIndex() throws Exception {
    NodeUpdaterMock nodeUpdater = new NodeUpdaterMock();
    NodeLoaderMock nodeLoader = new NodeLoaderMock(nodeUpdater.storage);
    StoredNodeFactory<Bytes32> nodeFactory =
        new StoredNodeFactory<>(nodeLoader, value -> (Bytes32) value);
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
    trie.commit(nodeUpdater);

    Node<Bytes32> storedRoot = nodeFactory.retrieve(Bytes.EMPTY, Bytes32.ZERO).get();
    SimpleVerkleTrie<Bytes32, Bytes32> storedTrie =
        new SimpleVerkleTrie<Bytes32, Bytes32>(storedRoot);
    assertThat(storedTrie.get(key1).orElse(null)).isEqualTo(value1);
    assertThat(storedTrie.get(key2).orElse(null)).isEqualTo(value2);
  }

  @Test
  public void testTwoValuesWithDivergentStemsAtDepth2() throws Exception {
    NodeUpdaterMock nodeUpdater = new NodeUpdaterMock();
    NodeLoaderMock nodeLoader = new NodeLoaderMock(nodeUpdater.storage);
    StoredNodeFactory<Bytes32> nodeFactory =
        new StoredNodeFactory<>(nodeLoader, value -> (Bytes32) value);
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
    trie.commit(nodeUpdater);

    Node<Bytes32> storedRoot = nodeFactory.retrieve(Bytes.EMPTY, Bytes32.ZERO).get();
    SimpleVerkleTrie<Bytes32, Bytes32> storedTrie =
        new SimpleVerkleTrie<Bytes32, Bytes32>(storedRoot);
    assertThat(storedTrie.get(key1).orElse(null)).isEqualTo(value1);
    assertThat(storedTrie.get(key2).orElse(null)).isEqualTo(value2);
  }

  @Test
  public void testDeleteThreeValues() throws Exception {
    NodeUpdaterMock nodeUpdater = new NodeUpdaterMock();
    NodeLoaderMock nodeLoader = new NodeLoaderMock(nodeUpdater.storage);
    StoredNodeFactory<Bytes32> nodeFactory =
        new StoredNodeFactory<>(nodeLoader, value -> (Bytes32) value);
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
    trie.commit(nodeUpdater);

    Node<Bytes32> storedRoot = nodeFactory.retrieve(Bytes.EMPTY, Bytes32.ZERO).get();
    SimpleVerkleTrie<Bytes32, Bytes32> storedTrie =
        new SimpleVerkleTrie<Bytes32, Bytes32>(storedRoot);
    assertThat(storedTrie.get(key1).orElse(null)).isEqualTo(value1);
    assertThat(storedTrie.get(key2).orElse(null)).isEqualTo(value2);
    assertThat(storedTrie.get(key3).orElse(null)).isEqualTo(value3);
  }

  @Test
  public void testDeleteThreeValuesWithFlattening() throws Exception {
    NodeUpdaterMock nodeUpdater = new NodeUpdaterMock();
    NodeLoaderMock nodeLoader = new NodeLoaderMock(nodeUpdater.storage);
    StoredNodeFactory<Bytes32> nodeFactory =
        new StoredNodeFactory<>(nodeLoader, value -> (Bytes32) value);
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
    trie.commit(nodeUpdater);

    Node<Bytes32> storedRoot = nodeFactory.retrieve(Bytes.EMPTY, Bytes32.ZERO).get();
    SimpleVerkleTrie<Bytes32, Bytes32> storedTrie =
        new SimpleVerkleTrie<Bytes32, Bytes32>(storedRoot);
    assertThat(storedTrie.get(key1).orElse(null)).isEqualTo(value1);
    assertThat(storedTrie.get(key2).orElse(null)).isEqualTo(value2);
    assertThat(storedTrie.get(key3).orElse(null)).isEqualTo(value3);
  }
}
