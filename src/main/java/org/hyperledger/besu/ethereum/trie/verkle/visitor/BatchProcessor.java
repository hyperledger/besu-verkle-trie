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
package org.hyperledger.besu.ethereum.trie.verkle.visitor;

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
public class BatchProcessor {

  private static final Logger LOG = LogManager.getLogger(BatchProcessor.class);
  private static final int MAX_BATCH_SIZE = 1000; // Maximum number of nodes in a batch

  private final Hasher hasher = new PedersenHasher(); // Hasher for node hashing
  private final Map<Bytes, Node<?>> nodeToBatch = new HashMap<>(); // Map to hold nodes for batching
  private int currentBatchDepth = -1; // Tracks the depth of the current batch

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
      nodeToBatch.remove(location.orElseThrow());
    } else {
      nodeToBatch.put(location.orElseThrow(), node);
    }
  }

  /**
   * Returns the map of nodes currently added for future batching.
   *
   * @return Map of nodes to be batched.
   */
  public Map<Bytes, Node<?>> getNodeToBatch() {
    return nodeToBatch;
  }

  /**
   * Processes the nodes in batches. Sorts the nodes by their location and hashes them in batches.
   * Clears the batch after processing.
   */
  public void processInBatches() {
    if (nodeToBatch.isEmpty()) {
      return;
    }
    final List<Map.Entry<Bytes, Node<?>>> toSort = new ArrayList<>(nodeToBatch.entrySet());
    toSort.sort(
        (entry1, entry2) -> Integer.compare(entry2.getKey().size(), entry1.getKey().size()));
    final List<Node<?>> currentBatch = new ArrayList<>();
    for (Map.Entry<Bytes, Node<?>> entry : toSort) {
      Bytes key = entry.getKey();
      Node<?> node = entry.getValue();
      if (!(node instanceof NullNode<?>) && !(node instanceof NullLeafNode<?>)) {
        if (node.getLocation().orElseThrow().isEmpty()) {
          if (!currentBatch.isEmpty()) {
            hashMany(currentBatch);
          }
          calculateRootInternalNodeHashes((InternalNode<?>) node);
          nodeToBatch.clear();
          return;
        } else {
          if (node instanceof BranchNode<?>) {
            if (key.size() != currentBatchDepth || currentBatch.size() >= MAX_BATCH_SIZE) {
              if (!currentBatch.isEmpty()) {
                hashMany(currentBatch);
                currentBatch.clear();
              }
              currentBatchDepth = key.size();
            }
            if (node.isDirty() || node.getHash().isEmpty() || node.getCommitment().isEmpty()) {
              currentBatch.add(node);
            }
          }
        }
      } else {
        if (key.isEmpty()) {
          nodeToBatch.clear();
          return;
        }
      }
    }

    throw new IllegalStateException("root node not found");
  }

  private void hashMany(List<Node<?>> nodes) {
    LOG.atTrace().log("Start hashing {} nodes", nodes.size());
    final List<Bytes> commitmentHashes = new ArrayList<>();
    for (final Node<?> node : nodes) {
      if (node instanceof StemNode<?>) {
        commitmentHashes.addAll(getStemNodeLeftRightCommitmentHashes((StemNode<?>) node));
      } else if (node instanceof InternalNode<?>) {
        commitmentHashes.addAll(getInternalNodeCommitmentHashes((InternalNode<?>) node));
      }
    }
    LOG.atTrace().log("Batching {} nodes", commitmentHashes.size());
    Iterator<Bytes32> frs =
        hasher.manyGroupToField(commitmentHashes.toArray(new Bytes[0])).iterator();

    commitmentHashes.clear();

    for (final Node<?> node : nodes) {
      if (node instanceof StemNode<?>) {
        commitmentHashes.add(getStemNodeCommitmentHash((StemNode<?>) node, frs));
      } else if (node instanceof InternalNode<?>) {
        calculateInternalNodeHashes((InternalNode<?>) node, frs);
      }
    }

    frs = hasher.manyGroupToField(commitmentHashes.toArray(new Bytes[0])).iterator();
    LOG.atTrace().log("Batching {} nodes", commitmentHashes.size());
    for (final Node<?> node : nodes) {
      if (node instanceof StemNode<?>) {
        calculateStemNodeHashes((StemNode<?>) node, frs);
      }
    }
  }

  private void calculateRootInternalNodeHashes(final InternalNode<?> internalNode) {
    final Bytes32 hash = Bytes32.wrap(getInternalNodeCommitmentHashes(internalNode).get(0));
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

  private List<Bytes> getStemNodeLeftRightCommitmentHashes(StemNode<?> stemNode) {
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

  private Bytes getStemNodeCommitmentHash(
      final StemNode<?> stemNode, final Iterator<Bytes32> iterator) {
    Bytes32[] hashes = new Bytes32[4];
    hashes[0] = Bytes32.rightPad(Bytes.of(1)); // extension marker
    hashes[1] = Bytes32.rightPad(stemNode.getStem());
    hashes[2] = iterator.next();
    hashes[3] = iterator.next();
    stemNode.replaceHash(null, null, hashes[2], hashes[2], hashes[3], hashes[3]);
    return hasher.commit(hashes);
  }

  private List<Bytes> getInternalNodeCommitmentHashes(InternalNode<?> internalNode) {
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
