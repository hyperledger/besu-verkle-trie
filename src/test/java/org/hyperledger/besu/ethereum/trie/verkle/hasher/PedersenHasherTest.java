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
package org.hyperledger.besu.ethereum.trie.verkle.hasher;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.junit.jupiter.api.Test;

public class PedersenHasherTest {

  PedersenHasher hasher = new PedersenHasher();

  // Pulled out of geth, good test case to compare in case of breaking changes on key generation
  @Test
  public void testGetStem() {
    byte[] addr = new byte[32];
    for (int i = 0; i < 16; i++) {
      addr[1 + 2 * i] = (byte) 0xff;
    }

    Bytes address = Bytes.wrap(addr);

    BigInteger n = BigInteger.ONE;
    n = n.shiftLeft(129);
    n = n.add(BigInteger.valueOf(3));
    Bytes32 index = UInt256.valueOf(n).toBytes();
    Bytes tk = hasher.computeStem(address, index);
    String got = tk.toHexString();
    String exp = "0x6ede905763d5856cd2d67936541e82aa78f7141bf8cd5ff6c962170f3e9dc2";

    assertEquals(exp, got);
  }
}
