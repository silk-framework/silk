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

package org.silkframework.entity

import scala.math.max

class Index private(private val indices: Set[Seq[Int]], private val sizes: Seq[Int]) {

  /** The number of index values in this index. */
  def size: Int = indices.size

  /** The number of dimensions of this index. */
  def dimensions: Int = sizes.size

  /** Tests whether this index is empty, i.e., does not contain any index value. */
  def isEmpty: Boolean = indices.isEmpty

  /**
   * Returns a human-readable description of this index.
   */
  override def toString: String = indices.toString

  /**
   * True indices match if they share at least one index value.
   */
  def matches(other: Index): Boolean = indices.exists(other.indices)

  /**
   * Tests if two indices are identical.
   */
  override def equals(other: Any): Boolean = other match {
    case o: Index => indices == o.indices
    case _ => false
  }

  override def hashCode(): Int = {
    indices.hashCode()
  }

  /**
   * Flattens the index vectors to scalars in the range [0, Int.MaxValue].
   */
  def flatten: Set[Int] = {
    for (index <- indices) yield {
      val flatIndex = (index zip sizes).foldLeft(0) {
        case (iLeft, (iRight, blocks)) => iLeft * blocks + iRight
      }
      flatIndex
    }
  }

  /**
   * Combines two indices disjunctively.
   * i.e. index1 matches index3 || index2 matches index4 <=> (index1 disjunction index2) matches (index3 disjunction index4)
   */
  def disjunction(other: Index): Index = {
    val newIndexSet1 = indices.map(_.padTo(max(sizes.size, other.sizes.size), 0))
    val newIndexSet2 = other.indices.map(_.zipAll(sizes, 0, 1).map {
      case (indexValue, indexSize) => indexSize + indexValue
    })

    val combinedIndices = newIndexSet1 ++ newIndexSet2
    val combinedSizes = sizes.zipAll(other.sizes, 1, 1).map { case (c1, c2) => c1 + c2 }

    new Index(combinedIndices, combinedSizes)
  }

  /**
   * Combines two indices conjunctively.
   * i.e. index1 matches index3 && index2 matches index4 <=> (index1 conjunction index2) matches (index3 conjunction index4)
   */
  def conjunction(other: Index): Index = {
    val combinedIndices = for (index1 <- indices; index2 <- other.indices) yield index1 ++ index2
    val combinedSizes = sizes ++ other.sizes

    new Index(combinedIndices, combinedSizes)
  }

  /**
   * Merges two indices of the same dimension.
   */
  def merge(other: Index): Index = {
    require(sizes.size == other.sizes.size, "Indexes must have same number of dimensions")

    new Index(
      indices = indices ++ other.indices,
      sizes = for((s1, s2) <- sizes zip other.sizes) yield max(s1, s2)
    )
  }

  /** Take maxSize index entries */
  def crop(maxSize: Int): Index = new Index(indices.take(maxSize), sizes)
}

object Index {

  def empty: Index = new Index(Set.empty, Seq(1))

  def default: Index = new Index(Set(Seq(0)), Seq(1))

  def blocks(blocks: Set[Int]): Index = {
    new Index(
      indices = blocks.map(block => if(block == Int.MinValue) Seq(0) else if(block == Int.MaxValue) Seq(Int.MaxValue - 1) else Seq(block.abs)),
      sizes = Seq(Int.MaxValue))
  }

  def multiDim(indices: Set[Seq[Int]], dimCount: Int): Index = {
    new Index(
      indices = indices,
      sizes = Seq.fill(dimCount)(Int.MaxValue)
    )
  }

  def multiDim(indices: Set[Seq[Int]], sizes: Seq[Int]): Index = new Index(indices, sizes)

  def oneDim(indices: Set[Int]): Index = new Index(indices.map(i => Seq(i)), Seq(Int.MaxValue))

  def oneDim(indices: Set[Int], size: Int): Index = new Index(indices.map(Seq(_)), Seq(size))

  @inline
  def continuous(value: Double, minValue: Double, maxValue: Double, limit: Double, overlap: Double = 0.5): Index = {
    val blockCount = ((maxValue - minValue) / limit * overlap).toInt
    continuous(value, minValue, maxValue, blockCount, overlap)
  }

  def continuous(value: Double, minValue: Double, maxValue: Double, blockCount: Int, overlap: Double): Index = {
    val block = ((value - minValue)/ (maxValue - minValue) ) * blockCount
    val blockIndex = block.toInt

    val indices =
      if (block <= 0.5) {
        Set(0)
      }
      else if (block >= blockCount - 0.5) {
        Set(blockCount - 1)
      }
      else {
        if (block - blockIndex < overlap)
          Set(blockIndex, blockIndex - 1)
        else if (block + 1 - blockIndex < overlap)
          Set(blockIndex, blockIndex + 1)
        else
          Set(blockIndex)
      }

    oneDim(indices, blockCount)
  }

  def continuousEmpty(minValue: Double, maxValue: Double, limit: Double, overlap: Double = 0.5): Index = {
    val blockCount = ((maxValue - minValue) / limit * overlap).toInt
    oneDim(Set.empty, blockCount)
  }
}

