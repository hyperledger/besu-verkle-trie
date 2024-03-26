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
package org.hyperledger.besu.ethereum.trie.verkle.visitor;

import org.hyperledger.besu.ethereum.trie.verkle.hasher.Hasher;
import org.hyperledger.besu.ethereum.trie.verkle.hasher.PedersenHasher;
import org.hyperledger.besu.ethereum.trie.verkle.node.InternalNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.Node;
import org.hyperledger.besu.ethereum.trie.verkle.node.StemNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

/**
 * A visitor class for hashing operations on Verkle Trie nodes.
 *
 * @param <V> The type of the node's value.
 */
public class HashVisitor<V extends Bytes> implements PathNodeVisitor<V> {
  Hasher hasher = new PedersenHasher();

  /**
   * Visits a internal node, computes its hash, and returns a new internal node with the updated
   * hash.
   *
   * @param internalNode The internal node to visit.
   * @param location The location associated with the internal node.
   * @return A new internal node with the updated hash.
   */
  @Override
  public Node<V> visit(InternalNode<V> internalNode, Bytes location) {
    if (!internalNode.isDirty()
        && internalNode.getHash().isPresent()
        && internalNode.getCommitment().isPresent()) {
      return internalNode;
    }

    List<Byte> indices = new ArrayList<Byte>();
    List<Bytes> oldScalars = new ArrayList<Bytes>();
    List<Bytes> newScalars = new ArrayList<Bytes>();
    int size = InternalNode.maxChild();

    for (int i = 0; i < size; i++) {
      byte index = (byte) i;
      Node<V> child = internalNode.child(index);
      Bytes nextLocation = Bytes.concatenate(location, Bytes.of(index));
      Node<V> updatedChild = child.accept(this, nextLocation);
      internalNode.replaceChild(index, updatedChild);

      Bytes32 oldHash = child.getHash().orElse(hasher.getDefaultScalar());
      Bytes32 newHash = updatedChild.getHash().orElse(hasher.getDefaultScalar());
      if (oldHash != newHash) {
        indices.add((Byte) index);
        oldScalars.add((Bytes) oldHash);
        newScalars.add((Bytes) newHash);
      }
    }
    if (indices.size() == 0) {
      return internalNode;
    }

    final Bytes32 hash;
    Optional<Bytes> previousCommitment = internalNode.getCommitment();
    try {
      Bytes commitment = hasher.updateSparse(previousCommitment, indices, oldScalars, newScalars);
      // Should modify soon
      if (location.isEmpty()) { // Root node
        hash = hasher.compress(commitment);
      } else { // Proper internal node
        hash = hasher.hash(commitment);
      }
      return internalNode.replaceHash(hash, commitment);
    } catch (Exception e) {
      return internalNode;
    }
  }

