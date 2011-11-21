/* 
 * Copyright 2009-2011 Freie Universität Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.entity

import math.{min, max}

class Index private(private val indices: Set[Seq[Int]], private val sizes: Seq[Int]) {
  override def equals(other: Any) = other match {
    case o: Index => indices == o.indices
    case _ => false
  }

  override def toString = indices.toString

  def intersect(other: Index): Boolean = !(indices intersect other.indices).isEmpty

  def isEmpty = indices.isEmpty

  def flatten: Set[Int] = {
    //Convert the index vectors to scalars in the range [0, Int.MaxValue]
    //TODO allow negative values
    for (index <- indices) yield {
      val flatIndex = (index zip sizes).foldLeft(0) {
        case (iLeft, (iRight, blocks)) => iLeft * blocks + iRight
      }

      if (flatIndex == Int.MinValue) 0 else flatIndex.abs
    }
  }

  def disjunction(other: Index) = {
    val newIndexSet1 = indices.map(_.padTo(max(sizes.size, other.sizes.size), 0))
    val newIndexSet2 = other.indices.map(_.zipAll(other.sizes, 0, 0).map {
      case (indexValue, blockCount) => blockCount + indexValue
    })

    val combinedIndices = newIndexSet1 ++ newIndexSet2
    val combinedSizes = other.sizes.zipAll(other.sizes, 0, 0).map { case (c1, c2) => c1 + c2 }

    new Index(combinedIndices, combinedSizes)
  }

  def conjunction(other: Index) = {
    val indexes1 = if (indices.isEmpty) Set(Seq.fill(sizes.size)(0)) else indices
    val indexes2 = if (other.indices.isEmpty) Set(Seq.fill(other.sizes.size)(0)) else other.indices

    val combinedIndices = for (index1 <- indexes1; index2 <- indexes2) yield index1 ++ index2
    val combinedSizes = sizes ++ other.sizes

    new Index(combinedIndices, combinedSizes)
  }

  def merge(other: Index) = {
    require(sizes.size == other.sizes.size, "Indexes must have same number of dimensions")

    new Index(
      indices = indices ++ other.indices,
      sizes = for((s1, s2) <- sizes zip other.sizes) yield max(s1, s2)
    )
  }

  def crop(maxSize: Int) = new Index(indices.take(maxSize), sizes)
}

object Index {

  private val maxBlockCount = 10000

  private val overlap = 0.5

  def empty = new Index(Set.empty, Seq(1))

  def default = new Index(Set(Seq(0)), Seq(1))

  def multiDim(indices: Set[Seq[Int]], sizes: Seq[Int]) = new Index(indices, sizes)

  def oneDim(indices: Set[Int]) = new Index(indices.map(i => Seq((i % maxBlockCount).abs)), Seq(maxBlockCount))

  def oneDim(indices: Set[Int], size: Int) = new Index(indices.map(Seq(_)), Seq(size))

  def continuous(value: Double, minValue: Double, maxValue: Double, limit: Double): Index = {
    val blockCount = min(maxBlockCount, ((maxValue - minValue) / limit * overlap).toInt)
    val block = (value - minValue) * blockCount
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
}

