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
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

/** Defines an interface for a Verkle Trie node hashing strategy. */
public interface Hasher {
  public Bytes getDefaultCommitment();

  public Bytes32 getDefaultScalar();

  /**
   * Compute commitment to a dense vector of scalar values.
   *
   * @param scalars Serialised scalar values to commit to, up to 32-bytes-le.
   * @return The uncompressed serialized commitment.
   * @throws Exception Problem with native invocation
   */
  public Bytes commit(Bytes[] scalars) throws Exception;

  /**
   * Compute and serialise compress commitment to a dense vector of scalar values.
   *
   * @param scalars Serialised scalar values to commit to, up to 32-bytes-le.
   * @return The compressed serialized commitment used for calucating root Commitment.
   * @throws Exception Problem with native invocation
   */
  public Bytes32 commitAsCompressed(Bytes[] scalars) throws Exception;

  /**
   * Update a commitment with a sparse vector of values.
   *
   * @param commitment Actual commitment value.
   * @param indices List of vector's indices where values are updated.
   * @param oldScalars List of previous scalar values.
   * @param newScalars List of new scalar values.
   * @return The uncompressed serialized updated commitment.
   * @throws Exception Problem with native invocation
   */
  public Bytes updateSparse(
      Optional<Bytes> commitment,
      List<Byte> indices,
      List<Bytes> oldScalars,
      List<Bytes> newScalars)
      throws Exception;

  /**
   * Computes the compressed serialised form of an uncompressed commitment.
   *
   * @param commitment Uncompressed serialised commitment to compress
   * @return compressed commitment
   * @throws Exception Problem with native invocation
   */
  public Bytes32 compress(Bytes commitment) throws Exception;
  // public List<Bytes32> compressMany(List<Bytes> commitments) throws Exception;

  /**
   * Computes the scalar mapped from a commitment.
   *
   * @param commitment Uncompressed serialised commitment to hash.
   * @return scalar
   * @throws Exception Problem with native invocation
   */
  public Bytes32 hash(Bytes commitment) throws Exception;

  /**
   * Computes scalars mapped from a list of commitments.
   *
   * <p>Note that this method is highly optimised vectorised version of hash. It should be used as
   * much as possible over multiple single hash.
   *
   * @param commitments List of uncompressed serialised commitment to hash.
   * @return List of scalars
   * @throws Exception Problem with native invocation
   */
  public Bytes32[] hashMany(List<Bytes> commitments) throws Exception;
}
