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
package org.hyperledger.besu.ethereum.trie.verkle.factory;

import org.hyperledger.besu.ethereum.trie.NodeLoader;
import org.hyperledger.besu.ethereum.trie.verkle.node.InternalNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.LeafNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.Node;
import org.hyperledger.besu.ethereum.trie.verkle.node.StemNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.StoredNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.rlp.RLP;

/** Node types that are saved to storage. */
enum NodeType {
  ROOT,
  INTERNAL,
  STEM,
  LEAF
}

/**
 * A factory for creating Verkle Trie nodes based on stored data.
 *
 * @param <V> The type of values stored in Verkle Trie nodes.
 */
public class StoredNodeFactory<V> implements NodeFactory<V> {
  private final NodeLoader nodeLoader;
  private final Function<Bytes, V> valueDeserializer;

  /**
   * Creates a new StoredNodeFactory with the given node loader and value deserializer.
   *
   * @param nodeLoader The loader for retrieving stored nodes.
   * @param valueDeserializer The function to deserialize values from Bytes.
   */
  public StoredNodeFactory(NodeLoader nodeLoader, Function<Bytes, V> valueDeserializer) {
    this.nodeLoader = nodeLoader;
    this.valueDeserializer = valueDeserializer;
  }

  /**
   * Retrieves a Verkle Trie node from stored data based on the location and hash.
   *
   * @param location Node's location
   * @param hash Node's hash
   * @return An optional containing the retrieved node, or an empty optional if the node is not
   *     found.
   */
  @Override
  public Optional<Node<V>> retrieve(final Bytes location, final Bytes32 hash) {
    /* Currently, Root and Leaf are distinguishable by location.
     * To distinguish internal from stem, we further need values.
     * Currently, they are distinguished by values length.
     */
    Optional<Bytes> optionalEncodedValues = nodeLoader.getNode(location, hash);
    if (optionalEncodedValues.isEmpty()) {
      return Optional.empty();
    }
    Bytes encodedValues = optionalEncodedValues.get();
    List<Bytes> values = RLP.decodeToList(encodedValues, reader -> reader.readValue().copy());
    final int locLength = location.size();
    final int nValues = values.size();
    NodeType type =
        (locLength == 32 ? NodeType.LEAF : (nValues == 2 ? NodeType.INTERNAL : NodeType.STEM));
    return switch (type) {
      case LEAF -> Optional.of(createLeafNode(location, values));
      case INTERNAL -> Optional.of(createInternalNode(location, values));
      case STEM -> Optional.of(createStemNode(location, values));
      default -> Optional.empty();
    };
  }

  /**
   * Creates a internalNode using the provided location, hash, and path.
   *
   * @param location The location of the internalNode.
   * @param values List of Bytes values retrieved from storage.
   * @return A internalNode instance.
   */
  InternalNode<V> createInternalNode(Bytes location, List<Bytes> values) {
    final int nChild = InternalNode.maxChild();
    ArrayList<Node<V>> children = new ArrayList<Node<V>>(nChild);
    for (int i = 0; i < nChild; i++) {
      children.add(new StoredNode<>(this, Bytes.concatenate(location, Bytes.of(i))));
    }
    final Bytes32 hash = (Bytes32) values.get(0);
    final Bytes32 commitment = (Bytes32) values.get(1);
    return new InternalNode<V>(location, hash, commitment, children);
  }

  /**
   * Creates a BranchNode using the provided location, hash, and path.
   *
   * @param location The location of the BranchNode.
   * @param values List of Bytes values retrieved from storage.
   * @return A BranchNode instance.
   */
  StemNode<V> createStemNode(Bytes location, List<Bytes> values) {
    final int nChild = StemNode.maxChild();
    final Bytes stem = values.get(0);
    final Bytes32 hash = (Bytes32) values.get(1);
    final Bytes32 commitment = (Bytes32) values.get(2);
    final Bytes32 leftHash = (Bytes32) values.get(3);
    final Bytes32 leftCommitment = (Bytes32) values.get(4);
    final Bytes32 rightHash = (Bytes32) values.get(5);
    final Bytes32 rightCommitment = (Bytes32) values.get(6);
    ArrayList<Node<V>> children = new ArrayList<Node<V>>(nChild);
    for (int i = 0; i < nChild; i++) {
      children.add(new StoredNode<>(this, Bytes.concatenate(stem, Bytes.of(i))));
    }
    return new StemNode<V>(
        location,
        stem,
        hash,
        commitment,
        leftHash,
        leftCommitment,
        rightHash,
        rightCommitment,
        children);
  }

  /**
   * Creates a LeafNode using the provided location, path, and value.
   *
   * @param key The key of the LeafNode.
   * @param values List of Bytes values retrieved from storage.
   * @return A LeafNode instance.
   */
  LeafNode<V> createLeafNode(Bytes key, List<Bytes> values) {
    V value = valueDeserializer.apply(values.get(0));
    return new LeafNode<V>(Optional.of(key), value);
  }
}
