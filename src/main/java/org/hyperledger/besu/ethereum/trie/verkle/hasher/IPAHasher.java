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
 * A class responsible for hashing an array of Bytes32 using the IPA (Inner Product Argument)
 * hashing algorithm.
 *
 * <p>This class implements the Hasher interface and provides a method to commit multiple Bytes32
 * inputs using the IPA hashing algorithm.
 */
public class IPAHasher implements Hasher<Bytes32> {
  /**
   * Commits an array of Bytes32 using the IPA hashing algorithm.
   *
   * @param inputs An array of Bytes32 inputs to be hashed and committed.
   * @return The resulting hash as a Bytes32.
   */
  @Override
  public Bytes32 commit(Bytes32[] inputs) {
    Bytes32[] rev = new Bytes32[inputs.length];
    for (int i = 0; i < inputs.length; ++i) {
      rev[i] = (Bytes32) inputs[i].reverse();
    }
    Bytes input_serialized = Bytes.concatenate(rev);
    return (Bytes32) Bytes32.wrap(LibIpaMultipoint.commit(input_serialized.toArray())).reverse();
  }
}
