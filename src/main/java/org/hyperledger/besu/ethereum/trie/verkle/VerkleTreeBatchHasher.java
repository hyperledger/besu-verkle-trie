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
package org.hyperledger.besu.ethereum.trie.verkle;

import static org.hyperledger.besu.ethereum.trie.verkle.node.Node.getHighValue;
import static org.hyperledger.besu.ethereum.trie.verkle.node.Node.getLowValue;

import org.hyperledger.besu.ethereum.trie.verkle.hasher.Hasher;
import org.hyperledger.besu.ethereum.trie.verkle.hasher.PedersenHasher;
import org.hyperledger.besu.ethereum.trie.verkle.node.BranchNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.InternalNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.Node;
import org.hyperledger.besu.ethereum.trie.verkle.node.NullLeafNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.NullNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.StemNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

/**
 * Processes batches of trie nodes for efficient hashing.
 *
 * <p>This class manages the batching and hashing of trie nodes to optimize performance.
 */
@SuppressWarnings("unchecked")
public class VerkleTreeBatchHasher {

  private static final Logger LOG = LogManager.getLogger(VerkleTreeBatchHasher.class);
  private static final int MAX_BATCH_SIZE = 1000; // Maximum number of nodes in a batch

  private final Hasher hasher = new PedersenHasher(); // Hasher for node hashing
  private final Map<Bytes, UpdatedNodeData<?>> updatedNodes =
      new HashMap<>(); // Map to hold nodes for batching

  /**
   * Adds a node for future batching. If the node is a NullNode or NullLeafNode and the location is
   * not empty, it removes the node from the batch.
   *
   * @param location The location of the node.
   * @param node The node to add.
   */
  public void addNodeToBatch(
      final Optional<Bytes> location, final Node<?> node, final Optional<Bytes> previous) {
    if ((node instanceof NullNode<?> || node instanceof NullLeafNode<?>)
        && !location.orElseThrow().isEmpty()) {
      updatedNodes.remove(location.orElseThrow());
    } else {
      updatedNodes.put(location.orElseThrow(), new UpdatedNodeData<>(node, previous));
    }
  }

  public void addNodeToBatch(final Optional<Bytes> location, final Node<?> node) {
    addNodeToBatch(location, node, Optional.empty());
  }

  /**
   * Returns the map of nodes currently added for future batching.
   *
   * @return Map of nodes to be batched.
   */
  public Map<Bytes, UpdatedNodeData<?>> getNodesToBatch() {
    return updatedNodes;
  }

  /**
   * Processes the nodes in batches. Sorts the nodes by their location and hashes them in batches.
   * Clears the batch after processing.
   */
  @SuppressWarnings("unchecked")
  public void calculateStateRoot() {
    if (updatedNodes.isEmpty()) {
      return;
    }

    final List<Map.Entry<Bytes, UpdatedNodeData<?>>> sortedNodesByLocation =
        new ArrayList<>(updatedNodes.entrySet());
    sortedNodesByLocation.sort(
        (entry1, entry2) -> Integer.compare(entry2.getKey().size(), entry1.getKey().size()));

    int currentDepth = -1; // Tracks the depth of the current batch

    final List<UpdatedNodeData<?>> nodesInSameLevel = new ArrayList<>();
    for (Map.Entry<Bytes, UpdatedNodeData<?>> entry : sortedNodesByLocation) {
      final Bytes location = entry.getKey();
      final UpdatedNodeData<?> foundUpdatedNode = entry.getValue();
      if (foundUpdatedNode.node instanceof BranchNode<?>) {
        if (location.size() != currentDepth || nodesInSameLevel.size() > MAX_BATCH_SIZE) {
          if (!nodesInSameLevel.isEmpty()) {
            processBatch(nodesInSameLevel);
            nodesInSameLevel.clear();
          }
          if (location.isEmpty()) {
            calculateRootInternalNodeHash((UpdatedNodeData<InternalNode<?>>) foundUpdatedNode);
            updatedNodes.clear();
            return;
          }
          currentDepth = location.size();
        }
        if (foundUpdatedNode.node.isDirty()
            || foundUpdatedNode.node.getHash().isEmpty()
            || foundUpdatedNode.node.getCommitment().isEmpty()) {
          nodesInSameLevel.add(foundUpdatedNode);
        }
      }
    }

    throw new IllegalStateException("root node not found");
  }

