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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class CachedPedersenHasher implements Hasher {
  private final Cache<Bytes32, Bytes> stemCache;
  private final Hasher fallbackHasher;

  public CachedPedersenHasher(final int cacheSize) {
    this(cacheSize, new HashMap<>());
  }

  public CachedPedersenHasher(final int cacheSize, final Map<Bytes32, Bytes> preloadedStems) {
    this(cacheSize, preloadedStems, new PedersenHasher());
  }

  public CachedPedersenHasher(
      final int cacheSize, final Map<Bytes32, Bytes> preloadedStems, final Hasher fallbackHasher) {
    this.stemCache = CacheBuilder.newBuilder().maximumSize(cacheSize).build();
    this.stemCache.putAll(preloadedStems);
    this.fallbackHasher = fallbackHasher;
  }

  @Override
  public Bytes commit(final Bytes32[] bytes32s) {
    return fallbackHasher.commit(bytes32s);
  }

  @Override
  public Bytes32 commitRoot(final Bytes[] bytes32s) {
    return fallbackHasher.commitRoot(bytes32s);
  }

  @Override
  public Bytes commitUpdate(
      Optional<Bytes> commitment,
      List<Byte> indices,
      List<Bytes> oldScalars,
      List<Bytes> newScalars) {
    return fallbackHasher.commitUpdate(commitment, indices, oldScalars, newScalars);
  }

  @Override
  public Bytes32 compress(Bytes commitment) {
    return fallbackHasher.compress(commitment);
  }

  @Override
  public Bytes computeStem(final Bytes address, final Bytes32 trieKeyIndex) {
    Bytes stem = stemCache.getIfPresent(trieKeyIndex);
    if (stem != null) {
      return stem;
    } else {
      stem = fallbackHasher.computeStem(address, trieKeyIndex);
      stemCache.put(trieKeyIndex, stem);
      return stem;
    }
  }

  @Override
  public Map<Bytes32, Bytes> manyStems(final Bytes address, final List<Bytes32> trieKeyIndexes) {
    final Map<Bytes32, Bytes> trieKeyHashes = fallbackHasher.manyStems(address, trieKeyIndexes);
    stemCache.putAll(trieKeyHashes);
    return trieKeyHashes;
  }

  @Override
  public Bytes32 hash(final Bytes bytes) {
    return fallbackHasher.hash(bytes);
  }

  @Override
  public List<Bytes32> hashMany(final Bytes[] inputs) {
    return fallbackHasher.hashMany(inputs);
  }
}