  /**
   * Visits a branch node, computes its hash, and returns a new branch node with the updated hash.
   *
   * @param stemNode The branch node to visit.
   * @param location The location associated with the branch node.
   * @return A new branch node with the updated hash.
   */
  @Override
  public Node<V> visit(StemNode<V> stemNode, Bytes location) {
    if (!stemNode.isDirty()
        && stemNode.getHash().isPresent()
        && stemNode.getCommitment().isPresent()) {
      return stemNode;
    }

    Optional<Bytes> leftCommitment = getSubCommitment(0, stemNode);
    Optional<Bytes> rightCommitment = getSubCommitment(1, stemNode);
    Optional<Bytes> oldCommitment = stemNode.getCommitment();
    Bytes commitment;
    Bytes32 leftHash = hasher.getDefaultScalar();
    Bytes32 rightHash = hasher.getDefaultScalar();
    if (oldCommitment.isPresent()) { // Update subcommitments only
      List<Byte> indices = new ArrayList<Byte>();
      List<Bytes> oldScalars = new ArrayList<Bytes>();
      List<Bytes> newScalars = new ArrayList<Bytes>();
      if (leftCommitment.isPresent()) {
        indices.add((Byte) (byte) 2);
        Optional<Bytes> maybeOldLeft = stemNode.getLeftCommitment();
        Bytes oldLeft = hasher.getDefaultScalar();
        if (maybeOldLeft.isPresent()) {
          try {
            oldLeft = hasher.hash(maybeOldLeft.get());
          } catch (Exception e) {
            // Should let it bubble up
          }
        }
        oldScalars.add(oldLeft);
        try {
          leftHash = hasher.hash(leftCommitment.get());
        } catch (Exception e) {
          // Should let it bubble up
        }
        newScalars.add(leftHash);
      }
      if (rightCommitment.isPresent()) {
        indices.add((Byte) (byte) 3);
        Optional<Bytes> maybeOldRight = stemNode.getRightCommitment();
        Bytes oldRight = hasher.getDefaultScalar();
        if (maybeOldRight.isPresent()) {
          try {
            oldRight = hasher.hash(maybeOldRight.get());
          } catch (Exception e) {
            // Should let it bubble up
          }
        }
        oldScalars.add(oldRight);
        try {
          rightHash = hasher.hash(rightCommitment.get());
        } catch (Exception e) {
          // Should let it bubble up
        }
        newScalars.add(rightHash);
      }
      try {
        commitment = hasher.updateSparse(oldCommitment, indices, oldScalars, newScalars);
      } catch (Exception e) {
        commitment = hasher.getDefaultCommitment();
      }
    } else { // commit also extension marker and stem
      Bytes[] hashes = new Bytes[4];
      hashes[0] = Bytes.of(1); // extension marker
      hashes[1] = stemNode.getStem();
      hashes[2] = leftHash;
      hashes[3] = rightHash;
      try {
        commitment = hasher.commit(hashes);
      } catch (Exception e) {
        // Should bubble up
        commitment = hasher.getDefaultCommitment();
      }
    }
    Bytes32 hash;
    try {
      hash = hasher.hash(commitment);
    } catch (Exception e) {
      // Should bubble up
      hash = hasher.getDefaultScalar();
    }
    return stemNode.replaceHash(
        hash, commitment,
        leftHash, leftCommitment.orElse(hasher.getDefaultCommitment()),
        rightHash, rightCommitment.orElse(hasher.getDefaultCommitment()));
  }

  /**
   * Retrieves the low value part of a given optional value.
   *
   * @param value The optional value.
   * @return The low value.
   */
  Bytes getLowValue(Optional<V> value) {
    // Low values have a 1 flag at bit 128.
    if (value.isPresent()) {
      return Bytes.concatenate(value.get().slice(0, 16), Bytes.of((byte) 1));
    } else {
      return Bytes.wrap(new byte[17]);
    }
  }

  /**
   * Retrieves the high value part of a given optional value.
   *
   * @param value The optional value.
   * @return The high value.
   */
  Bytes getHighValue(Optional<V> value) {
    // High values have a 0 flag at bit 128.
    if (value.isPresent()) {
      return Bytes.concatenate(value.get().slice(16, 16), Bytes.of((byte) 0));
    } else {
      return Bytes.wrap(new byte[17]);
    }
  }

  Optional<Bytes> getSubCommitment(int subIndex, StemNode<V> stemNode) {
    // Get sparse vector to updating subcommitments
    List<Byte> indices = new ArrayList<Byte>();
    List<Bytes> oldScalars = new ArrayList<Bytes>();
    List<Bytes> newScalars = new ArrayList<Bytes>();
    int size = StemNode.maxChild();
    int offset = subIndex * (size / 2);

    for (int i = offset; i < (offset + size / 2); i++) {
      Node<V> child = stemNode.child((byte) i);
      Optional<V> value = child.getValue();
      Optional<V> committedValue = child.getCommittedValue();
      Bytes lowValue = getLowValue(value);
      Bytes lowCommitted = getLowValue(committedValue);
      Bytes highValue = getHighValue(value);
      Bytes highCommitted = getHighValue(committedValue);
      if (!lowValue.equals(lowCommitted) || !highValue.equals(highCommitted)) {
        indices.add((Byte) (byte) (2 * (i - offset)));
        oldScalars.add(lowCommitted);
        newScalars.add(lowValue);
        indices.add((Byte) (byte) (2 * (i + 1 - offset)));
        oldScalars.add(highCommitted);
        newScalars.add(highValue);
      }
    }
    Optional<Bytes> oldCommitment =
        subIndex == 0 ? stemNode.getLeftCommitment() : stemNode.getRightCommitment();
    if (indices.size() == 0) {
      return oldCommitment;
    } else {
      try {
        return Optional.of(hasher.updateSparse(oldCommitment, indices, oldScalars, newScalars));
      } catch (Exception e) {
        // Should bubble up
        return Optional.empty();
      }
    }
  }
}
