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

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.hyperledger.besu.ethereum.trie.verkle.factory.StoredNodeFactory;

import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;

public class NodeDirtyTest {

  @Test
  public void testOneValueSimpleVerkleTrie() {
    NodeUpdaterMock nodeUpdater = new NodeUpdaterMock();
    SimpleVerkleTrie<Bytes32, Bytes32> trie = new SimpleVerkleTrie<>();
    Bytes32 key =
        Bytes32.fromHexString("0x00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    Bytes32 value =
        Bytes32.fromHexString("0x1000000000000000000000000000000000000000000000000000000000000000");
    trie.put(key, value);

    trie.commit(nodeUpdater);
    assertFalse(trie.getRoot().isDirty());
  }

  @Test
  public void testOneValueStoredVerkleTrie() {
    NodeUpdaterMock nodeUpdater = new NodeUpdaterMock();
    NodeLoaderMock nodeLoader = new NodeLoaderMock(nodeUpdater.storage);
    StoredNodeFactory<Bytes32> nodeFactory =
        new StoredNodeFactory<>(nodeLoader, value -> (Bytes32) value);
    StoredVerkleTrie<Bytes32, Bytes32> trie = new StoredVerkleTrie<>(nodeFactory);
    Bytes32 key =
        Bytes32.fromHexString("0x00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    Bytes32 value =
        Bytes32.fromHexString("0x1000000000000000000000000000000000000000000000000000000000000000");
    trie.put(key, value);

    trie.commit(nodeUpdater);
    assertFalse(trie.getRoot().isDirty());
  }
}
