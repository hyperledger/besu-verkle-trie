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

import static org.hyperledger.besu.ethereum.trie.verkle.node.Node.getHighValue;
import static org.hyperledger.besu.ethereum.trie.verkle.node.Node.getLowValue;

import org.hyperledger.besu.ethereum.trie.verkle.hasher.Hasher;
import org.hyperledger.besu.ethereum.trie.verkle.hasher.PedersenHasher;
import org.hyperledger.besu.ethereum.trie.verkle.node.InternalNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.Node;
import org.hyperledger.besu.ethereum.trie.verkle.node.StemNode;

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
    int size = InternalNode.maxChild();
    Bytes32[] hashes = new Bytes32[size];
    for (int i = 0; i < size; i++) {
      byte index = (byte) i;
      Node<V> child = internalNode.child(index);
      Bytes nextLocation = Bytes.concatenate(location, Bytes.of(index));
      Node<V> updatedChild = child.accept(this, nextLocation);
      internalNode.replaceChild(index, updatedChild);
      hashes[i] = updatedChild.getHash().get();
    }
    final Bytes32 hash;
    if (location.isEmpty()) {
      hash = hasher.commitRoot(hashes);
    } else {
      hash = hasher.hash(hasher.commit(hashes));
    }
    return internalNode.replaceHash(hash, hash); // commitment should be different
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
    int size = StemNode.maxChild();
    Bytes32[] hashes = new Bytes32[4];
    Bytes32[] leftValues = new Bytes32[size];
    Bytes32[] rightValues = new Bytes32[size];
    for (int i = 0; i < size / 2; i++) {
      byte index = (byte) i;
      Node<V> child = stemNode.child(index);
      leftValues[2 * i] = getLowValue(child.getValue());
      leftValues[2 * i + 1] = getHighValue(child.getValue());
    }
    for (int i = size / 2; i < size; i++) {
      byte index = (byte) i;
      Node<V> child = stemNode.child(index);
      rightValues[2 * i - size] = getLowValue(child.getValue());
      rightValues[2 * i + 1 - size] = getHighValue(child.getValue());
    }
    hashes[0] = Bytes32.rightPad(Bytes.of(1)); // extension marker
    hashes[1] = Bytes32.rightPad(stemNode.getStem());
    hashes[2] = hasher.hash(hasher.commit(leftValues));
    hashes[3] = hasher.hash(hasher.commit(rightValues));
    final Bytes32 hash = hasher.hash(hasher.commit(hashes));
    return stemNode.replaceHash(
        hash, hash, hashes[2], hashes[2], hashes[3], hashes[3]); // commitment should be different
  }
}
