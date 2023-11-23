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
package org.hyperledger.besu.ethereum.trie.verkle.node;

import org.hyperledger.besu.ethereum.trie.verkle.visitor.NodeVisitor;
import org.hyperledger.besu.ethereum.trie.verkle.visitor.PathNodeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

/**
 * Represents a branch node in the Verkle Trie.
 *
 * @param <V> The type of the node's value.
 */
public abstract class BranchNode<V> implements Node<V> {
  private final Optional<Bytes> location; // Location in the tree
  private final Optional<Bytes32> hash; // Vector commitment's hash
  private final Optional<Bytes32> commitment; // Vector commitment serialized
  private final List<Node<V>> children; // List of children nodes

  private boolean dirty = true; // not persisted

  /**
   * Constructs a new BranchNode with location, hash, path, and children.
   *
   * @param location The location in the tree.
   * @param hash Node's vector commitment's hash.
   * @param commitment Node's vector commitment.
   * @param children The list of children nodes.
   */
  public BranchNode(
      final Bytes location,
      final Bytes32 hash,
      final Bytes32 commitment,
      final List<Node<V>> children) {
    assert (children.size() == maxChild());
    this.location = Optional.of(location);
    this.hash = Optional.of(hash);
    this.commitment = Optional.of(commitment);
    this.children = children;
  }

  /**
   * Constructs a new BranchNode with optional location, optional hash, optional commitment and
   * children.
   *
   * @param location The optional location in the tree.
   * @param hash The optional vector commitment of children's commitments.
   * @param commitment Node's optional vector commitment.
   * @param children The list of children nodes.
   */
  public BranchNode(
      final Optional<Bytes> location,
      final Optional<Bytes32> hash,
      final Optional<Bytes32> commitment,
      final List<Node<V>> children) {
    assert (children.size() == maxChild());
    this.location = location;
    this.hash = hash;
    this.commitment = commitment;
    this.children = children;
  }

  /**
   * Constructs a new BranchNode with optional location, path, and children.
   *
   * @param location The optional location in the tree.
   * @param children The list of children nodes.
   */
  public BranchNode(final Optional<Bytes> location, final List<Node<V>> children) {
    assert (children.size() == maxChild());
    this.location = location;
    this.children = children;
    hash = Optional.empty();
    commitment = Optional.empty();
  }

  /**
   * Constructs a new BranchNode with optional location and path, initializing children to
   * NullNodes.
   *
   * @param location The optional location in the tree.
   */
  public BranchNode(final Bytes location) {
    this.location = Optional.of(location);
    this.children = new ArrayList<>();
    for (int i = 0; i < maxChild(); i++) {
      children.add(NullNode.instance());
    }
    hash = Optional.of(EMPTY_HASH);
    commitment = Optional.of(EMPTY_COMMITMENT);
  }

  /**
   * Get the maximum number of children nodes (256 for byte indexes).
   *
   * @return The maximum number of children nodes.
   */
  public static int maxChild() {
    return 256;
  }

  /**
   * Accepts a visitor for path-based operations on the node.
   *
   * @param visitor The path node visitor.
   * @param path The path associated with a node.
   * @return The result of the visitor's operation.
   */
  @Override
  public abstract Node<V> accept(PathNodeVisitor<V> visitor, Bytes path);

  /**
   * Accepts a visitor for generic node operations.
   *
   * @param visitor The node visitor.
   * @return The result of the visitor's operation.
   */
  @Override
  public abstract Node<V> accept(final NodeVisitor<V> visitor);

  /**
   * Get the child node at a specified index.
   *
   * @param childIndex The index of the child node.
   * @return The child node.
   */
  public Node<V> child(final byte childIndex) {
    return children.get(Byte.toUnsignedInt(childIndex));
  }

  /**
   * Replaces the child node at a specified index with a new node.
   *
   * @param index The index of the child node to replace.
   * @param childNode The new child node.
   */
  public void replaceChild(final byte index, final Node<V> childNode) {
    children.set(Byte.toUnsignedInt(index), childNode);
  }

  /**
   * Get the vector commitment's hash of child commitment hashes.
   *
   * @return An optional containing the vector commitment.
   */
  @Override
  public Optional<Bytes32> getHash() {
    return hash;
  }

  /**
   * Get the vector commitment's hash of child commitment hashes.
   *
   * @return An optional containing the vector commitment.
   */
  @Override
  public Optional<Bytes32> getCommitment() {
    return commitment;
  }

  /**
   * Get the location in the tree.
   *
   * @return An optional containing the location if available.
   */
  @Override
  public Optional<Bytes> getLocation() {
    return location;
  }

  /**
   * Get the RLP-encoded value of the node.
   *
   * @return The RLP-encoded value.
   */
  @Override
  public abstract Bytes getEncodedValue();

  /**
   * Get the list of children nodes.
   *
   * @return The list of children nodes.
   */
  @Override
  public List<Node<V>> getChildren() {
    return children;
  }

  /** Marks the node as dirty, indicating that it needs to be persisted. */
  @Override
  public void markDirty() {
    dirty = true;
  }

  /**
   * Checks if the node is dirty, indicating that it needs to be persisted.
   *
   * @return `true` if the node is marked as dirty, `false` otherwise.
   */
  @Override
  public boolean isDirty() {
    return dirty;
  }

  /**
   * Generates a string representation of the branch node and its children.
   *
   * @return A string representing the branch node and its children.
   */
  @Override
  public String print() {
    final StringBuilder builder = new StringBuilder();
    builder.append("Branch:");
    for (int i = 0; i < maxChild(); i++) {
      final Node<V> child = child((byte) i);
      if (!Objects.equals(child, NullNode.instance())) {
        final String branchLabel = "[" + Integer.toHexString(i) + "] ";
        final String childRep = child.print().replaceAll("\n\t", "\n\t\t");
        builder.append("\n\t").append(branchLabel).append(childRep);
      }
    }
    return builder.toString();
  }

  /**
   * Generates DOT representation for the BranchNode.
   *
   * @return DOT representation of the BranchNode.
   */
  @Override
  public String toDot(Boolean showRepeatingEdges) {
    StringBuilder result =
        new StringBuilder()
            .append(getClass().getSimpleName())
            .append(getLocation().orElse(Bytes.EMPTY))
            .append("\", location=\"")
            .append(getLocation().orElse(Bytes.EMPTY))
            .append("\", commitment=\"")
            .append(getHash().orElse(Bytes32.ZERO))
            .append("\"]\n");

    for (Node<V> child : getChildren()) {
      String edgeString =
          getClass().getSimpleName()
              + getLocation().orElse(Bytes.EMPTY)
              + " -> "
              + child.getClass().getSimpleName()
              + child.getLocation().orElse(Bytes.EMPTY)
              + "\n";

      if (showRepeatingEdges || !result.toString().contains(edgeString)) {
        result.append(edgeString);
      }
      result.append(child.toDot(showRepeatingEdges));
    }

    return result.toString();
  }
}
