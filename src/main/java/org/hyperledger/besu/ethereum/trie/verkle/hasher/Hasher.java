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
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

/** Defines an interface for a Verkle Trie node hashing strategy. */
public interface Hasher {

  Bytes DEFAULT_COMMITMENT =
      Bytes.concatenate(Bytes32.ZERO, Bytes32.rightPad(Bytes.fromHexString("0x01")));

  /**
   * Commit to a vector of values.
   *
   * @param inputs vector of serialised scalars to commit to.
   * @return uncompressed serialised commitment.
   */
  Bytes commit(Bytes32[] inputs);

  /**
   * Compute and serialise compress commitment to a dense vector of scalar values.
   *
   * @param scalars Serialised scalar values to commit to, up to 32-bytes-le.
   * @return The compressed serialized commitment used for calucating root Commitment.
   */
  Bytes32 commitRoot(Bytes[] scalars);

  /**
   * Update a commitment with a sparse vector of values.
   *
   * @param commitment Actual commitment value.
   * @param indices List of vector's indices where values are updated.
   * @param oldScalars List of previous scalar values.
   * @param newScalars List of new scalar values.
   * @return The uncompressed serialized updated commitment.
   */
  Bytes commitUpdate(
      Optional<Bytes> commitment,
      List<Byte> indices,
      List<Bytes> oldScalars,
      List<Bytes> newScalars);

  /**
   * Convert a commitment to its serialised compressed form.
   *
   * @param commitment uncompressed serialised commitment
   * @return serialised scalar
   */
  Bytes32 compress(Bytes commitment);

  /**
   * Convert a commitment to its corresponding scalar.
   *
   * @param commitment uncompressed serialised commitment
   * @return serialised scalar
   */
  Bytes32 hash(Bytes commitment);

  /**
   * Map a vector of commitments to its corresponding vector of scalars.
   *
   * <p>The vectorised version is highly optimised, making use of Montgoméry's batch inversion
   * trick.
   *
   * @param commitments uncompressed serialised commitments
   * @return serialised scalars
   */
  List<Bytes32> hashMany(Bytes[] commitments);

  /**
   * Calculates the stem for an address and index.
   *
   * @param address Account address.
   * @param index index in storage.
   * @return trie-key hash
   */
  Bytes computeStem(Bytes address, Bytes32 index);

  /**
   * Calculates the stem for an address and indexes.
   *
   * @param address Account address.
   * @param indexes list of indexes in storage.
   * @return The list of trie-key hashes
   */
  Map<Bytes32, Bytes> manyStems(Bytes address, List<Bytes32> indexes);
}
