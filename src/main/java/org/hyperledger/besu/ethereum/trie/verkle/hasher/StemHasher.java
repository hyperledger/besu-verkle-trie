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

import org.hyperledger.besu.ethereum.trie.verkle.hasher.cache.CacheStrategy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import verkle.cryptography.LibIpaMultipoint;

public class StemHasher implements Hasher {

  // The amount of bytes that we will take from the input when `trieKeyHash` is
  // called.
  private static final int CHUNK_SIZE = 16;
  // Size of the stem is 31 bytes
  private static final int STEM_SIZE = 31;

  private final CacheStrategy<Bytes32, Bytes> stemCache;
  private final CacheStrategy<Bytes, Bytes> addressCommitmentCache;

  /**
   * Constructs a StemHasher with caching strategies for stems and address commitments.
   *
   * @param stemCache The caching strategy for stems.
   * @param addressCommitmentCache The caching strategy for address commitments.
   */
  public StemHasher(
      CacheStrategy<Bytes32, Bytes> stemCache, CacheStrategy<Bytes, Bytes> addressCommitmentCache) {
    this.stemCache = stemCache;
    this.addressCommitmentCache = addressCommitmentCache;
  }

  /**
   * Computes the stem for a given address and index.
   *
   * @param address The address to compute the stem for.
   * @param index The index within the storage.
   * @return The trie-key hash as a Bytes object.
   */
  public Bytes computeStem(final Bytes address, final Bytes32 index) {
    final Bytes addressCommitment = computeAddressCommitment(address);
    return stemCache.get(index, key -> computeHashes(addressCommitment, List.of(key)).get(key));
  }

  /**
   * Computes stems for a given address and multiple indexes.
   *
   * @param address The address to compute stems for.
   * @param trieKeyIndexes List of indexes within the storage.
   * @return A map of indexes to their corresponding trie-key hashes.
   */
  public Map<Bytes32, Bytes> manyStems(final Bytes address, final List<Bytes32> trieKeyIndexes) {
    final Bytes addressCommitment = computeAddressCommitment(address);
    final Map<Bytes32, Bytes> result = new HashMap<>();
    final List<Bytes32> missingKeys = new ArrayList<>();

    // Retrieve existing stems from the cache and identify missing keys
    trieKeyIndexes.forEach(
        key -> {
          Bytes value = stemCache.getIfPresent(key);
          if (value != null) {
            result.put(key, value);
          } else {
            missingKeys.add(key);
          }
        });

    // Generate missing stems if any keys are missing
    if (!missingKeys.isEmpty()) {
      final Map<Bytes32, Bytes> generatedStems = computeHashes(addressCommitment, missingKeys);
      result.putAll(generatedStems);
      stemCache.putAll(generatedStems);
    }

    return result;
  }

  /**
   * Computes the address commitment for a given address.
   *
   * @param address The address to compute the commitment for.
   * @return The computed address commitment as a byte array.
   */
  private Bytes computeAddressCommitment(final Bytes address) {
    return addressCommitmentCache.get(address, this::computeAddressCommitmentInternal);
  }

  /**
   * Computes the address commitment for a given address.
   *
   * @param address The address to compute the commitment for.
   * @return The computed address commitment as a byte array.
   */
  private Bytes computeAddressCommitmentInternal(final Bytes address) {
    Bytes32 paddedAddress = Bytes32.leftPad(address);
    Bytes32[] chunks = generatePartialAddressChunks(paddedAddress);
    return Bytes.wrap(LibIpaMultipoint.commit(Bytes.concatenate(chunks).toArray()));
  }

  /**
   * Generalized method to compute hashes for one or more indexes.
   *
   * @param addressCommitment Account address commitment.
   * @param indexes List of indexes in storage.
   * @return A map of indexes to their corresponding hashes.
   */
  private Map<Bytes32, Bytes> computeHashes(
      final Bytes addressCommitment, final List<Bytes32> indexes) {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      for (Bytes32 index : indexes) {
        Bytes[] indexChunks = generatePartialIndexChunks(index);
        outputStream.writeBytes(
            LibIpaMultipoint.updateSparse(
                addressCommitment.toArrayUnsafe(),
                new byte[] {3, 4}, // Index low + index high
                new byte[Bytes32.SIZE * 2],
                prepareScalars(indexChunks).toArrayUnsafe()));
      }

      Bytes hashedData = Bytes.wrap(LibIpaMultipoint.hashMany(outputStream.toByteArray()));
      Map<Bytes32, Bytes> stems = new HashMap<>();
      for (int i = 0; i < indexes.size(); i++) {
        stems.put(indexes.get(i), hashedData.slice(i * Bytes32.SIZE, STEM_SIZE));
      }
      return stems;
    } catch (IOException e) {
      throw new RuntimeException("Failed to compute trie-key hashes", e);
    }
  }

  /**
   * Generates chunks for the given address, padded to 32 bytes.
   *
   * @param address The address to chunk.
   * @return The address chunks.
   */
  private Bytes32[] generatePartialAddressChunks(final Bytes address) {
    final Bytes32 paddedAddress = Bytes32.leftPad(address);
    return new Bytes32[] {
      Bytes32.rightPad(Bytes.of(2, 64)),
      Bytes32.rightPad(paddedAddress.slice(0, CHUNK_SIZE)),
      Bytes32.rightPad(paddedAddress.slice(CHUNK_SIZE, CHUNK_SIZE))
    };
  }

  /**
   * Generates chunks for the given index, reversed to little-endian format.
   *
   * @param index The index to chunk.
   * @return The index chunks.
   */
  private Bytes32[] generatePartialIndexChunks(final Bytes32 index) {
    Bytes32 indexLE = Bytes32.wrap(index.reverse());
    return new Bytes32[] {
      Bytes32.rightPad(indexLE.slice(0, CHUNK_SIZE)), // Index low
      Bytes32.rightPad(indexLE.slice(CHUNK_SIZE, CHUNK_SIZE)) // Index high
    };
  }
}
