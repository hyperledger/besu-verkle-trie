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
import org.hyperledger.besu.ethereum.trie.verkle.node.LeafNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.NullLeafNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.NullNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.StemNode;

import java.util.ArrayList;
import java.util.HashMap;
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
public class VerkleTreeBatchUpdateHasher {

  private static final Logger LOG = LogManager.getLogger(VerkleTreeBatchUpdateHasher.class);
  private static final int MAX_BATCH_SIZE = 1000; // Maximum number of nodes in a batch

  private final Hasher hasher = new PedersenHasher(); // Hasher for node hashing
  private final Map<Bytes, BranchNode<?>> updatedInternalNodes =
      new HashMap<>(); // Map to hold branch nodes for batching
  private final Map<Bytes, StemNode<?>> updatedStemNodes =
      new HashMap<>(); // Map to hold stem nodes for batching leaf nodes
  private final Map<Bytes, LeafNode<?>> updatedLeafNodes =
      new HashMap<>(); // Map to hold leaf nodes for batching

  /**
   * Adds a node for future batching.
   *
   * @param node The node to add.
   */
  public void accept(final InternalNode<?> node) {
    Bytes location = node.getLocation().orElseThrow();
    updatedInternalNodes.put(location, (BranchNode<?>) node);
  }

  /**
   * Adds a node for future batching
   *
   * @param node The node to add.
   */
  public void accept(final StemNode<?> node) {
    Bytes location = node.getLocation().orElseThrow();
    updatedInternalNodes.put(location, (BranchNode<?>) node);
    updatedStemNodes.put(node.getStem(), node);
  }

  /**
   * Adds a node for future batching
   *
   * @param node The node to add.
   */
  public void accept(final LeafNode<?> node) {
    Bytes location = node.getLocation().orElseThrow();
    updatedLeafNodes.put(location, node);
  }

  /**
   * Removes the node from the batch at given location.
   *
   * @param node The node to add.
   * @param location The location of the node.
   */
  public void accept(final NullNode<?> node, Optional<Bytes> location) {
    Bytes loc = location.orElseThrow();
    updatedInternalNodes.remove(loc);
  }

  /**
   * Removes the node from the batch at given location.
   *
   * @param node The node to add.
   * @param location The location of the node.
   */
  public void accept(final NullLeafNode<?> node, Optional<Bytes> location) {
    Bytes loc = location.orElseThrow();
    updatedLeafNodes.remove(loc);
  }

  /**
   * Returns the map of Internal/Stem nodes currently added for future batching.
   *
   * @return Map of nodes to be batched.
   */
  public Map<Bytes, BranchNode<?>> getInternalNodesToBatch() {
    return updatedInternalNodes;
  }

  /**
   * Returns the map of Leaf nodes currently added for future batching.
   *
   * @return Map of nodes to be batched.
   */
  public Map<Bytes, LeafNode<?>> getLeafNodesToBatch() {
    return updatedLeafNodes;
  }

  /**
   * Processes the nodes in batches. Sorts the nodes by their location and hashes them in batches.
   * Clears the batch after processing.
   */
  public void calculateStateRoot() {
    if (updatedLeafNodes.isEmpty()) {
      return;
    }
    processLeafNodes();
    processInternalNodes();
  }

  /**
   * Compares two TrieKeys according to the following order: longer strings come first, otherwise
   * use bytewise order from left to right.
   *
   * @param x bytes to compare to
   * @param y bytes to compare with
   * @return comparaison int
   */
  private int compareKeys(Bytes x, Bytes y) {
    // Longer keys are first
    int cmp;
    cmp = Integer.compare(y.size(), x.size());
    if (cmp != 0) {
      return cmp;
    }
    // 00 < 01 < .. < ff
    for (int i = 0; i < x.size(); i++) {
      cmp = Integer.compare(x.get(i) & 0xff, y.get(i) & 0xff);
      if (cmp != 0) {
        return cmp;
      }
    }
    return 0;
  }

