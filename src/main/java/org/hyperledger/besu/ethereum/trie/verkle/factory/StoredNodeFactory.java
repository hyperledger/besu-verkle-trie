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
package org.hyperledger.besu.ethereum.trie.verkle.factory;

import org.hyperledger.besu.ethereum.trie.NodeLoader;
import org.hyperledger.besu.ethereum.trie.verkle.node.InternalNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.LeafNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.Node;
import org.hyperledger.besu.ethereum.trie.verkle.node.NullNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.NullLeafNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.StemNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.StoredNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.bytes.MutableBytes;

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
   * Creates a new StoredNodeFactory with the given node loader and value
   * deserializer.
   *
   * @param nodeLoader        The loader for retrieving stored nodes.
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
   * @param hash     Node's hash
   * @return An optional containing the retrieved node, or an empty optional if
   *         the node is not
   *         found.
   */
  @Override
  public Optional<Node<V>> retrieve(final Bytes location, final Bytes32 hash) {
    /*
     * Currently, Root and Leaf are distinguishable by location.
     * To distinguish internal from stem, we further need values.
     * Currently, they are distinguished by values length.
     */
    Optional<Bytes> optionalEncodedValues = nodeLoader.getNode(location, hash);
    if (optionalEncodedValues.isEmpty()) {
      return Optional.empty();
    }
    Bytes encodedValues = optionalEncodedValues.get();
    int indicator = (int) encodedValues.get(0);
    Bytes values = encodedValues.slice(1);
    return switch (indicator) {
      case 0 -> Optional.of(createRootNode(values));
      case 1 -> Optional.of(createInternalNode(location, values, hash));
      case 2 -> Optional.of(createStemNode(location, values, hash));
      default -> Optional.empty();
    };
  }

  private List<Boolean> decodeIsNull(Bytes nullBitMap, int nChild) {
    List<Boolean> isNull = new ArrayList<>(nChild);
    for (int i = 0; i < nChild / 8; i++) {
      int mask = 128;
      for (int j = 8 * i; j < 8 * (i + 1); j++) {
        isNull.add((nullBitMap.get(i) & mask) != 0);
        mask = mask >> 1;
      }
    }
    return isNull;

  }

  private List<Bytes32> decodeScalars(List<Boolean> isNull, Bytes values) {
    int nChild = isNull.size();
    List<Bytes32> scalars = new ArrayList<>(nChild);
    for (int i = 0; i < nChild; i++) {
      if (!isNull.get(i)) {
        Bytes32 scalar = (Bytes32) values.slice(0, 32);
        values = values.slice(32);
        scalars.add(scalar);
      } else {
        scalars.add(Bytes32.ZERO);
      }
    }
    return scalars;
  }

  private List<Bytes> decodeValues(List<Boolean> isNull, Bytes encoded) {
    int nChild = isNull.size();
    List<Bytes> values = new ArrayList<>(nChild);
    for (int i = 0; i < nChild; i++) {
      if (!isNull.get(i)) {
        Bytes value = encoded.slice(0, 32);
        encoded = encoded.slice(32);
        values.add(value);
      } else {
        values.add(Bytes.EMPTY);
      }
    }
    return values;
  }

  /**
   * Creates a rootNode using the provided location, hash, and path.
   *
   * @param encodedValues List of Bytes values retrieved from storage.
   * @return A internalNode instance.
   */
  InternalNode<V> createRootNode(Bytes encodedValues) {
    Bytes32 hash = (Bytes32) encodedValues.slice(0, 32);
    return createInternalNode(Bytes.EMPTY, encodedValues.slice(32), hash);
  }

  /**
   * Creates a internalNode using the provided location, hash, and path.
   *
   * @param location      The location of the internalNode.
   * @param encodedValues List of Bytes values retrieved from storage.
   * @param hash          Node's hash value.
   * @return A internalNode instance.
   */
  InternalNode<V> createInternalNode(Bytes location, Bytes encodedValues, Bytes32 hash) {
    int nChild = InternalNode.maxChild();
    Bytes commitment = encodedValues.slice(0, 64);
    Bytes nullBitMap = encodedValues.slice(64, 32);
    Bytes values = encodedValues.slice(96);

    List<Boolean> isNull = decodeIsNull(nullBitMap, nChild);
    List<Bytes32> scalars = decodeScalars(isNull, values);

    List<Node<V>> children = new ArrayList<>(nChild);
    MutableBytes nextBuffer = MutableBytes.create(location.size() + 1);
    Bytes.concatenate(location, Bytes.of(0)).copyTo(nextBuffer);
    for (int i = 0; i < nChild; i++) {
      nextBuffer.set(location.size(), Bytes.of(i));
      Bytes nextLoc = nextBuffer.copy();
      if (isNull.get(i)) {
        children.add(new NullNode<V>());
      } else {
        children.add(new StoredNode<V>(this, nextLoc, scalars.get(i)));
      }
    }
    return new InternalNode<V>(location, hash, commitment, children);
  }

  /**
   * Creates a BranchNode using the provided location, hash, and path.
   *
   * @param location      The location of the BranchNode.
   * @param encodedValues List of Bytes values retrieved from storage.
   * @param hash          Node's hash value.
   * @return A BranchNode instance.
   */
  StemNode<V> createStemNode(Bytes location, Bytes encodedValues, Bytes32 hash) {
    final int nChild = StemNode.maxChild();
    Bytes stem = encodedValues.slice(0, 31);
    Bytes commitment = encodedValues.slice(31, 64);
    Bytes leftCommitment = encodedValues.slice(95, 64);
    Bytes rightCommitment = encodedValues.slice(159, 64);
    Bytes32 leftHash = (Bytes32) encodedValues.slice(223, 32);
    Bytes32 rightHash = (Bytes32) encodedValues.slice(255, 32);

    Bytes nullBitMap = encodedValues.slice(287, 32);
    Bytes encoded = encodedValues.slice(319);

    List<Boolean> isNull = decodeIsNull(nullBitMap, nChild);
    List<Bytes> values = decodeValues(isNull, encoded);

    List<Node<V>> children = new ArrayList<>(nChild);
    MutableBytes nextBuffer = MutableBytes.create(stem.size() + 1);
    Bytes.concatenate(stem, Bytes.of(0)).copyTo(nextBuffer);
    for (int i = 0; i < nChild; i++) {
      nextBuffer.set(stem.size(), Bytes.of(i));
      Bytes nextLoc = nextBuffer.copy();
      if (isNull.get(i)) {
        children.add(new NullLeafNode<V>());
      } else {
        children.add(createLeafNode(nextLoc, values.get(i)));
      }
    }
    return new StemNode<V>(
        location, stem,
        hash, commitment,
        leftHash, leftCommitment, rightHash, rightCommitment,
        children);
  }

  /**
   * Creates a LeafNode using the provided location, path, and value.
   *
   * @param key          The key of the LeafNode.
   * @param encodedValue Leaf value retrieved from storage.
   * @return A LeafNode instance.
   */
  LeafNode<V> createLeafNode(Bytes key, Bytes encodedValue) {
    V value = valueDeserializer.apply(encodedValue);
    return new LeafNode<V>(Optional.of(key), value);
  }
}
