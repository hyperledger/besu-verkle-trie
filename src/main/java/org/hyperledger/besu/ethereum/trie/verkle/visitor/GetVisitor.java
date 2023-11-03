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
package org.hyperledger.besu.ethereum.trie.verkle.visitor;

import org.hyperledger.besu.ethereum.trie.verkle.node.BranchNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.LeafNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.Node;
import org.hyperledger.besu.ethereum.trie.verkle.node.NullNode;

import org.apache.tuweni.bytes.Bytes;

/**
 * Class representing a visitor for traversing nodes in a Trie tree to find a node based on a path.
 *
 * @param <V> The type of node values.
 */
public class GetVisitor<V> implements PathNodeVisitor<V> {
  private final Node<V> NULL_NODE_RESULT = NullNode.instance();

  /**
   * Visits a BranchNode to determine the node matching a given path.
   *
   * @param branchNode The BranchNode being visited.
   * @param path The path to search in the tree.
   * @return The matching node or NULL_NODE_RESULT if not found.
   */
  @Override
  public Node<V> visit(final BranchNode<V> branchNode, final Bytes path) {
    final Bytes nodePath = branchNode.getPath();
    final Bytes commonPath = nodePath.commonPrefix(path);
    if (commonPath.compareTo(nodePath) != 0) {
      // path diverges before the end of the extension, so it cannot match
      return NULL_NODE_RESULT;
    }
    final Bytes pathSuffix = path.slice(commonPath.size());
    final byte childIndex = pathSuffix.get(0);
    return branchNode.child(childIndex).accept(this, pathSuffix.slice(1));
  }

  /**
   * Visits a LeafNode to determine the matching node based on a given path.
   *
   * @param leafNode The LeafNode being visited.
   * @param path The path to search in the tree.
   * @return The matching node or NULL_NODE_RESULT if not found.
   */
  @Override
  public Node<V> visit(LeafNode<V> leafNode, Bytes path) {
    final Bytes leafPath = leafNode.getPath();
    final Bytes commonPath = leafPath.commonPrefix(path);
    if (commonPath.compareTo(leafPath) != 0) {
      return NULL_NODE_RESULT;
    }
    return leafNode;
  }

  /**
   * Visits a NullNode to determine the matching node based on a given path.
   *
   * @param nullNode The NullNode being visited.
   * @param path The path to search in the tree.
   * @return The NULL_NODE_RESULT since NullNode represents a missing node on the path.
   */
  @Override
  public Node<V> visit(NullNode<V> nullNode, Bytes path) {
    return NULL_NODE_RESULT;
  }
}
