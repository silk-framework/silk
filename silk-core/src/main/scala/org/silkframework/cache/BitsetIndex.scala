/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.cache

import java.io.{DataInput, DataOutput}

/**
 * Efficient index.
 */
final class BitsetIndex private(private val index: Set[Int], private val bitset: Array[Long]) {

  private val mask = {
    var m = 0L
    var i = 0
    while(i < 64) {
      if(bitset(i) != 0)
        m |= (1L << i)
      i += 1
    }
    m
  }

  /**
   * Checks if this index matches another index.
   */
  def matches(other: BitsetIndex) = {
    (mask & other.mask) != 0 && bitsetMatches(other) && indexMatches(other)
  }

  @inline
  private def indexMatches(other: BitsetIndex) = {
    !(index intersect other.index).isEmpty
  }

  @inline
  private def bitsetMatches(other: BitsetIndex) = {
    var found = false
    var i = 0
    while (!found && i < BitsetIndex.Size) {
      found = (bitset(i) & other.bitset(i)) != 0
      i += 1
    }

    found
  }

  def serialize(stream: DataOutput) {
    stream.writeInt(index.size)
    index.foreach(stream.writeInt)
    bitset.foreach(stream.writeLong)
  }
}

object BitsetIndex {
  /**
   * Size of the index i.e. the number of long integers used to represent the bit set.
   */
  private val Size = 64

  def build(index: Set[Int]) = {
    val array = new Array[Long](Size)

    for (i <- index) {
      val ci = i % (Size * 64)

      array(ci >> 6) |= (1L << ci)
    }

    new BitsetIndex(index, array)
  }

  def deserialize(stream: DataInput) = {
    val indexSize = stream.readInt()
    val index = Array.fill(indexSize)(stream.readInt).toSet
    val bitset = Array.fill(Size)(stream.readLong)

    new BitsetIndex(index, bitset)
  }
}