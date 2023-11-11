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
package org.hyperledger.besu.ethereum.trie.verkle.visitor;

import org.hyperledger.besu.ethereum.trie.verkle.node.InternalNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.LeafNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.Node;
import org.hyperledger.besu.ethereum.trie.verkle.node.NullLeafNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.NullNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.StemNode;

import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;

/**
 * A visitor for removing nodes in a Verkle Trie while preserving its structure.
 *
 * <p>This class implements the PathNodeVisitor interface and is used to visit and remove nodes in
 * the Verkle Trie while maintaining the Trie's structural integrity.
 *
 * @param <V> The type of values associated with the nodes.
 */
public class RemoveVisitor<V> implements PathNodeVisitor<V> {
  private final Node<V> NULL_NODE = NullNode.instance();
  private final Node<V> NULL_LEAF_NODE = NullLeafNode.instance();
  private final FlattenVisitor<V> flatten = new FlattenVisitor<>();
  private final GetVisitor<V> getter = new GetVisitor<>();

  /**
   * Visits a internal node to remove a node associated with the provided path and maintain the
   * Trie's structure.
   *
   * @param internalNode The internal node to visit.
   * @param path The path associated with the node to be removed.
   * @return The updated internal node with the removed node and preserved structure.
   */
  @Override
  public Node<V> visit(InternalNode<V> internalNode, Bytes path) {
    final byte index = path.get(0);
    final Node<V> childNode = internalNode.child(index);
    final Node<V> updatedChild = childNode.accept(this, path.slice(1));
    internalNode.replaceChild(index, updatedChild);
    final boolean wasChildNullified = (childNode != NULL_NODE && updatedChild == NULL_NODE);
    if (updatedChild.isDirty() || wasChildNullified) {
      internalNode.markDirty();
    }
    final Optional<Byte> onlyChildIndex = findOnlyChild(internalNode);
    if (onlyChildIndex.isEmpty()) {
      return internalNode;
    }
    final Node<V> newNode = internalNode.child(onlyChildIndex.get()).accept(flatten);
    if (newNode != NULL_NODE) { // Flatten StemNode one-level up
      newNode.markDirty();
      return newNode;
    }
    return internalNode; // Unique child was a internalNode, do nothing
  }

  /**
   * Visits a stem node to remove a node associated with the provided path and maintain the Trie's
   * structure.
   *
   * @param stemNode The stem node to visit.
   * @param path The path associated with the node to be removed.
   * @return The updated branch node with the removed node and preserved structure.
   */
  @Override
  public Node<V> visit(StemNode<V> stemNode, Bytes path) {
    final byte childIndex = path.get(path.size() - 1);
    final Node<V> child = stemNode.child(childIndex);
    final Node<V> newChild = child.accept(this, path);
    stemNode.replaceChild(childIndex, newChild);
    if (allLeavesAreNull(stemNode)) {
      return NULL_NODE;
    }
    if (child != NULL_LEAF_NODE) { // Removed a genuine leaf-node
      stemNode.markDirty();
    }
    return stemNode;
  }

  /**
   * Visits a leaf node to remove a node associated with the provided path and maintain the Trie's
   * structure.
   *
   * @param leafNode The leaf node to visit.
   * @param path The path associated with the node to be removed.
   * @return A null node, indicating the removal of the node.
   */
  @Override
  public Node<V> visit(LeafNode<V> leafNode, Bytes path) {
    return NULL_LEAF_NODE;
  }

  /**
   * Visits a null node and returns a null node, indicating that no removal is required.
   *
   * @param nullNode The null node to visit.
   * @param path The path associated with the removal (no operation).
   * @return A null node, indicating no removal is needed.
   */
  @Override
  public Node<V> visit(NullNode<V> nullNode, Bytes path) {
    return NULL_NODE;
  }

  /**
   * Visits a null leaf node and returns a null node, indicating that no removal is required.
   *
   * @param nullLeafNode The null node to visit.
   * @param path The path associated with the removal (no operation).
   * @return A null node, indicating no removal is needed.
   */
  @Override
  public Node<V> visit(NullLeafNode<V> nullLeafNode, Bytes path) {
    return NULL_LEAF_NODE;
  }

  /**
   * Finds the index of the only non-null child in the list of children nodes.
   *
   * @param branchNode BranchNode to scan for unique child.
   * @return The index of the only non-null child if it exists, or an empty optional if there is no
   *     or more than one non-null child.
   */
  Optional<Byte> findOnlyChild(final InternalNode<V> branchNode) {
    final List<Node<V>> children = branchNode.getChildren();
    Optional<Byte> onlyChildIndex = Optional.empty();
    for (int i = 0; i < children.size(); ++i) {
      if (children.get(i) != NULL_NODE) {
        if (onlyChildIndex.isPresent()) {
          return Optional.empty();
        }
        onlyChildIndex = Optional.of((byte) i);
      }
    }
    return onlyChildIndex;
  }

  boolean allLeavesAreNull(final StemNode<V> stemNode) {
    final List<Node<V>> children = stemNode.getChildren();
    for (int i = 0; i < children.size(); ++i) {
      Node<V> child =
          children.get(i).accept(getter, Bytes.EMPTY); // forces to load node if StoredNode;
      stemNode.replaceChild((byte) i, child);
      if (child != NULL_LEAF_NODE) {
        return false;
      }
    }
    return true;
  }
}
