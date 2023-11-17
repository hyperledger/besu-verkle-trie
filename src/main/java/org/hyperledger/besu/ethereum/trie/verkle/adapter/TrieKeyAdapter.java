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
package org.hyperledger.besu.ethereum.trie.verkle.adapter;

import org.hyperledger.besu.ethereum.trie.verkle.hasher.Hasher;

import java.util.ArrayList;
import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;

/**
 * Utility class for generating keys used in a Verkle Trie.
 *
 * <p>The `TrieKeyAdapter` class provides methods for generating various keys, such as storage keys,
 * code chunk keys, and header keys, used in a Verkle Trie structure.
 */
public class TrieKeyAdapter {
  private final UInt256 VERSION_LEAF_KEY = UInt256.valueOf(0);
  private final UInt256 BALANCE_LEAF_KEY = UInt256.valueOf(1);
  private final UInt256 NONCE_LEAF_KEY = UInt256.valueOf(2);
  private final UInt256 CODE_KECCAK_LEAF_KEY = UInt256.valueOf(3);
  private final UInt256 CODE_SIZE_LEAF_KEY = UInt256.valueOf(4);
  private final UInt256 HEADER_STORAGE_OFFSET = UInt256.valueOf(64);
  private final UInt256 CODE_OFFSET = UInt256.valueOf(128);
  private final UInt256 VERKLE_NODE_WIDTH = UInt256.valueOf(256);
  private final UInt256 MAIN_STORAGE_OFFSET = UInt256.valueOf(256).pow(31);

  private final Hasher hasher;

  /**
   * Creates a TrieKeyAdapter with the provided hasher.
   *
   * @param hasher The hasher used for key generation.
   */
  public TrieKeyAdapter(Hasher hasher) {
    this.hasher = hasher;
  }

  /**
   * Swaps the last byte of the base key with a given subIndex.
   *
   * @param base The base key.
   * @param subIndex The subIndex.
   * @return The modified key.
   */
  Bytes32 swapLastByte(Bytes32 base, UInt256 subIndex) {
    Bytes lastByte = Bytes.of(subIndex.toBytes().reverse().get(0));
    return (Bytes32) Bytes.concatenate(base.slice(0, 31), lastByte);
  }

  /**
   * Generates a storage key for a given address and storage key.
   *
   * @param address The address.
   * @param storageKey The storage key.
   * @return The generated storage key.
   */
  public Bytes32 storageKey(Bytes address, Bytes32 storageKey) {
    UInt256 index = UInt256.fromBytes(storageKey);
    UInt256 headerOffset = CODE_OFFSET.subtract(HEADER_STORAGE_OFFSET);
    UInt256 offset =
        ((index.compareTo(headerOffset) < 0) ? HEADER_STORAGE_OFFSET : MAIN_STORAGE_OFFSET);
    UInt256 pos = offset.add(index);
    Bytes32 base = hasher.trieKeyHash(address, pos.divide(VERKLE_NODE_WIDTH));
    Bytes32 key = swapLastByte(base, pos.mod(VERKLE_NODE_WIDTH));
    return key;
  }

  /**
   * Generates a code chunk key for a given address and chunkId.
   *
   * @param address The address.
   * @param chunkId The chunk ID.
   * @return The generated code chunk key.
   */
  public Bytes32 codeChunkKey(Bytes address, int chunkId) {
    return codeChunkKey(address, UInt256.valueOf(chunkId));
  }

  /**
   * Generates a code chunk key for a given address and chunkId.
   *
   * @param address The address.
   * @param chunkId The chunk ID.
   * @return The generated code chunk key.
   */
  public Bytes32 codeChunkKey(Bytes address, UInt256 chunkId) {
    UInt256 pos = CODE_OFFSET.add(chunkId);
    Bytes32 base = hasher.trieKeyHash(address, pos.divide(VERKLE_NODE_WIDTH).toBytes());
    Bytes32 key = swapLastByte(base, pos.mod(VERKLE_NODE_WIDTH));
    return key;
  }

  /**
   * Generates a header key for a given address and leafKey.
   *
   * @param address The address.
   * @param leafKey The leaf key.
   * @return The generated header key.
   */
  Bytes32 headerKey(Bytes address, UInt256 leafKey) {
    Bytes32 base = hasher.trieKeyHash(address, UInt256.valueOf(0).toBytes());
    Bytes32 key = swapLastByte(base, leafKey);
    return key;
  }

  /**
   * Generates a version key for a given address.
   *
   * @param address The address.
   * @return The generated version key.
   */
  public Bytes32 versionKey(Bytes address) {
    return headerKey(address, VERSION_LEAF_KEY);
  }

  /**
   * Generates a balance key for a given address.
   *
   * @param address The address.
   * @return The generated balance key.
   */
  public Bytes32 balanceKey(Bytes address) {
    return headerKey(address, BALANCE_LEAF_KEY);
  }

  /**
   * Generates a nonce key for a given address.
   *
   * @param address The address.
   * @return The generated nonce key.
   */
  public Bytes32 nonceKey(Bytes address) {
    return headerKey(address, NONCE_LEAF_KEY);
  }

  /**
   * Generates a code Keccak key for a given address.
   *
   * @param address The address.
   * @return The generated code Keccak key.
   */
  public Bytes32 codeKeccakKey(Bytes address) {
    return headerKey(address, CODE_KECCAK_LEAF_KEY);
  }

  /**
   * Generates a code size key for a given address.
   *
   * @param address The address.
   * @return The generated code size key.
   */
  public Bytes32 codeSizeKey(Bytes address) {
    return (headerKey(address, CODE_SIZE_LEAF_KEY));
  }

  /**
   * Chunk code's bytecode for insertion in the Trie. Each chunk code uses its position in the list
   * as chunkId
   *
   * @param bytecode Code's bytecode
   * @return List of 32-bytes code chunks
   */
  public List<Bytes32> chunkifyCode(Bytes bytecode) {
    if (bytecode.isEmpty()) {
      return new ArrayList<Bytes32>();
    }

    // Chunking variables
    int CHUNK_SIZE = 31;
    int nChunks = 1 + ((bytecode.size() - 1) / CHUNK_SIZE);
    int padSize = nChunks * CHUNK_SIZE - bytecode.size();
    Bytes code = Bytes.concatenate(bytecode, Bytes.repeat((byte) 0, padSize));
    List<Bytes32> chunks = new ArrayList<Bytes32>(nChunks);

    // OpCodes for PUSH's
    int PUSH_OFFSET = 95;
    int PUSH1 = PUSH_OFFSET + 1;
    int PUSH32 = PUSH_OFFSET + 32;

    // Iterator data
    int chunkPos = 0; // cursor position to start of current chunk
    int posInChunk = 0; // cursor position relative to the current chunk
    int nPushData = 0; // number of bytes in current push data

    // Create chunk iteratively
    for (int chunkId = 0; chunkId < nChunks; ++chunkId) {
      chunkPos = chunkId * CHUNK_SIZE;
      posInChunk = nPushData;
      while (posInChunk < CHUNK_SIZE) {
        int opCode = Byte.toUnsignedInt(code.get(chunkPos + posInChunk));
        posInChunk += 1;
        if (PUSH1 <= opCode && opCode <= PUSH32) {
          posInChunk += opCode - PUSH_OFFSET;
        }
      }
      chunks.add(
          (Bytes32) Bytes.concatenate(Bytes.of(nPushData), code.slice(chunkPos, CHUNK_SIZE)));
      nPushData = posInChunk - CHUNK_SIZE;
    }

    return chunks;
  }
}
