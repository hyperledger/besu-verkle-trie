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

import org.hyperledger.besu.ethereum.trie.verkle.VerkleTreeBatchHasher;
import org.hyperledger.besu.ethereum.trie.verkle.node.InternalNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.Node;
import org.hyperledger.besu.ethereum.trie.verkle.node.NullNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.StemNode;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;

/**
 * Class representing a visitor for flattening a node in a Trie tree.
 *
 * <p>Flattening a node means that it is merged with its parent, adding one level to the extension
 * path. Per current specs, only StemNodes can have extensions, so only StemNodes can potentially be
 * flattened.
 *
 * @param <V> The type of node values.
 */
public class FlattenVisitor<V> implements NodeVisitor<V> {

  private final Optional<VerkleTreeBatchHasher> batchProcessor;

  public FlattenVisitor(final Optional<VerkleTreeBatchHasher> batchProcessor) {
    this.batchProcessor = batchProcessor;
  }

  @Override
  public Node<V> visit(InternalNode<V> internalNode) {
    return new NullNode<>();
  }

  @Override
  public Node<V> visit(StemNode<V> stemNode) {
    final Bytes location = stemNode.getLocation().get();
    final Bytes newLocation = location.slice(0, location.size() - 1);
    // Should not flatten root node
    if (!newLocation.isEmpty()) {
      final StemNode<V> updateStemNode = stemNode.replaceLocation(newLocation);
      batchProcessor.ifPresent(
          processor -> {
            final NullNode<V> nullNode = new NullNode<>();
            nullNode.markDirty();
            processor.addNodeToBatch(stemNode.getLocation(), nullNode);
            updateStemNode.markDirty();
            processor.addNodeToBatch(updateStemNode.getLocation(), updateStemNode);
          });
      return updateStemNode;
    } else {
      return new NullNode<>();
    }
  }
}
