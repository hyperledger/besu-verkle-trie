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
package org.hyperledger.besu.ethereum.trie.verkle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;

public class GenesisTest {
  @Test
  public void putGenesis() throws IOException {
    HashMap<Bytes, Bytes> storage = new HashMap<Bytes, Bytes>();
    VerkleTrie<Bytes32, Bytes> trie = new SimpleVerkleTrie<Bytes32, Bytes>();
    InputStream input = GenesisTest.class.getResourceAsStream("/genesis.csv");
    // try (Reader reader = Files.newBufferedReader(Paths.get(csvPath));
    try (Reader reader = new InputStreamReader(input, "UTF-8");
        CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT); ) {
      for (CSVRecord csvRecord : csvParser) {
        Bytes32 key = Bytes32.fromHexString(csvRecord.get(0));
        Bytes value = Bytes.fromHexString(csvRecord.get(1));
        trie.put(key, value);
      }
    }
    trie.commit((location, hash, value) -> storage.put(location, value));
    Bytes32 expected =
        Bytes32.fromHexString("0x6e077a5ba3d6b0db91ed0c35b6bb6916981d1247a2b85e811a97f400ccc0ab1c");
    assertThat(trie.getRootHash()).isEqualTo(expected);
  }
}
