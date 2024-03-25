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

import org.hyperledger.besu.nativelib.ipamultipoint.LibIpaMultipoint;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

/**
 * A class responsible for hashing an array of Bytes32 using the pedersen commitment - multi scalar
 * multiplication vector commitment algorithm.
 *
 * <p>This class implements the Hasher interface and provides a method to commit multiple Bytes32
 * inputs using the pedersen commitment - multi scalar multiplication vector commitment algorithm.
 */
public class PedersenHasher implements Hasher {

  // The amount of bytes that we will take from the input when `trieKeyHash` is
  // called.
  private static final int CHUNK_SIZE = 16;

  // The input will always be 64 bytes once it is padded in `trieKeyHash `
  // and since we are taking 16 bytes at a time we will create four chunks from
  // the input.
  // An extra chunk which has a constant value is added as a domain separator and
  // length encoder,
  // making the total number of chunks equal to five.
  private static final int NUM_CHUNKS = 5;

  /**
   * Commits an array of Bytes32 using the pedersen commitment - multi scalar multiplication vector
   * commitment algorithm.
   *
   * @param inputs An array of Bytes32 inputs to be committed.
   * @return The resulting commitment serliazed as uncompressed eliptic curve element (64bytes).
   */
  @Override
  public Bytes commit(Bytes32[] inputs) {
    return Bytes.wrap(LibIpaMultipoint.commit(Bytes.concatenate(inputs).toArray()));
  }

  /**
   * @param inputs An array of Bytes32 inputs to be committed.
   * @return The result is commitment as compressed eliptic curve element (32bytes). This is needed
   *     to computing root Commitment.
   */
  @Override
  public Bytes32 commitRoot(final Bytes32[] inputs) {
    return Bytes32.wrap(LibIpaMultipoint.commitRoot(Bytes.concatenate(inputs).toArray()));
  }

  /**
   * @param input Uncompressed serialized commitment (64bytes)
   * @return return Fr, to be used in pared commitment.
   */
  @Override
  public Bytes32 groupToField(Bytes input) {
    return Bytes32.wrap(LibIpaMultipoint.groupToField(input.toArray()));
  }

  private Bytes32[] generateTrieKeyChunks(final Bytes address, final Bytes32 index) {
    // Reverse the index so that it is in little endian format
    final Bytes32 indexLE = Bytes32.wrap(index.reverse());

    // Pad the address so that it is 32 bytes
    final Bytes32 addr = Bytes32.leftPad(address);
    final Bytes input = Bytes.concatenate(addr, indexLE);

    final Bytes32[] chunks = new Bytes32[NUM_CHUNKS];

    // Set the first chunk which is always 2 + 256 * 64
    chunks[0] = Bytes32.rightPad(Bytes.of(2, 64));

    // Given input is 64 bytes, we create exactly 4 chunks of 16 bytes each
    // The chunks are then padded to 32 bytes since the commit methods requires 32
    // byte scalars.
    for (int i = 0; i < NUM_CHUNKS - 1; i++) {
      // Slice input into 16 byte segments
      Bytes chunk = input.slice(i * CHUNK_SIZE, CHUNK_SIZE);
      // Pad each chunk to make it 32 bytes
      chunks[i + 1] = Bytes32.rightPad(chunk);
    }

    return chunks;
  }

  /**
   * Calculates the hash for an address and index.
   *
   * @param address Account address.
   * @param index Index in storage.
   * @return The trie-key hash
   */
  @Override
  public Bytes32 trieKeyHash(Bytes address, Bytes32 index) {

    final Bytes32[] chunks = generateTrieKeyChunks(address, index);

    // Set the first chunk which is always 2 + 256 * 64
    chunks[0] = Bytes32.rightPad(Bytes.of(2, 64));

    // commitRoot returns the hash in big endian format, so we reverse it to get it
    // in little endian
    // format. When we migrate to using `groupToField`, this reverse will not be
    // needed.
    return Bytes32.wrap(
        Bytes.wrap(
                LibIpaMultipoint.hash(LibIpaMultipoint.commit(Bytes.concatenate(chunks).toArray())))
            .reverse());
  }

  /**
   * Calculates the hash for an address and indexes.
   *
   * @param address Account address.
   * @param indexes list of indexes in storage.
   * @return The list of trie-key hashes
   */
  @Override
  public List<Bytes32> manyTrieKeyHashes(Bytes address, List<Bytes32> indexes) {

    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      for (Bytes32 index : indexes) {
        final Bytes32[] chunks = generateTrieKeyChunks(address, index);
        // Set the first chunk which is always 2 + 256 * 64
        chunks[0] = Bytes32.rightPad(Bytes.of(2, 64));

        outputStream.writeBytes(
            LibIpaMultipoint.commit(
                Bytes.concatenate(chunks).toArray()));
      }

      final Bytes hashBE = Bytes.wrap(LibIpaMultipoint.hashMany(outputStream.toByteArray()));

      final List<Bytes32> hashes = new ArrayList<>();
      for (int i = 0; i < indexes.size() - 1; i++) {
        // Slice input into 16 byte segments
        hashes.add(
            Bytes32.wrap(
                hashBE.slice(
                    i * Bytes32.SIZE,
                    Bytes32
                        .SIZE)));
        // commitRoot returns the hash in big endian format, so we reverse
        // it to get it
        // in little endian
        // format. When we migrate to using `groupToField`, this reverse will not be
        // needed.
        // return Bytes32.wrap(hashBE.reverse());
        // TODO when doing reverse ??
      }
      return hashes;
    } catch (IOException e) {
      throw new RuntimeException("unable to generate trie key hash", e);
    }
  }
}
