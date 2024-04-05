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
public class VerkleTreeBatchHasher {

  private static final Logger LOG = LogManager.getLogger(VerkleTreeBatchHasher.class);
  private static final int MAX_BATCH_SIZE = 1000; // Maximum number of nodes in a batch

  private final Hasher hasher = new PedersenHasher(); // Hasher for node hashing
  private final Map<Bytes, Node<?>> updatedNodes =
      new HashMap<>(); // Map to hold nodes for batching

  /**
   * Adds a node for future batching. If the node is a NullNode or NullLeafNode and the location is
   * not empty, it removes the node from the batch.
   *
   * @param location The location of the node.
   * @param node The node to add.
   */
  public void addNodeToBatch(final Optional<Bytes> location, final Node<?> node) {
    if ((node instanceof NullNode<?> || node instanceof NullLeafNode<?>)
        && !location.orElseThrow().isEmpty()) {
      updatedNodes.remove(location.orElseThrow());
    } else {
      updatedNodes.put(location.orElseThrow(), node);
    }
  }

  /**
   * Returns the map of nodes currently added for future batching.
   *
   * @return Map of nodes to be batched.
   */
  public Map<Bytes, Node<?>> getNodesToBatch() {
    return updatedNodes;
  }

  /**
   * Processes the nodes in batches. Sorts the nodes by their location and hashes them in batches.
   * Clears the batch after processing.
   */
  public void calculateStateRoot() {
    if (updatedNodes.isEmpty()) {
      return;
    }

    final List<Map.Entry<Bytes, Node<?>>> sortedNodesByLocation =
        new ArrayList<>(updatedNodes.entrySet());
    sortedNodesByLocation.sort(
        (entry1, entry2) -> Integer.compare(entry2.getKey().size(), entry1.getKey().size()));

    int currentDepth = -1; // Tracks the depth of the current batch

    final List<Node<?>> nodesInSameLevel = new ArrayList<>();
    for (Map.Entry<Bytes, Node<?>> entry : sortedNodesByLocation) {
      final Bytes location = entry.getKey();
      final Node<?> node = entry.getValue();
      if (node instanceof BranchNode<?>) {
        if (location.size() != currentDepth || nodesInSameLevel.size() > MAX_BATCH_SIZE) {
          if (!nodesInSameLevel.isEmpty()) {
            processBatch(nodesInSameLevel);
            nodesInSameLevel.clear();
          }
          if (location.isEmpty()) {
            calculateRootInternalNodeHash((InternalNode<?>) node);
            updatedNodes.clear();
            return;
          }
          currentDepth = location.size();
        }
        if (node.isDirty() || node.getHash().isEmpty() || node.getCommitment().isEmpty()) {
          nodesInSameLevel.add(node);
        }
      }
    }

    throw new IllegalStateException("root node not found");
  }

  private void processBatch(List<Node<?>> nodes) {
    LOG.atInfo().log("Start hashing {} batch of nodes", nodes.size());
    final List<Bytes> commitments = new ArrayList<>();

    LOG.atTrace().log("Creating commitments for stem nodes and internal nodes");
    for (final Node<?> node : nodes) {
      if (node instanceof StemNode<?>) {
        commitments.addAll(getStemNodeLeftRightCommitments((StemNode<?>) node));
      } else if (node instanceof InternalNode<?>) {
        commitments.addAll(getInternalNodeCommitments((InternalNode<?>) node));
      }
    }

    LOG.atTrace()
        .log(
            "Executing batch hashing for {} commitments of stem (left/right) and internal nodes.",
            commitments.size());
    Iterator<Bytes32> frs = hasher.hashMany(commitments.toArray(new Bytes[0])).iterator();

    commitments.clear();

    LOG.atTrace()
        .log("Creating commitments for stem nodes and refreshing hashes of internal nodes");
    for (final Node<?> node : nodes) {
      if (node instanceof StemNode<?>) {
        commitments.add(getStemNodeCommitment((StemNode<?>) node, frs));
      } else if (node instanceof InternalNode<?>) {
        calculateInternalNodeHashes((InternalNode<?>) node, frs);
      }
    }
    LOG.atTrace()
        .log("Executing batch hashing for {} commitments of stem nodes.", commitments.size());
    frs = hasher.hashMany(commitments.toArray(new Bytes[0])).iterator();

    LOG.atTrace().log("Refreshing hashes of stem nodes");
    for (final Node<?> node : nodes) {
      if (node instanceof StemNode<?>) {
        calculateStemNodeHashes((StemNode<?>) node, frs);
      }
    }
  }

