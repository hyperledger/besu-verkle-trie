/*
 * Copyright Besu Contributors
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

  private static final long committerPointer =
      LibIpaMultipoint.committerPointer(new LibIpaMultipoint());

  /**
   * Commits an array of Bytes32 using the pedersen commitment - multi scalar multiplication vector
   * commitment algorithm.
   *
   * @param inputs An array of Bytes32 inputs to be hashed and committed.
   * @return The resulting hash as a Bytes32.
   */
  @Override
  public Bytes32 commit(Bytes32[] inputs) {
    Bytes input_serialized = Bytes.concatenate(inputs);
    return Bytes32.wrap(LibIpaMultipoint.commit(input_serialized.toArray(), committerPointer));
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
    Bytes32 addr = Bytes32.leftPad(address);
    Bytes input = Bytes.concatenate(addr, index);
    return Bytes32.wrap(LibIpaMultipoint.pedersenHash(input.toArray()));
  }
}