  private void processBatch(List<UpdatedNodeData<?>> updatedNodeDataList) {
    LOG.atInfo().log("Start hashing {} batch of nodes", updatedNodeDataList.size());
    final List<Bytes> commitments = new ArrayList<>();

    LOG.atTrace().log("Creating commitments for stem nodes and internal nodes");
    for (final UpdatedNodeData<?> foundUpdatedNode : updatedNodeDataList) {
      if (foundUpdatedNode.node instanceof StemNode<?>) {
        commitments.addAll(
            getStemNodeLeftRightCommitments((UpdatedNodeData<StemNode<?>>) foundUpdatedNode));
      } else if (foundUpdatedNode.node instanceof InternalNode<?>) {
        commitments.addAll(
            getInternalNodeCommitments((UpdatedNodeData<InternalNode<?>>) foundUpdatedNode));
      }
    }

    LOG.atTrace()
        .log(
            "Executing batch hashing for {} commitments of stem (left/right) and internal nodes.",
            commitments.size());
    Iterator<Bytes> commitmentsIterator = new ArrayList<>(commitments).iterator();
    Iterator<Bytes32> frs = hasher.hashMany(commitments.toArray(new Bytes[0])).iterator();

    commitments.clear();

    LOG.atTrace()
        .log("Creating commitments for stem nodes and refreshing hashes of internal nodes");
    for (final UpdatedNodeData<?> foundUpdatedNode : updatedNodeDataList) {
      if (foundUpdatedNode.node instanceof StemNode<?>) {
        commitments.add(
            getStemNodeCommitment(
                (UpdatedNodeData<StemNode<?>>) foundUpdatedNode, commitmentsIterator, frs));
      } else if (foundUpdatedNode.node instanceof InternalNode<?>) {
        calculateInternalNodeHashes(
            (UpdatedNodeData<InternalNode<?>>) foundUpdatedNode, commitmentsIterator, frs);
      }
    }
    LOG.atTrace()
        .log("Executing batch hashing for {} commitments of stem nodes.", commitments.size());
    frs = hasher.hashMany(commitments.toArray(new Bytes[0])).iterator();

    LOG.atTrace().log("Refreshing hashes of stem nodes");
    for (final UpdatedNodeData<?> foundUpdatedNode : updatedNodeDataList) {
      if (foundUpdatedNode.node instanceof StemNode<?>) {
        calculateStemNodeHashes((UpdatedNodeData<StemNode<?>>) foundUpdatedNode, frs);
      }
    }
  }

  private void calculateRootInternalNodeHash(final UpdatedNodeData<InternalNode<?>> internalNode) {
    final Bytes32 hash = Bytes32.wrap(getInternalNodeCommitments(internalNode).get(0));
    internalNode.node.replaceHash(hash, hash);
  }

  private void calculateStemNodeHashes(
      final UpdatedNodeData<StemNode<?>> foundUpdatedNode, final Iterator<Bytes32> iterator) {
    final Bytes32 hash = iterator.next();
    foundUpdatedNode.node.replaceHash(
        hash,
        hash,
        foundUpdatedNode.node.getLeftHash().orElseThrow(),
        foundUpdatedNode.node.getLeftCommitment().orElseThrow(),
        foundUpdatedNode.node.getRightHash().orElseThrow(),
        foundUpdatedNode.node.getRightCommitment().orElseThrow());
  }

  private void calculateInternalNodeHashes(
      final UpdatedNodeData<InternalNode<?>> foundUpdatedNode,
      final Iterator<Bytes> commitmentsIterator,
      final Iterator<Bytes32> iterator) {
    final Bytes32 hash = iterator.next();
    foundUpdatedNode.node.replaceHash(hash, commitmentsIterator.next());
  }