  private Bytes32 processInternalNodes() {
    final List<Map.Entry<Bytes, BranchNode<?>>> sortedNodesByLocation =
        new ArrayList<>(updatedInternalNodes.entrySet());
    sortedNodesByLocation.sort((entry1, entry2) -> compareKeys(entry1.getKey(), entry2.getKey()));

    int currentDepth = -1; // Tracks the depth of the current batch
    Optional<Bytes> currentParent =
        Optional.empty(); // Tracks the current parent of children nodes (sorted groupby)
    int batch_size = 0; // Tracks number of parent nodes in current batch

    final List<InternalNode<?>> parentNodesBatch = new ArrayList<>();
    final List<List<BranchNode<?>>> childrenNodesBatch = new ArrayList<>();

    for (Map.Entry<Bytes, BranchNode<?>> entry : sortedNodesByLocation) {

      final Bytes location = entry.getKey();
      final BranchNode<?> node = entry.getValue();

      if (location.size() != currentDepth || batch_size > MAX_BATCH_SIZE) {
        processBatchInternalNodes(parentNodesBatch, childrenNodesBatch);
        parentNodesBatch.clear();
        childrenNodesBatch.clear();
        batch_size = 0;
        currentDepth = location.size();
      }
      if (location.isEmpty()) { // Root Node
        updatedInternalNodes.clear();
        return hasher.compress(node.getCommitment().orElseThrow());
      } else {
        if (node.isDirty() || node.getHash().isEmpty() || node.getCommitment().isEmpty()) {
          Bytes parent_loc = location.slice(0, location.size() - 1);
          if (currentParent.isEmpty() || !currentParent.get().equals(parent_loc)) {
            currentParent = Optional.of(parent_loc);
            parentNodesBatch.add((InternalNode<?>) updatedInternalNodes.get(parent_loc));
            List<BranchNode<?>> children = new ArrayList<>();
            children.add(node);
            childrenNodesBatch.add(children);
          } else {
            childrenNodesBatch.get(childrenNodesBatch.size() - 1).add(node);
          }
        }
      }
    }
    throw new IllegalStateException("root node not found");
  }

  private void processLeafNodes() {
    final List<Map.Entry<Bytes, LeafNode<?>>> sortedNodesByLocation =
        new ArrayList<>(updatedLeafNodes.entrySet());
    sortedNodesByLocation.sort((entry1, entry2) -> compareKeys(entry1.getKey(), entry2.getKey()));

    Optional<Bytes> currentParent = Optional.empty(); // Tracks the current parent of children nodes (sorted groupby)
    int batch_size = 0; // Tracks number of parent nodes in current batch
    final List<StemNode<?>> stemNodes = new ArrayList<>();
    final List<List<LeafNode<?>>> leavesByStem = new ArrayList<>();

    for (Map.Entry<Bytes, LeafNode<?>> entry : sortedNodesByLocation) {
      final Bytes location = entry.getKey();
      final LeafNode<?> node = entry.getValue();

      if (batch_size > MAX_BATCH_SIZE) {
        processBatchLeafNodes(stemNodes, leavesByStem);
        processBatchStemNodes(stemNodes);
        stemNodes.clear();
        leavesByStem.clear();
        batch_size = 0;
      }
      if (node.isDirty()
          && (node.getCommittedValue().isEmpty()
              || !node.getValue().orElse(null).equals(node.getCommittedValue().get()))) {
        Bytes parent_loc = location.slice(0, location.size() - 1);
        if (currentParent.isEmpty() || !currentParent.get().equals(parent_loc)) {
          currentParent = Optional.of(parent_loc);
          stemNodes.add((StemNode<?>) updatedStemNodes.get(parent_loc));
          List<LeafNode<?>> children = new ArrayList<>();
          children.add(node);
          leavesByStem.add(children);
        } else {
          leavesByStem.get(leavesByStem.size() - 1).add(node);
        }
      }
    }
    processBatchLeafNodes(stemNodes, leavesByStem);
    processBatchStemNodes(stemNodes);
  }

  private void processBatchInternalNodes(
      List<InternalNode<?>> parentNodes, List<List<BranchNode<?>>> childrenNodes) {
    LOG.atInfo().log(
        "Processing batch of {} internal nodes with {} values",
        parentNodes.size(),
        childrenNodes.size());
    // Gather all arguments
    Bytes commitment;
    final List<Bytes> commitments = new ArrayList<>();
    final List<Byte> indices = new ArrayList<>();
    final List<Bytes> oldValues = new ArrayList<>();
    final List<Bytes> newValues = new ArrayList<>();

    LOG.atInfo().log("Rolling-Up commitments");
    for (int i = 0; i < parentNodes.size(); i++) {
      InternalNode<?> parent = parentNodes.get(i);
      for (BranchNode<?> node : childrenNodes.get(i)) {
        Bytes loc = node.getLocation().orElseThrow();
        indices.add(loc.get(loc.size() - 1));
        oldValues.add(node.getCommittedHash().orElse(Bytes32.ZERO));
        newValues.add(node.getHash().orElseThrow());
        node.replaceCommittedHash(node.getHash().orElseThrow());
      }
      commitment = hasher.commitUpdate(parent.getCommitment(), indices, oldValues, newValues);
      commitments.add(commitment);
      indices.clear();
      oldValues.clear();
      newValues.clear();
    }

    LOG.atTrace().log("Hashing commitments");
    List<Bytes32> frs = hasher.hashMany(commitments.toArray(new Bytes[0]));

    LOG.atTrace().log("Updating commitments");
    for (int i = 0; i < parentNodes.size(); i++) {
      InternalNode<?> parent = parentNodes.get(i);
      commitment = commitments.get(i);
      Bytes32 hash = frs.get(i);
      parent.replaceHash(hash, commitment);
    }
  }

