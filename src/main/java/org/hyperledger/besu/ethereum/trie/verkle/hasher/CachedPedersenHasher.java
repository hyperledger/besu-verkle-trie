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
package org.hyperledger.besu.ethereum.trie.verkle.hasher;

import java.util.List;
import java.util.Map;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class CachedPedersenHasher implements Hasher {
  private final Map<Bytes32, Bytes32> preloadedTrieKeyHashes;
  private final Hasher fallbackHasher;

  public CachedPedersenHasher(final Map<Bytes32, Bytes32> preloadedTrieKeyHashes) {
    this.preloadedTrieKeyHashes = preloadedTrieKeyHashes;
    this.fallbackHasher = new PedersenHasher();
  }

  public CachedPedersenHasher(
      final Map<Bytes32, Bytes32> preloadedTrieKeyHashes, final Hasher fallbackHasher) {
    this.preloadedTrieKeyHashes = preloadedTrieKeyHashes;
    this.fallbackHasher = fallbackHasher;
  }

  @Override
  public Bytes commit(final Bytes32[] bytes32s) {
    return fallbackHasher.commit(bytes32s);
  }

  @Override
  public Bytes32 commitRoot(final Bytes32[] bytes32s) {
    return fallbackHasher.commitRoot(bytes32s);
  }

  @Override
  public Bytes32 trieKeyHash(final Bytes bytes, final Bytes32 bytes32) {
    final Bytes32 hash = preloadedTrieKeyHashes.get(bytes32);
    if (hash != null) {
      return hash;
    } else {
      return fallbackHasher.trieKeyHash(bytes, bytes32);
    }
  }

  @Override
  public Map<Bytes32, Bytes32> manyTrieKeyHashes(final Bytes bytes, final List<Bytes32> list) {
    return fallbackHasher.manyTrieKeyHashes(bytes, list);
  }

  @Override
  public Bytes32 groupToField(final Bytes bytes) {
    return fallbackHasher.groupToField(bytes);
  }
}
