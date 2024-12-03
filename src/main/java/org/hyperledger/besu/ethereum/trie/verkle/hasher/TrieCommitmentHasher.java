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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import verkle.cryptography.LibIpaMultipoint;

public class TrieCommitmentHasher implements Hasher {

  Bytes DEFAULT_COMMITMENT =
      Bytes.concatenate(Bytes32.ZERO, Bytes32.rightPad(Bytes.fromHexString("0x01")));

  /**
   * Commit to a vector of values.
   *
   * @param inputs vector of serialized scalars to commit to.
   * @return uncompressed serialized commitment.
   */
  public Bytes commit(Bytes32[] inputs) {
    return Bytes.wrap(LibIpaMultipoint.commit(Bytes.concatenate(inputs).toArray()));
  }

  /**
   * Commit for the root node
   *
   * @param inputs An array of Bytes inputs to commit.
   * @return Compressed serialized commitment (32 bytes).
   */
  public Bytes32 commitRoot(final Bytes[] inputs) {
    return Bytes32.wrap(LibIpaMultipoint.commitAsCompressed(Bytes.concatenate(inputs).toArray()));
  }

  /**
   * Commit to a partially updated vector of values.
   *
   * @param commitment Optional existing commitment.
   * @param indices List of indices to update.
   * @param oldScalars List of old scalar values.
   * @param newScalars List of new scalar values.
   * @return Updated uncompressed serialized commitment.
   */
  public Bytes commitPartialUpdate(
      final Optional<Bytes> commitment,
      final List<Byte> indices,
      final List<Bytes> oldScalars,
      final List<Bytes> newScalars) {
    final Bytes cmnt = commitment.orElse(DEFAULT_COMMITMENT);
    final byte[] idx = new byte[indices.size()];
    for (int i = 0; i < indices.size(); i++) {
      idx[i] = indices.get(i);
    }
    return Bytes.wrap(
        LibIpaMultipoint.updateSparse(
            cmnt.toArray(),
            idx,
            prepareScalars(oldScalars.toArray(new Bytes[0])).toArray(),
            prepareScalars(newScalars.toArray(new Bytes[0])).toArray()));
  }

  /**
   * Convert a commitment to its serialized compressed form.
   *
   * @param commitment Uncompressed serialized commitment.
   * @return Serialized scalar (32 bytes).
   */
  public Bytes32 compress(final Bytes commitment) {
    return Bytes32.wrap(LibIpaMultipoint.compress(commitment.toArray()));
  }

  /**
   * Convert a commitment to its corresponding scalar.
   *
   * @param commitment Uncompressed serialized commitment.
   * @return Serialized scalar (32 bytes).
   */
  public Bytes32 hash(final Bytes commitment) {
    return Bytes32.wrap(LibIpaMultipoint.hashMany(commitment.toArray()));
  }

  /**
   * Map a vector of commitments to their corresponding scalars.
   *
   * <p>The vectorized version is highly optimized, making use of Montgomery's batch inversion
   * trick.
   *
   * @param commitments Uncompressed serialized commitments.
   * @return Serialized scalars (32 bytes each).
   */
  public List<Bytes32> hashMany(final Bytes[] commitments) {
    final Bytes hashMany =
        Bytes.wrap(LibIpaMultipoint.hashMany(Bytes.concatenate(commitments).toArray()));
    final List<Bytes32> hashes = new ArrayList<>();
    for (int i = 0; i < commitments.length; i++) {
      hashes.add(Bytes32.wrap(hashMany.slice(i * Bytes32.SIZE, Bytes32.SIZE)));
    }
    return hashes;
  }
}
