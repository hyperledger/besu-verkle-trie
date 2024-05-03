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
import org.hyperledger.besu.ethereum.trie.verkle.node.LeafNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.Node;
import org.hyperledger.besu.ethereum.trie.verkle.node.NullLeafNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.NullNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.StemNode;
import org.hyperledger.besu.nativelib.ipamultipoint.LibIpaMultipoint;

import java.math.BigInteger;
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
import org.apache.tuweni.units.bigints.UInt256;

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
      new HashMap<>(); // Map to hold nodes for batching
  private final Map<Bytes, LeafNode<?>> updatedLeafNodes =
      new HashMap<>(); // Map to hold nodes for batching

  /**
   * Adds a node for future batching. If the node is a NullNode or NullLeafNode and the location is
   * not empty, it removes the node from the batch.
   *
   * @param location The location of the node.
   * @param node The node to add.
   */
  public void accept(final InternalNode<?> node) {
    Bytes location = node.getLocation().orElseThrow();
    updatedInternalNodes.put(location, (BranchNode<?>) node);
  }

  /**
   * Adds a node for future batching. If the node is a NullNode or NullLeafNode and the location is
   * not empty, it removes the node from the batch.
   *
   * @param location The location of the node.
   * @param node The node to add.
   */
  public void accept(final StemNode<?> node) {
    Bytes location = node.getLocation().orElseThrow();
    updatedInternalNodes.put(location, (BranchNode<?>) node);
  }

  public void accept(final LeafNode<?> node) {
    Bytes location = node.getLocation().orElseThrow();
    updatedLeafNodes.put(location, node);
  }
  /**
   * Adds a node for future batching. If the node is a NullNode or NullLeafNode and the location is
   * not empty, it removes the node from the batch.
   *
   * @param location The location of the node.
   * @param node The node to add.
   */
  public void accept(final NullNode<?> node) {
    Bytes location = node.getLocation().orElseThrow();
    updatedInternalNodes.remove(location);
  }

  /**
   * Adds a node for future batching. If the node is a NullNode or NullLeafNode and the location is
   * not empty, it removes the node from the batch.
   *
   * @param location The location of the node.
   * @param node The node to add.
   */
  public void accept(final NullLeafNode<?> node) {
    Bytes location = node.getLocation().orElseThrow();
    updatedLeafNodes.remove(location);
  }

  /**
   * Returns the map of nodes currently added for future batching.
   *
   * @return Map of nodes to be batched.
   */
  public Map<Bytes, BranchNode<?>> getInternalNodesToBatch() {
    return updatedInternalNodes;
  }

  /**
   * Returns the map of nodes currently added for future batching.
   *
   * @return Map of nodes to be batched.
   */
  public Map<Bytes, LeafNode<?>> getLeafNodesToBatch() {
    return updatedLeafNodes;
  }

  /**
   * Compares two hexstrings in descending ascending order
   * 
   * @return
   */
  private Integer compareKeys(Bytes x, Bytes y) {
    // Longer keys are first
    int cmp;
    cmp = Integer.compare(y.size(), x.size());
    if ( cmp != 0 ) {
      return cmp;
    }
    // 00 < 01 < .. < ff
    for ( int i = 0; i < x.size(); i++ ) {
      cmp = Integer.compare(x.get(i) & 0xff, y.get(i) & 0xff);
      if ( cmp != 0 ) {
        return cmp;
      }
    }
    return 0;
  }


  /**
   * Collects old values from updated Nodes
   */
  public Map<Bytes, Map<Byte, Bytes>> collectOldValues(Bytes, Node<?>batchNodes) {
    for (Map.Entry<Bytes, Node<?>> entry : batchNodes) {
      final Bytes location = entry.getKey();
      final Node<?> node = entry.getValue();
      if ( !location.isEmpty() ) {
        Bytes parent = location.slice(0, location.size() - 1);
        Byte index = location.get(location.size());
        List<Byte> indices = oldIndices.get(parent);
        List<Optional<Bytes32>> values = oldValues.get(parent);
        if ( indices == null || values == null) {
          indices = new ArrayList<>();
          values = new ArrayList<>();
          oldIndices.put(parent, indices);
          oldValues.put(parent, values);
        }
        if ( node instanceof BranchNode<?> ) {
          indices.add(index);
          values.add(node.getHash());
        } else if ( node instanceof LeafNode<?> ) {
          values.put(index, node.getCommittedValue());
        }
      }
    }
  }

  /**
   * Processes the nodes in batches. Sorts the nodes by their location and hashes them in batches.
   * Clears the batch after processing.
   */
  public void calculateStateRoot() {
    if (updatedLeafNodes.isEmpty()) {
      return;
    }

    final List<Map.Entry<Bytes, BranchNode<?>>> sortedNodesByLocation =
        new ArrayList<>(updatedInternalNodes.entrySet());
    sortedNodesByLocation.sort( (entry1, entry2) -> compareKeys(entry1.getKey(), entry2.getKey()) );

    int currentDepth = -1;  // Tracks the depth of the current batch
    Optional<Bytes> currentParent = Optional.empty();  // Tracks the current parent of children nodes (sorted groupby)
    int batch_size = 0;  // Tracks number of parent nodes in current batch

    final Map<Bytes, List<Node<?>>> nodesInSameLevel = new HashMap<>();
    
    for (Map.Entry<Bytes, BranchNode<?>> entry : sortedNodesByLocation) {
      final Bytes location = entry.getKey();
      final Node<?> node = entry.getValue();
      if ( location.size() != currentDepth || batch_size > MAX_BATCH_SIZE ) {
        processBatchInternalNodes(nodesInSameLevel);
        nodesInSameLevel.clear();
        currentDepth = location.size();
        batch_size = 0;
      }
      if ( location.isEmpty() ) {  // Root Node
        calculateRootInternalNodeHash((InternalNode<?>) node);
        updatedInternalNodes.clear();
        return;
      } else {
        if (node.isDirty() || node.getHash().isEmpty() || node.getCommitment().isEmpty()) {
          Bytes parent_loc = location.slice(0, location.size() - 1);
          if ( currentParent.isEmpty() || currentParent.get() != parent_loc ) {
            currentParent = Optional.of(parent_loc);
            List<Node<?>> children = new ArrayList<>();
            children.add(node);
            nodesInSameLevel.put(parent_loc, children);
          } else {
            nodesInSameLevel.get(parent_loc).add(node);
          }
        }
      }
    }
    throw new IllegalStateException("root node not found");
  }

  private void processBatchInternalNodes(Map<Bytes, List<BranchNode<?>>> batch) {
    LOG.atInfo().log("Start processing batch of {} internal nodes", batch.size());
    // Gather all arguments
    Bytes commitment;
    Bytes32 hash;
    BranchNode<?> parent;
    final List<Bytes> commitments = new ArrayList<>();
    final List<BranchNode<?>> parentNodes = new ArrayList<>();
    final List<Byte> indices = new ArrayList<>();
    final List<Bytes> oldValues = new ArrayList<>();
    final List<Bytes> newValues = new ArrayList<>();

    LOG.atInfo().log("Rolling-Up commitments");
    for ( Map.Entry<Bytes, List<BranchNode<?>>> entry : batch.entrySet() ) {
      Bytes parent_loc = entry.getKey();
      BranchNode<?> parent = updatedInternalNodes.get(parent_loc);
      List<BranchNode<?>> children = entry.getValue();
      Bytes oldCommitment = parent.getCommitment().orElseThrow();
      for ( BranchNode<?> node : children ) {
        Bytes loc = node.getLocation().orElseThrow();
        indices.add(loc.get(loc.size()));
        oldValues.add(node.getCommittedHash().orElseThrow());
        newValues.add(node.getHash().orElseThrow());
        node.replaceCommittedHash(node.getHash().orElseThrow());
      }
      commitment = hasher.commitUpdate(parent.getCommitment(), indices, oldValues, newValues);
      indices.clear();
      oldValues.clear();
      newValues.clear();
      parentNodes.add(parent);
      commitments.add(commitment);
    }

    LOG.atTrace().log("Hashing commitments");
    List<Bytes32> frs = hasher.hashMany(commitments.toArray(new Bytes[0]));
    for ( int i=0; i < commitments.size(); i++ ) {
      parent = parentNodes.get(i);
      commitment = commitments.get(i);
      hash = frs.get(i);
      parent.replaceHash(commitment, hash);
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
