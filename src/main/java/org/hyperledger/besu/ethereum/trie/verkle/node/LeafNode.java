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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.rlp.RLP;
import org.apache.tuweni.rlp.RLPWriter;

/**
 * Represents a leaf node in the Verkle Trie.
 *
 * @param <V> The type of the node's value.
 */
public class LeafNode<V> implements Node<V> {
  private final Optional<Bytes> location; // Location in the tree, or the key
  private final V value; // Value associated with the node
  private Optional<Bytes> encodedValue = Optional.empty(); // Encoded value
  private final Function<V, Bytes> valueSerializer; // Serializer function for the value
  private boolean dirty = true; // not persisted

  /**
   * Constructs a new LeafNode with location, value.
   *
   * @param location The location of the node in the tree.
   * @param value The value associated with the node.
   */
  public LeafNode(final Bytes location, final V value) {
    this.location = Optional.of(location);
    this.value = value;
    this.valueSerializer = val -> (Bytes) val;
  }

  /**
   * Constructs a new LeafNode with optional location, value.
   *
   * @param location The location of the node in the tree (Optional).
   * @param value The value associated with the node.
   */
  public LeafNode(final Optional<Bytes> location, final V value) {
    this.location = location;
    this.value = value;
    this.valueSerializer = val -> (Bytes) val;
  }

  /**
   * Accepts a visitor for path-based operations on the node.
   *
   * @param visitor The path node visitor.
   * @param path The path associated with a node.
   * @return The result of the visitor's operation.
   */
  @Override
  public Node<V> accept(final PathNodeVisitor<V> visitor, final Bytes path) {
    return visitor.visit(this, path);
  }

  /**
   * Accepts a visitor for generic node operations.
   *
   * @param visitor The node visitor.
   * @return The result of the visitor's operation.
   */
  @Override
  public Node<V> accept(final NodeVisitor<V> visitor) {
    return visitor.visit(this);
  }

  /**
   * Get the value associated with the node.
   *
   * @return An optional containing the value of the node if available.
   */
  @Override
  public Optional<V> getValue() {
    return Optional.ofNullable(value);
  }

  /**
   * Get the location of the node.
   *
   * @return An optional containing the location of the node if available.
   */
  @Override
  public Optional<Bytes> getLocation() {
    return location;
  }

  /**
   * Get the children of the node. A leaf node does not have children, so this method throws an
   * UnsupportedOperationException.
   *
   * @return The list of children nodes (unsupported operation).
   * @throws UnsupportedOperationException if called on a leaf node.
   */
  @Override
  public List<Node<V>> getChildren() {
    throw new UnsupportedOperationException("LeafNode does not have children.");
  }

  /**
   * Get the RLP-encoded value of the node.
   *
   * @return The RLP-encoded value.
   */
  @Override
  public Bytes getEncodedValue() {
    if (encodedValue.isPresent()) {
      return encodedValue.get();
    }
    Bytes encodedVal =
        getValue().isPresent() ? valueSerializer.apply(getValue().get()) : Bytes.EMPTY;
    List<Bytes> values = Arrays.asList(encodedVal);
    Bytes result = RLP.encodeList(values, RLPWriter::writeValue);
    this.encodedValue = Optional.of(result);
    return result;
  }

  /** Marks the node as needing to be persisted. */
  @Override
  public void markDirty() {
    dirty = true;
  }

  /**
   * Checks if the node needs to be persisted.
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
    return "Leaf:" + getValue().map(Object::toString).orElse("empty");
  }

  /**
   * Generates DOT representation for the LeafNode.
   *
   * @return DOT representation of the LeafNode.
   */
  @Override
  public String toDot(Boolean showRepeatingEdges) {
    Bytes locationBytes = getLocation().orElse(Bytes.EMPTY);

    return new StringBuilder()
        .append(getClass().getSimpleName())
        .append(locationBytes)
        .append("[location=\"")
        .append(locationBytes)
        .append("\", suffix=\"")
        .append(locationBytes.get(locationBytes.size() - 1))
        .append("\", value=\"")
        .append(getValue().orElse(null))
        .append("\"]\n")
        .toString();
  }
}
