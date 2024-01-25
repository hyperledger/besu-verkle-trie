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
package org.hyperledger.besu.ethereum.trie.verkle.util;

import org.apache.tuweni.units.bigints.UInt256;

public class Parameters {
  public static final UInt256 VERSION_LEAF_KEY = UInt256.valueOf(0);
  public static final UInt256 BALANCE_LEAF_KEY = UInt256.valueOf(1);
  public static final UInt256 NONCE_LEAF_KEY = UInt256.valueOf(2);
  public static final UInt256 CODE_KECCAK_LEAF_KEY = UInt256.valueOf(3);
  public static final UInt256 CODE_SIZE_LEAF_KEY = UInt256.valueOf(4);
  public static final UInt256 HEADER_STORAGE_OFFSET = UInt256.valueOf(64);
  public static final UInt256 CODE_OFFSET = UInt256.valueOf(128);
  public static final UInt256 VERKLE_NODE_WIDTH = UInt256.valueOf(256);
  // TODO should be UInt256.valueOf(256).pow(31) , but there is currently a bug
  // in the testnet and instead, the other clients are using a shift left operation.
  // So we are doing a shift left to follow the testnet,
  // but this should be fixed later.
}