  private List<Bytes> getStemNodeLeftRightCommitments(
      UpdatedNodeData<StemNode<?>> foundUpdatedNode) {
    int size = StemNode.maxChild();
    List<Bytes> commitmentsHashes = new ArrayList<>();

    final List<Byte> leftIndices = new ArrayList<>();
    final List<Bytes> leftOldValues = new ArrayList<>();
    final List<Bytes> leftNewValues = new ArrayList<>();

    final List<Byte> rightIndices = new ArrayList<>();
    final List<Bytes> rightOldValues = new ArrayList<>();
    final List<Bytes> rightNewValues = new ArrayList<>();

    int halfSize = size / 2;
    for (int i = 0; i < size; i++) {
      Node<?> node = foundUpdatedNode.node.child((byte) i);
      int idx = Byte.toUnsignedInt((byte) i);
      Optional<Bytes> previousValue =
          node.getLocation()
              .map(updatedNodes::get)
              .flatMap(updatedNodeData -> updatedNodeData.previous);
      if (idx < halfSize) {
        leftIndices.add((byte) (2 * idx));
        leftIndices.add((byte) (2 * idx + 1));
        leftOldValues.add(getLowValue(previousValue));
        leftOldValues.add(getHighValue(previousValue));
        leftNewValues.add(getLowValue(node.getValue()));
        leftNewValues.add(getHighValue(node.getValue()));
      } else {
        rightIndices.add((byte) (2 * idx));
        rightIndices.add((byte) (2 * idx + 1));
        rightOldValues.add(getLowValue(previousValue));
        rightOldValues.add(getHighValue(previousValue));
        rightNewValues.add(getLowValue(node.getValue()));
        rightNewValues.add(getHighValue(node.getValue()));
      }
    }

    if (!leftIndices.isEmpty()) {
      commitmentsHashes.add(
          hasher.commitUpdate(
              foundUpdatedNode.node.getLeftCommitment(),
              leftIndices,
              leftOldValues,
              leftNewValues));
      leftIndices.clear();
      leftOldValues.clear();
      leftNewValues.clear();
    }
    if (!rightIndices.isEmpty()) {
      commitmentsHashes.add(
          hasher.commitUpdate(
              foundUpdatedNode.node.getRightCommitment(),
              rightIndices,
              rightOldValues,
              rightNewValues));
      rightIndices.clear();
      rightOldValues.clear();
      rightNewValues.clear();
    }

    return commitmentsHashes;
  }

  private Bytes getStemNodeCommitment(
      final UpdatedNodeData<StemNode<?>> foundUpdatedNode,
      final Iterator<Bytes> commitmentsIterator,
      final Iterator<Bytes32> iterator) {
    Bytes32[] hashes = new Bytes32[4];
    hashes[0] = Bytes32.rightPad(Bytes.of(1)); // extension marker
    hashes[1] = Bytes32.rightPad(foundUpdatedNode.node.getStem());
    hashes[2] = iterator.next();
    hashes[3] = iterator.next();
    foundUpdatedNode.node.replaceHash(
        null, null, hashes[2], commitmentsIterator.next(), hashes[3], commitmentsIterator.next());
    return hasher.commit(hashes);
  }

  private List<Bytes> getInternalNodeCommitments(
      UpdatedNodeData<InternalNode<?>> foundUpdatedNode) {
    int size = InternalNode.maxChild();
    List<Bytes> commitmentsHashes = new ArrayList<>();

    final List<Byte> indices = new ArrayList<>();
    final List<Bytes> oldValues = new ArrayList<>();
    final List<Bytes> newValues = new ArrayList<>();

    for (int i = 0; i < size; i++) {
      Node<?> node = foundUpdatedNode.node.child((byte) i);
      indices.add((byte) i);
      oldValues.add(
          node.getLocation()
              .map(updatedNodes::get)
              .flatMap(updatedNodeData -> updatedNodeData.previous)
              .orElse(Bytes.EMPTY));
      newValues.add(node.getHash().get());
    }
    if (foundUpdatedNode.node.getLocation().orElseThrow().isEmpty()) {
      commitmentsHashes.add(hasher.commitRoot(newValues.toArray(new Bytes[] {})));
    } else {
      System.out.println(foundUpdatedNode.node.getLocation());
      commitmentsHashes.add(
          hasher.commitUpdate(
              foundUpdatedNode.node.getCommitment(), indices, oldValues, newValues));
    }
    return commitmentsHashes;
  }

  public record UpdatedNodeData<V extends Node<?>>(V node, Optional<Bytes> previous) {}
}
