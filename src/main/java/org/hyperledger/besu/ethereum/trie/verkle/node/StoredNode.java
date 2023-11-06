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

public class StoredNode<V> implements Node<V> {
  private final Bytes location;
  private final NodeFactory<V> nodeFactory;
  private Optional<Node<V>> loadedNode;

  private boolean dirty = true; // not persisted

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
   * Get the path associated with the node.
   *
   * @return The path of the node.
   */
  @Override
  public Bytes getPath() {
    final Node<V> node = load();
    return node.getPath();
  }
  ;

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
   * Replace the path of the node.
   *
   * @param path The new path to set.
   * @return A new node with the updated path.
   */
  @Override
  public Node<V> replacePath(Bytes path) {
    final Node<V> node = load();
    markDirty();
    loadedNode = Optional.of(node.replacePath(path));
    return loadedNode.get();
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
    return node.print();
  }

  private Node<V> load() {
    if (!loadedNode.isPresent()) {
      loadedNode = nodeFactory.retrieve(location, null);
    }
    return loadedNode.orElse(NullNode.instance());
  }
}
