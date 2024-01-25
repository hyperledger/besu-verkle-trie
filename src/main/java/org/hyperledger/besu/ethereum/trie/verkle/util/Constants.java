package org.hyperledger.besu.ethereum.trie.verkle.util;

import org.apache.tuweni.units.bigints.UInt256;

public class Constants {
    public static final UInt256 VERSION_LEAF_KEY = UInt256.valueOf(0);
    public static final UInt256 BALANCE_LEAF_KEY = UInt256.valueOf(1);
    public static final UInt256 NONCE_LEAF_KEY = UInt256.valueOf(2);
    public static final UInt256 CODE_KECCAK_LEAF_KEY = UInt256.valueOf(3);
    public static final UInt256 CODE_SIZE_LEAF_KEY = UInt256.valueOf(4);
    public static final UInt256 HEADER_STORAGE_OFFSET = UInt256.valueOf(64);
    public static final UInt256 CODE_OFFSET = UInt256.valueOf(128);
    public static final UInt256 VERKLE_NODE_WIDTH = UInt256.valueOf(256);
}
