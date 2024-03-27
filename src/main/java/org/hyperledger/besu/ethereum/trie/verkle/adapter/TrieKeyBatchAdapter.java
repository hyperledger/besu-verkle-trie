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
package org.hyperledger.besu.ethereum.trie.verkle.adapter;

import static org.hyperledger.besu.ethereum.trie.verkle.util.Parameters.VERKLE_NODE_WIDTH;

import org.hyperledger.besu.ethereum.trie.verkle.hasher.Hasher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;

public class TrieKeyBatchAdapter extends TrieKeyAdapter {

  /**
   * Creates a TrieKeyBatchAdapter with the provided hasher.
   *
   * @param hasher The hasher used for key generation.
   */
  public TrieKeyBatchAdapter(final Hasher hasher) {
    super(hasher);
  }

  public Map<Bytes32, Bytes32> manyAccountTrieKeys(
      final Bytes address,
      final List<Bytes32> headerKeys,
      final List<Bytes32> storageKeys,
      final List<Bytes32> codeChunkIds) {

    final Set<Bytes32> offsets = new HashSet<>();

    if (headerKeys.size() > 0) {
      offsets.add(UInt256.ZERO);
    }
    for (Bytes32 storageKey : storageKeys) {
      final UInt256 storageOffset = locateStorageKeyOffset(storageKey);
      offsets.add(storageOffset.divide(VERKLE_NODE_WIDTH));
    }
    for (Bytes32 codeChunkId : codeChunkIds) {
      final UInt256 codeChunkOffset = locateCodeChunkKeyOffset(codeChunkId);
      offsets.add(codeChunkOffset.divide(VERKLE_NODE_WIDTH));
    }

    final Map<Bytes32, Bytes32> hashes =
        getHasher().manyTrieKeyHashes(address, new ArrayList<>(offsets));

    final Map<Bytes32, Bytes32> trieKeys = new HashMap<>();

    // header part
    for (Bytes32 headerKey : headerKeys) {
      trieKeys.put(headerKey, swapLastByte(hashes.get(UInt256.ZERO), headerKey));
    }
    // storage
    for (Bytes32 storageKey : storageKeys) {
      final UInt256 storageOffset = locateStorageKeyOffset(storageKey);
      trieKeys.put(
          storageKey,
          swapLastByte(
              hashes.get(storageOffset.divide(VERKLE_NODE_WIDTH)),
              storageOffset.mod(VERKLE_NODE_WIDTH)));
    }
    for (Bytes32 codeChunkId : codeChunkIds) {
      final UInt256 codeChunkOffset = locateCodeChunkKeyOffset(codeChunkId);
      trieKeys.put(
          codeChunkId,
          swapLastByte(
              hashes.get(codeChunkOffset.divide(VERKLE_NODE_WIDTH)),
              codeChunkOffset.mod(VERKLE_NODE_WIDTH)));
    }

    return trieKeys;
  }
}
