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

import kotlin.Pair;
import org.hyperledger.besu.ethereum.trie.verkle.hasher.Hasher;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

  public List<Bytes32> manyAccountKeys(final Bytes address,final List<UInt256> headerKeys, final List<UInt256> codeChunkIds, final List<Bytes32> storageKeys) {

    final List<Pair<UInt256,UInt256>> offsets = new ArrayList<>();

    if(headerKeys.size()>0){
      offsets.add(new Pair<>(UInt256.ZERO,UInt256.ZERO));
    }
    for (Bytes32 storageKey : storageKeys) {
      final UInt256 storageOffset = locateStorageKeyOffset(storageKey);
      offsets.add(new Pair<>(storageOffset.divide(VERKLE_NODE_WIDTH),storageOffset.mod(VERKLE_NODE_WIDTH)));
    }
    for (UInt256 codeChunkId : codeChunkIds) {
      final UInt256 codeChunkOffset = locateCodeChunkKeyOffset(codeChunkId);
      offsets.add(new Pair<>(codeChunkOffset.divide(VERKLE_NODE_WIDTH),codeChunkOffset.mod(VERKLE_NODE_WIDTH)));
    }

    final Iterator<Bytes32> hashes =
        getHasher()
            .manyTrieKeyHashes(
                address,
                offsets.stream()
                    .map(Pair::getFirst)
                    .collect(Collectors.toList())).iterator();

    final List<Bytes32> trieKeys = new ArrayList<>();

    Bytes32 currentHash = hashes.next();

    //header part
    for (UInt256 headerKey: headerKeys) {
      trieKeys.add(swapLastByte(currentHash, headerKey));
    }

    //storage
    for (Pair<UInt256, UInt256> storageOffset : offsets) {
      currentHash = hashes.next();
      trieKeys.add(swapLastByte(currentHash, storageOffset.getSecond()));
    }

    //code
    for (Pair<UInt256, UInt256> chunkOffset : offsets) {
      currentHash = hashes.next();
      trieKeys.add(swapLastByte(currentHash, chunkOffset.getSecond()));
    }

    return trieKeys;
  }


}
