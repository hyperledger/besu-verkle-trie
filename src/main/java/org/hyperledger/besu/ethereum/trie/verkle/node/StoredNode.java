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

import org.hyperledger.besu.ethereum.trie.verkle.factory.NodeFactory;
import org.hyperledger.besu.ethereum.trie.verkle.visitor.NodeVisitor;
import org.hyperledger.besu.ethereum.trie.verkle.visitor.PathNodeVisitor;

import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

/**
 * Represents a regular node that can possibly be stored in storage.
 *
 * <p>StoredNodes wrap regular nodes and loads them lazily from storage as needed.
 *
 * @param <V> The type of the node's value.
 */
public class StoredNode<V> implements Node<V> {
  private final Bytes location;
  private final NodeFactory<V> nodeFactory;
  private Optional<Node<V>> loadedNode;

  private boolean dirty = true; // not persisted

  /**
   * Constructs a new StoredNode at location.
   *
   * @param nodeFactory The node factory for creating nodes from storage.
   * @param location The location in the tree.
   */
  public StoredNode(final NodeFactory<V> nodeFactory, final Bytes location) {
    this.location = location;
    this.nodeFactory = nodeFactory;
    loadedNode = Optional.empty();
  }

  /**
   * Accept a visitor to perform operations on the node based on a provided path.
   *
   * @param visitor The visitor to accept.
   * @param path The path associated with a node.
   * @return The result of visitor's operation.
   */
  @Override
  public Node<V> accept(PathNodeVisitor<V> visitor, Bytes path) {
    final Node<V> node = load();
    return node.accept(visitor, path);
  }

  /**
   * Accept a visitor to perform operations on the node.
   *
   * @param visitor The visitor to accept.
   * @return The result of the visitor's operation.
   */
  @Override
  public Node<V> accept(NodeVisitor<V> visitor) {
    final Node<V> node = load();
    return node.accept(visitor);
  }

  /**
   * Get the location of the node.
   *
   * @return An optional containing the location of the node if available.
   */
  @Override
  public Optional<Bytes> getLocation() {
    return Optional.of(location);
  }

  /**
   * Get the value associated with the node.
   *
   * @return An optional containing the value of the node if available.
   */
  @Override
  public Optional<V> getValue() {
    final Node<V> node = load();
    return node.getValue();
  }

  /**
   * Get the hash associated with the node.
   *
   * @return An optional containing the hash of the node if available.
   */
  @Override
  public Optional<Bytes32> getHash() {
    final Node<V> node = load();
    return node.getHash();
  }

  /**
   * Get the commitment associated with the node.
   *
   * @return An optional containing the commitment of the node if available.
   */
  @Override
  public Optional<Bytes32> getCommitment() {
    final Node<V> node = load();
    return node.getCommitment();
  }

  /**
   * Get the encoded value of the node.
   *
   * @return The encoded value of the node.
   */
  @Override
  public Bytes getEncodedValue() {
    final Node<V> node = load();
    return node.getEncodedValue();
  }

  /**
   * Get the children nodes of this node.
   *
   * @return A list of children nodes.
   */
  @Override
  public List<Node<V>> getChildren() {
    final Node<V> node = load();
    return node.getChildren();
  }

  /** Marks the node as needs to be persisted */
  @Override
  public void markDirty() {
    dirty = true;
  }

  /**
   * Is this node not persisted and needs to be?
   *
   * @return True if the node needs to be persisted.
   */
  @Override
  public boolean isDirty() {
    return dirty;
  }

  /**
   * Get a string representation of the node.
   *
   * @return A string representation of the node.
   */
  @Override
  public String print() {
    final Node<V> node = load();
    return String.format("(stored) %s", node.print());
  }

  /**
   * Generates DOT representation for the StoredNode.
   *
   * @return DOT representation of the StoredNode.
   */
  @Override
  public String toDot(Boolean showRepeatingEdges) {
    String result =
        getClass().getSimpleName()
            + getLocation().orElse(Bytes.EMPTY)
            + "[location=\""
            + getLocation().orElse(Bytes.EMPTY)
            + "\"]\n";
    return result;
  }

  private Node<V> load() {
    if (loadedNode.isEmpty()) {
      loadedNode = nodeFactory.retrieve(location, null);
    }
    if (loadedNode.isPresent()) {
      return loadedNode.get();
    } else if (location.size() == 32) {
      return NullLeafNode.instance();
    } else {
      return NullNode.instance();
    }
  }
}
