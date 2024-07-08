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

import org.hyperledger.besu.ethereum.trie.NodeUpdater;

import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class NodeUpdaterMock implements NodeUpdater {

  public SortedMap<Bytes, byte[]> storage;

  public NodeUpdaterMock() {
    this.storage =
        new TreeMap<Bytes, byte[]>((b1, b2) -> b1.toHexString().compareTo(b2.toHexString()));
  }

  public NodeUpdaterMock(SortedMap<Bytes, byte[]> storage) {
    this.storage = storage;
  }

  @Override
  public void store(Bytes location, Bytes32 hash, Bytes value) {
    storage.put(location, value.toArray());
  }
}
