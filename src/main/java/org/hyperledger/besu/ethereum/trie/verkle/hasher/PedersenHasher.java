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

import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.bytes.MutableBytes;

/**
 * A class responsible for hashing an array of Bytes32 using the pedersen commitment - multi scalar
 * multiplication vector commitment algorithm.
 *
 * <p>This class implements the Hasher interface and provides a method to commit multiple Bytes32
 * inputs using the pedersen commitment - multi scalar multiplication vector commitment algorithm.
 */
@SuppressWarnings("null")
public class PedersenHasher implements Hasher {
  private static final Bytes defaultCommitment;
  private static final Bytes32 defaultScalar;

  static {
    // TODO: include defaults in LibIpaMultipoint
    // defaultCommitment = Bytes.wrap(LibIpaMultipoint.defaultCommitment());
    // defaultScalar = Bytes32.wrap(LibIpaMultipoint.defaultScalar());
    defaultCommitment =
        Bytes.concatenate(Bytes32.ZERO, Bytes32.rightPad(Bytes.fromHexString("0x01")));
    defaultScalar = Bytes32.ZERO;
  }

  @Override
  public Bytes getDefaultCommitment() {
    return defaultCommitment;
  }

  @Override
  public Bytes32 getDefaultScalar() {
    return defaultScalar;
  }

  @Override
  public Bytes commit(Bytes[] scalars) throws Exception {
    return Bytes.wrap(LibIpaMultipoint.commit(prepareScalars(scalars).toArray()));
  }

  @Override
  public Bytes32 commitAsCompressed(Bytes[] scalars) throws Exception {
    return Bytes32.wrap(LibIpaMultipoint.commitAsCompressed(prepareScalars(scalars).toArray()));
  }

  @Override
  public Bytes updateSparse(
      final Optional<Bytes> commitment,
      final List<Byte> indices,
      final List<Bytes> oldScalars,
      final List<Bytes> newScalars)
      throws Exception {
    Bytes cmnt = commitment.orElse(defaultCommitment);
    byte[] idx = new byte[indices.size()];
    for (int i = 0; i < indices.size(); i++) {
      idx[i] = (byte) indices.get(i);
    }
    return Bytes.wrap(
        LibIpaMultipoint.updateSparse(
            cmnt.toArray(),
            idx,
            prepareScalars(oldScalars.toArray(new Bytes[oldScalars.size()])).toArray(),
            prepareScalars(newScalars.toArray(new Bytes[newScalars.size()])).toArray()));
  }

  @Override
  public Bytes32 compress(final Bytes commitment) throws Exception {
    return Bytes32.wrap(LibIpaMultipoint.compress(commitment.toArray()));
  }

  // @Override
  // public Bytes32 compressMany(final Bytes commitment) throws Exception {
  //   if ( commitment.size != 64 ) { throw new IllegalArgumentException(); }
  //   return Bytes32.wrap(LibIpaMultipoint.compressCommitment(commitment.toArray()));
  // }

  @Override
  public Bytes32 hash(Bytes commitment) throws Exception {
    return Bytes32.wrap(LibIpaMultipoint.hash(commitment.toArray()));
  }

  @Override
  public Bytes32[] hashMany(List<Bytes> commitments) throws Exception {
    Bytes input = prepareCommitments(commitments.toArray(new Bytes[commitments.size()]));
    byte[] rawScalars = LibIpaMultipoint.hashMany(input.toArray());
    Bytes32[] scalars = new Bytes32[rawScalars.length / 32];
    for (int i = 0; i < scalars.length; i++) {
      scalars[i] = Bytes32.wrap(rawScalars, i * 32);
    }
    return scalars;
  }

  // Protected methods

  Bytes rightPadInput(int size, Bytes[] inputs) throws Exception {
    MutableBytes result = MutableBytes.create(inputs.length * size);
    for (int i = 0; i < inputs.length; i++) {
      int offset = i * size;
      if (inputs[i].size() > size) {
        throw new IllegalArgumentException();
      }
      result.set(offset, inputs[i]);
    }
    return result;
  }

  Bytes prepareScalars(Bytes[] inputs) throws Exception {
    if (inputs == null || inputs.length == 0) {
      throw new IllegalArgumentException();
    }
    return rightPadInput(32, inputs);
  }

  Bytes prepareCommitments(Bytes[] inputs) throws Exception {
    if (inputs == null || inputs.length == 0) {
      throw new IllegalArgumentException();
    }
    return rightPadInput(64, inputs);
  }
}
