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
package org.hyperledger.besu.ethereum.trie.verkle.exceptions;

import org.apache.tuweni.bytes.Bytes;

/** This exception is thrown when there is an issue retrieving or decoding values from Storage */
public class VerkleTrieException extends RuntimeException {
  /** Location at which the Exception occurs */
  private Bytes location;

  /**
   * Constructs a VerkleTrieException.
   *
   * @param message Exception's messasge.
   */
  public VerkleTrieException(final String message) {
    super(message);
  }

  /**
   * Constructs a VerkleTrieException at location.
   *
   * @param message Exception's messasge.
   * @param location Exception occured at location.
   */
  public VerkleTrieException(final String message, final Bytes location) {
    super(message);
    this.location = location;
  }

  /**
   * Constructs a VerkleTrieException throwned from another exception.
   *
   * @param message Exception's messasge.
   * @param cause Exception from which this exception was throwned.
   */
  public VerkleTrieException(final String message, final Exception cause) {
    super(message, cause);
  }

  /**
   * Location at which the exception occured
   *
   * @return the location at which the exception occured
   */
  public Bytes getLocation() {
    return location;
  }
}
