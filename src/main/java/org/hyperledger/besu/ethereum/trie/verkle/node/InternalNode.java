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

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.rlp.RLP;
import org.apache.tuweni.rlp.RLPWriter;

/**
 * Represents an internal node in the Verkle Trie.
 *
 * @param <V> The type of the node's value.
 */
public class InternalNode<V> extends BranchNode<V> {
  private Optional<Bytes> encodedValue = Optional.empty(); // Encoded value

  /**
   * Constructs a new InternalNode with location, hash, path, and children.
   *
   * @param location The location in the tree.
   * @param hash Node's vector commitment's hash.
   * @param commitment Node's vector commitment.
   * @param children The list of children nodes.
   */
  public InternalNode(
      final Bytes location,
      final Bytes32 hash,
      final Bytes32 commitment,
      final List<Node<V>> children) {
    super(location, hash, commitment, children);
  }

  /**
   * Constructs a new InternalNode with optional location, optional hash, optional commitment and
   * children.
   *
   * @param location The optional location in the tree.
   * @param hash The optional vector commitment of children's commitments.
   * @param commitment Node's optional vector commitment.
   * @param children The list of children nodes.
   */
  public InternalNode(
      final Optional<Bytes> location,
      final Optional<Bytes32> hash,
      final Optional<Bytes32> commitment,
      final List<Node<V>> children) {
    super(location, hash, commitment, children);
  }

  /**
   * Constructs a new InternalNode with optional location, path, and children.
   *
   * @param location The optional location in the tree.
   * @param children The list of children nodes.
   */
  public InternalNode(final Optional<Bytes> location, final List<Node<V>> children) {
    super(location, children);
  }

  /**
   * Constructs a new InternalNode with optional location and path, initializing children to
   * NullNodes.
   *
   * @param location The optional location in the tree.
   */
  public InternalNode(final Bytes location) {
    super(location);
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
   * Replace the vector commitment with a new one.
   *
   * @param hash The new vector commitment's hash to set.
   * @param commitment The new vector commitment to set.
   * @return A new InternalNode with the updated vector commitment.
   */
  public Node<V> replaceHash(Bytes32 hash, Bytes32 commitment) {
    return new InternalNode<V>(
        getLocation(), Optional.of(hash), Optional.of(commitment), getChildren());
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
    List<Bytes> values = Arrays.asList((Bytes) getHash().get(), (Bytes) getCommitment().get());
    Bytes result = RLP.encodeList(values, RLPWriter::writeValue);
    this.encodedValue = Optional.of(result);
    return result;
  }

  /**
   * Generates a string representation of the branch node and its children.
   *
   * @return A string representing the branch node and its children.
   */
  @Override
  public String print() {
    final StringBuilder builder = new StringBuilder();
    builder.append("Internal:");
    for (int i = 0; i < maxChild(); i++) {
      final Node<V> childNode = child((byte) i);
      if (childNode != NullNode.instance()) {
        final String label = String.format("[%02x] ", i);
        final String childRep = childNode.print().replaceAll("\n\t", "\n\t\t");
        builder.append("\n\t").append(label).append(childRep);
      }
    }
    return builder.toString();
  }

  /**
   * Generates DOT representation for the InternalNode.
   *
   * @return DOT representation of the InternalNode.
   */
  @Override
  public String toDot(Boolean showRepeatingEdges) {
    StringBuilder result =
        new StringBuilder()
            .append(getClass().getSimpleName())
            .append(getLocation().orElse(Bytes.EMPTY))
            .append("[location=\"")
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
