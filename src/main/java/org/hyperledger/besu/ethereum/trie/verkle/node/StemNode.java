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
 * Represents a stem node in the Verkle Trie.
 *
 * <p>StemNodes are nodes storing the stem of the key, and is the root of the suffix to value trie.
 *
 * @param <V> The type of the node's value.
 */
public class StemNode<V> extends BranchNode<V> {
  private final Node<V> NULL_LEAF_NODE = NullLeafNode.instance();

  private final Bytes stem;
  private final Optional<Bytes32> leftHash;
  private final Optional<Bytes32> leftCommitment;
  private final Optional<Bytes32> rightHash;
  private final Optional<Bytes32> rightCommitment;
  private Optional<Bytes> encodedValue = Optional.empty();

  /**
   * Constructs a new StemNode with non-optional parameters.
   *
   * @param location The location in the tree.
   * @param stem Node's stem.
   * @param hash Node's vector commitment's hash.
   * @param commitment Node's vector commitment.
   * @param leftHash Hash of vector commitment to left values.
   * @param leftCommitment Vector commitment to left values.
   * @param rightHash Hash of vector commitment to right values.
   * @param rightCommitment Vector commitment to right values.
   * @param children The list of children nodes.
   */
  public StemNode(
      final Bytes location,
      final Bytes stem,
      final Bytes32 hash,
      final Bytes32 commitment,
      final Bytes32 leftHash,
      final Bytes32 leftCommitment,
      final Bytes32 rightHash,
      final Bytes32 rightCommitment,
      final List<Node<V>> children) {
    super(location, hash, commitment, children);
    this.stem = extractStem(stem);
    this.leftHash = Optional.of(leftHash);
    this.leftCommitment = Optional.of(leftCommitment);
    this.rightHash = Optional.of(rightHash);
    this.rightCommitment = Optional.of(rightCommitment);
  }

  /**
   * Constructs a new StemNode with optional parameters.
   *
   * @param location Optional location in the tree.
   * @param stem Node's stem.
   * @param hash Optional node's vector commitment's hash.
   * @param commitment Optional node's vector commitment.
   * @param leftHash Optional hash of vector commitment to left values.
   * @param leftCommitment Optional vector commitment to left values.
   * @param rightHash Optional hash of vector commitment to right values.
   * @param rightCommitment Optional vector commitment to right values.
   * @param children The list of children nodes.
   */
  public StemNode(
      final Optional<Bytes> location,
      final Bytes stem,
      final Optional<Bytes32> hash,
      final Optional<Bytes32> commitment,
      final Optional<Bytes32> leftHash,
      final Optional<Bytes32> leftCommitment,
      final Optional<Bytes32> rightHash,
      final Optional<Bytes32> rightCommitment,
      final List<Node<V>> children) {
    super(location, hash, commitment, children);
    this.stem = extractStem(stem);
    this.leftHash = leftHash;
    this.leftCommitment = leftCommitment;
    this.rightHash = rightHash;
    this.rightCommitment = rightCommitment;
  }

  /**
   * Constructs a new BranchNode with non-optional parameters.
   *
   * @param location The location in the tree.
   * @param stem Node's stem.
   */
  public StemNode(final Bytes location, final Bytes stem) {
    super(location);
    for (int i = 0; i < maxChild(); i++) {
      replaceChild((byte) i, NULL_LEAF_NODE);
    }
    this.stem = extractStem(stem);
    this.leftHash = Optional.empty();
    this.leftCommitment = Optional.empty();
    this.rightHash = Optional.empty();
    this.rightCommitment = Optional.empty();
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
   * Get the stem.
   *
   * @return the stem.
   */
  public Bytes getStem() {
    return stem;
  }

  /**
   * Get Node's extension.
   *
   * @return the extension path.
   */
  public Optional<Bytes> getPathExtension() {
    return getLocation().map((loc) -> stem.slice(loc.size()));
  }

  /**
   * Get the leftHash.
   *
   * @return the leftHash.
   */
  public Optional<Bytes32> getLeftHash() {
    return leftHash;
  }

  /**
   * Get the stem.
   *
   * @return the stem.
   */
  public Optional<Bytes32> getLeftCommitment() {
    return leftCommitment;
  }

  /**
   * Get the rightHash.
   *
   * @return the rightHash.
   */
  public Optional<Bytes32> getRightHash() {
    return rightHash;
  }

  /**
   * Get the stem.
   *
   * @return the stem.
   */
  public Optional<Bytes32> getRightCommitment() {
    return rightCommitment;
  }

  /**
   * Creates a new node by replacing its location
   *
   * @param location The location in the tree.
   * @return StemNode with new location.
   */
  public StemNode<V> replaceLocation(final Bytes location) {
    return new StemNode<V>(
        Optional.of(location),
        stem,
        getHash(),
        getCommitment(),
        leftHash,
        leftCommitment,
        rightHash,
        rightCommitment,
        getChildren());
  }

  /**
   * Creates a new node by replacing all its commitments
   *
   * @param hash Node's vector commitment hash
   * @param commitment Node's vector commitment
   * @param leftHash Node's left vector commitment hash
   * @param leftCommitment Node's left vector commitment
   * @param rightHash Node's right vector commitment hash
   * @param rightCommitment Node's right vector commitment
   * @return StemNode with new commitments.
   */
  public StemNode<V> replaceHash(
      final Bytes32 hash,
      final Bytes32 commitment,
      final Bytes32 leftHash,
      final Bytes32 leftCommitment,
      final Bytes32 rightHash,
      final Bytes32 rightCommitment) {
    return new StemNode<V>(
        getLocation().get(),
        stem,
        hash,
        commitment,
        leftHash,
        leftCommitment,
        rightHash,
        rightCommitment,
        getChildren());
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
    List<Bytes> values =
        Arrays.asList(
            getStem(),
            (Bytes) getHash().get(),
            (Bytes) getCommitment().get(),
            (Bytes) getLeftHash().get(),
            (Bytes) getLeftCommitment().get(),
            (Bytes) getRightHash().get(),
            (Bytes) getRightCommitment().get());
    Bytes result = RLP.encodeList(values, RLPWriter::writeValue);
    this.encodedValue = Optional.of(result);
    return result;
  }

  /**
   * Generates a string representation of the stem node and its children.
   *
   * @return A string representing the stem node and its children.
   */
  @Override
  public String print() {
    final StringBuilder builder = new StringBuilder();
    builder.append(String.format("Stem: %s", stem));
    for (int i = 0; i < maxChild(); i++) {
      final Node<V> child = child((byte) i);
      if (child != NullLeafNode.instance()) {
        final String label = String.format("[%02x] ", i);
        final String childRep = child.print().replaceAll("\n\t", "\n\t\t");
        builder.append("\n\t").append(label).append(childRep);
      }
    }
    return builder.toString();
  }

  private Bytes extractStem(final Bytes stemValue) {
    return stemValue.slice(0, 31);
  }

  @Override
  public String toDot(Boolean showRepeatingEdges) {
    StringBuilder result =
        new StringBuilder()
            .append(getClass().getSimpleName())
            .append(getLocation().orElse(Bytes.EMPTY))
            .append("[location=\"")
            .append(getLocation().orElse(Bytes.EMPTY))
            .append("\", stem=\"")
            .append(getStem())
            .append("\", leftCommitment=\"")
            .append(getLeftCommitment().orElse(Bytes32.ZERO))
            .append("\", rightCommitment=\"")
            .append(getRightCommitment().orElse(Bytes32.ZERO))
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