  private void calculateRootInternalNodeHash(final InternalNode<?> internalNode) {
    final Bytes32 hash = Bytes32.wrap(getInternalNodeCommitments(internalNode).get(0));
    internalNode.replaceHash(hash, hash);
  }

  private void calculateStemNodeHashes(
      final StemNode<?> stemNode, final Iterator<Bytes32> iterator) {
    final Bytes32 hash = iterator.next();
    stemNode.replaceHash(
        hash,
        hash,
        stemNode.getLeftHash().orElseThrow(),
        stemNode.getLeftCommitment().orElseThrow(),
        stemNode.getRightHash().orElseThrow(),
        stemNode.getRightCommitment().orElseThrow());
  }

  private void calculateInternalNodeHashes(
      final InternalNode<?> internalNode, final Iterator<Bytes32> iterator) {
    final Bytes32 hash = iterator.next();
    internalNode.replaceHash(hash, hash);
  }

  private List<Bytes> getStemNodeLeftRightCommitments(StemNode<?> stemNode) {
    int size = StemNode.maxChild();
    List<Bytes> commitmentsHashes = new ArrayList<>();
    Bytes32[] leftValues = new Bytes32[size];
    Bytes32[] rightValues = new Bytes32[size];
    for (int i = 0; i < size / 2; i++) {
      byte index = (byte) i;
      Node<?> child = stemNode.child(index);
      leftValues[2 * i] = getLowValue(child.getValue());
      leftValues[2 * i + 1] = Node.getHighValue(child.getValue());
    }
    for (int i = size / 2; i < size; i++) {
      byte index = (byte) i;
      Node<?> child = stemNode.child(index);
      rightValues[2 * i - size] = getLowValue(child.getValue());
      rightValues[2 * i + 1 - size] = Node.getHighValue(child.getValue());
    }
    commitmentsHashes.add(hasher.commit(leftValues));
    commitmentsHashes.add(hasher.commit(rightValues));
    return commitmentsHashes;
  }

  private Bytes getStemNodeCommitment(
      final StemNode<?> stemNode, final Iterator<Bytes32> iterator) {
    Bytes32[] hashes = new Bytes32[4];
    hashes[0] = Bytes32.rightPad(Bytes.of(1)); // extension marker
    hashes[1] = Bytes32.rightPad(stemNode.getStem());
    hashes[2] = iterator.next();
    hashes[3] = iterator.next();
    stemNode.replaceHash(null, null, hashes[2], hashes[2], hashes[3], hashes[3]);
    return hasher.commit(hashes);
  }

  private List<Bytes> getInternalNodeCommitments(InternalNode<?> internalNode) {
    int size = InternalNode.maxChild();
    List<Bytes> commitmentsHashes = new ArrayList<>();
    Bytes32[] hashes = new Bytes32[size];
    for (int i = 0; i < size; i++) {
      byte index = (byte) i;
      Node<?> child = internalNode.child(index);
      hashes[i] = child.getHash().get();
    }
    if (internalNode.getLocation().orElseThrow().isEmpty()) {
      commitmentsHashes.add(hasher.commitRoot(hashes));
    } else {
      commitmentsHashes.add(hasher.commit(hashes));
    }
    return commitmentsHashes;
  }
}