  private void processBatchStemNodes(List<StemNode<?>> stemNodes) {
    LOG.atInfo().log("Processing batch of {} stem-extension nodes", stemNodes.size());
    final List<Bytes> commitments = new ArrayList<>();
    final Bytes32[] hashes = new Bytes32[4];
    // IDEA: extension marker and stem could be committed to on StemNode creation.
    hashes[0] = Bytes32.rightPad(Bytes.of(1)); // extension marker

    LOG.atInfo().log("Rolling-Up commitments");
    for (StemNode<?> stemNode : stemNodes) {
      hashes[1] = Bytes32.rightPad(stemNode.getStem());
      hashes[2] = stemNode.getLeftHash().orElse(Bytes32.ZERO);
      hashes[3] = stemNode.getRightHash().orElse(Bytes32.ZERO);
      commitments.add(hasher.commit(hashes));
    }

    LOG.atTrace().log("Hashing commitments");
    List<Bytes32> frs = hasher.hashMany(commitments.toArray(new Bytes[0]));

    LOG.atTrace().log("Updating commitments");
    for (int i = 0; i < stemNodes.size(); i++) {
      StemNode<?> stemNode = stemNodes.get(i);
      Bytes commitment = commitments.get(i);
      Bytes32 hash = frs.get(i);
      stemNode.replaceHash(hash, commitment);
    }
  }

  private void processBatchLeafNodes(
      List<StemNode<?>> parentNodes, List<List<LeafNode<?>>> childrenNodes) {
    LOG.atInfo().log(
        "Processing batch of LeafNodes: {} stems with {} values",
        parentNodes.size(),
        childrenNodes.size());
    Bytes commitment;

    final List<Bytes> leftCommitments = new ArrayList<>();
    final List<Byte> leftIndices = new ArrayList<>();
    final List<Bytes> leftOldValues = new ArrayList<>();
    final List<Bytes> leftNewValues = new ArrayList<>();
    final List<StemNode<?>> leftStems = new ArrayList<>();

    final List<Bytes> rightCommitments = new ArrayList<>();
    final List<Byte> rightIndices = new ArrayList<>();
    final List<Bytes> rightOldValues = new ArrayList<>();
    final List<Bytes> rightNewValues = new ArrayList<>();
    final List<StemNode<?>> rightStems = new ArrayList<>();

    LOG.atInfo().log("Rolling-Up commitments");
    for (int i = 0; i < parentNodes.size(); i++) {
      StemNode<?> parent = parentNodes.get(i);
      int size = StemNode.maxChild();
      int halfSize = size / 2;
      for (LeafNode<?> node : childrenNodes.get(i)) {
        Bytes loc = node.getLocation().orElseThrow();
        int idx = Byte.toUnsignedInt(loc.get(loc.size() - 1));

        if (idx < halfSize) {
          leftIndices.add((byte) (2 * idx));
          leftIndices.add((byte) (2 * idx + 1));
          leftOldValues.add(getLowValue(node.getCommittedValue()));
          leftOldValues.add(getHighValue(node.getCommittedValue()));
          leftNewValues.add(getLowValue(node.getValue()));
          leftNewValues.add(getHighValue(node.getValue()));
        } else {
          rightIndices.add((byte) (2 * idx));
          rightIndices.add((byte) (2 * idx + 1));
          rightOldValues.add(getLowValue(node.getCommittedValue()));
          rightOldValues.add(getHighValue(node.getCommittedValue()));
          rightNewValues.add(getLowValue(node.getValue()));
          rightNewValues.add(getHighValue(node.getValue()));
        }
      }
      if (!leftIndices.isEmpty()) {
        commitment =
            hasher.commitUpdate(
                parent.getLeftCommitment(), leftIndices, leftOldValues, leftNewValues);
        leftCommitments.add(commitment);
        leftStems.add(parent);
        leftIndices.clear();
        leftOldValues.clear();
        leftNewValues.clear();
      }
      if (!rightIndices.isEmpty()) {
        commitment =
            hasher.commitUpdate(
                parent.getRightCommitment(), rightIndices, rightOldValues, rightNewValues);
        rightCommitments.add(commitment);
        rightStems.add(parent);
        rightIndices.clear();
        rightOldValues.clear();
        rightNewValues.clear();
      }
    }

    LOG.atTrace().log("Hashing commitments");
    List<Bytes32> leftFrs = hasher.hashMany(leftCommitments.toArray(new Bytes[0]));
    List<Bytes32> rightFrs = hasher.hashMany(rightCommitments.toArray(new Bytes[0]));

    LOG.atTrace().log("Updating commitments");
    for (int i = 0; i < leftStems.size(); i++) {
      StemNode<?> parent = leftStems.get(i);
      commitment = leftCommitments.get(i);
      Bytes32 hash = leftFrs.get(i);
      parent.replaceLeftHash(hash, commitment);
    }
    for (int i = 0; i < rightStems.size(); i++) {
      StemNode<?> parent = rightStems.get(i);
      commitment = rightCommitments.get(i);
      Bytes32 hash = rightFrs.get(i);
      parent.replaceRightHash(hash, commitment);
    }
  }
}
