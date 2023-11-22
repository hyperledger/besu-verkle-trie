package org.hyperledger.besu.ethereum.trie.verkle;

import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.ethereum.trie.verkle.factory.StoredNodeFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class NodeDirtyTest {

    @Test
    public void testOneValueSimpleVerkleTrie() {
        NodeUpdaterMock nodeUpdater = new NodeUpdaterMock();
        SimpleVerkleTrie<Bytes32, Bytes32> trie = new SimpleVerkleTrie<>();
        Bytes32 key = Bytes32.fromHexString("0x00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
        Bytes32 value = Bytes32.fromHexString("0x1000000000000000000000000000000000000000000000000000000000000000");
        trie.put(key, value);

        assertTrue(trie.getRoot().isDirty());
        trie.commit(nodeUpdater);
        assertFalse(trie.getRoot().isDirty());
    }

    @Test
    public void testOneValueStoredVerkleTrie() {
        NodeUpdaterMock nodeUpdater = new NodeUpdaterMock();
        NodeLoaderMock nodeLoader = new NodeLoaderMock(nodeUpdater.storage);
        StoredNodeFactory<Bytes32> nodeFactory = new StoredNodeFactory<>(nodeLoader, value -> (Bytes32) value);
        StoredVerkleTrie<Bytes32, Bytes32> trie = new StoredVerkleTrie<>(nodeFactory);
        Bytes32 key = Bytes32.fromHexString("0x00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
        Bytes32 value = Bytes32.fromHexString("0x1000000000000000000000000000000000000000000000000000000000000000");
        trie.put(key, value);

        trie.commit(nodeUpdater);
        assertFalse(trie.getRoot().isDirty());
    }
}
