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

import org.hyperledger.besu.ethereum.trie.verkle.node.InternalNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.Node;
import org.hyperledger.besu.ethereum.trie.verkle.node.NullLeafNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.NullNode;
import org.hyperledger.besu.ethereum.trie.verkle.node.StemNode;

import org.apache.tuweni.bytes.Bytes;

/**
 * Class representing a visitor for traversing nodes in a Trie tree to find a node based on a path.
 *
 * @param <V> The type of node values.
 */
public class GetVisitor<V> implements PathNodeVisitor<V> {
  private final Node<V> NULL_NODE_RESULT = NullLeafNode.instance();

  /**
   * Visits a internalNode to determine the node matching a given path.
   *
   * @param internalNode The internalNode being visited.
   * @param path The path to search in the tree.
   * @return The matching node or NULL_NODE_RESULT if not found.
   */
  @Override
  public Node<V> visit(final InternalNode<V> internalNode, final Bytes path) {
    final byte childIndex = path.get(0);
    return internalNode.child(childIndex).accept(this, path.slice(1));
  }

  /**
   * Visits a stemNode to determine the node matching a given path.
   *
   * @param stemNode The stemNode being visited.
   * @param path The path to search in the tree.
   * @return The matching node or NULL_NODE_RESULT if not found.
   */
  @Override
  public Node<V> visit(final StemNode<V> stemNode, final Bytes path) {
    final Bytes extension = stemNode.getPathExtension().get();
    final int prefix = path.commonPrefixLength(extension);
    if (prefix < extension.size()) {
      return NULL_NODE_RESULT;
    }
    final byte childIndex = path.get(prefix); // extract suffix
    return stemNode.child(childIndex).accept(this, path.slice(prefix + 1));
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

  /**
   * Visits a NullLeafNode to determine the matching node based on a given path.
   *
   * @param nullLeafNode The NullLeafNode being visited.
   * @param path The path to search in the tree.
   * @return NullLeafNode represents a missing leaf node on the path.
   */
  @Override
  public Node<V> visit(NullLeafNode<V> nullLeafNode, Bytes path) {
    return NULL_NODE_RESULT;
  }
}
