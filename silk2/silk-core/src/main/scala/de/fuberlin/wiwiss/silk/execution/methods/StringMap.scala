package de.fuberlin.wiwiss.silk.execution.methods

import de.fuberlin.wiwiss.silk.linkagerule.similarity.DistanceMeasure
import de.fuberlin.wiwiss.silk.execution.ExecutionMethod
import de.fuberlin.wiwiss.silk.cache.Partition
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.entity.{Index, Path, Entity}
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.execution.methods.StringMap.Mapper
import de.fuberlin.wiwiss.silk.plugins.distance.characterbased.LevenshteinDistance

case class StringMap(sourceKey: Path, targetKey: Path, distThreshold: Int = 2) extends ExecutionMethod {

  /**
   * Generates an index for a single entity.
   * StringMap don't uses the indexing.
   */
  override def indexEntity(entity: Entity, rule: LinkageRule): Index = Index.default

  /**
   * Generates comparison pairs from two partitions.
   */
  override def comparisonPairs(sourcePartition: Partition, targetPartition: Partition, full: Boolean) = {

    val sourceValues = sourcePartition.entities.map(_.evaluate(sourceKey))
    val targetValues = targetPartition.entities.map(_.evaluate(targetKey))

    // Initialize the string map implementation
    val sm = new Mapper(
      stringVector = sourceValues ++ targetValues,
      distanceMetric = LevenshteinDistance(),
      dimensionality = 20
    )

    // Compute the threshold in the mapped space that corresponds to the specified distance threshold
    val mappedThreshold = sm.computeThreshold(sourceValues, 0.5, targetValues, 0.5, distThreshold)

    // Return a traversable of all pairs which are closer than the mapped threshold
    new Traversable[DPair[Entity]] {
      def foreach[U](f: DPair[Entity] => U) {
        for(i <- 0 until sourceValues.size;
            j <- 0 until targetValues.size
            if sm.mappedDistance(sm.coordinates(i), sm.coordinates(sourceValues.size + j)) < mappedThreshold) yield {
          f(DPair(sourcePartition.entities(i), targetPartition.entities(j)))
        }
      }
    }
  }
}

object StringMap {

  /**
   * StringMap algorithm as proposed in:
   *
   * "Efficient Record Linkage in Large Data Sets," by Liang Jin, Chen Li, and Sharad Mehrotra,
   * in 8th International Conference on Database Systems for Advanced Applications (DASFAA) 2003, Kyoto, Japan.
   *
   * Partly based on the original implementation by the same authors from:
   *
   * http://flamingo.ics.uci.edu/releases/4.1/
   *
   * @param stringVector The string values that are to be mapped.
   * @param distanceMetric The metric that assigns a distance value to each pair of string values.
   * @param dimensionality The number of dimensions of the mapped space.
   */
  class Mapper(stringVector: Array[Set[String]], distanceMetric: DistanceMeasure, dimensionality: Int = 20) {

    private val size = stringVector.size

    private val coordinatesVector = Array.fill(size)(Array.fill(dimensionality)(0.0))

    private val pivotsA = Array.fill(dimensionality)(0)
    private val pivotsB = Array.fill(dimensionality)(0)

    // Execute the mapping
    nextsm(dimensionality)

    /** Retrieves the mapped coordinates. */
    def coordinates = coordinatesVector

    /**
     * Computes the distance between two mapped coordinates.
     */
    def mappedDistance(c1: Array[Double], c2: Array[Double]) = {
      var sqrDist = 0.0
      for(i <- 0 until c1.size)
        sqrDist += (c1(i) - c2(i)) * (c1(i) - c2(i))
      math.sqrt(sqrDist)
    }

    /**
     * Given a distance threshold between unmapped values, returns the corresponding threshold in the mapped space.
     */
    def computeThreshold(stringVector1: Array[Set[String]], percentage1: Double,
                         stringVector2: Array[Set[String]], percentage2: Double,
                         distanceThreshold: Int) =
    {
      var mappedThreshold = 0.0

      // generate two partial lists
      val v1 = stringVector1.take((stringVector1.size * percentage1).toInt)
      val v2 = stringVector2.take((stringVector2.size * percentage2).toInt)

      // use a nested loop to find the maximal mapped distance of similar pairs
      for(s1 <- v1; s2 <- v2) {
        val dist = distanceMetric(s1, s2, distanceThreshold)
        if (dist <= distanceThreshold && dist > 0) {
          // found one
          val coordinates1 = mapValue(s1)
          val coordinates2 = mapValue(s2)
          val mappedDist = mappedDistance(coordinates1, coordinates2)
          //cout << "[" << s1 << "],[" << s2 << "] = " << dist << endl;
          if(mappedDist > mappedThreshold)
            mappedThreshold = mappedDist
          }
      }

      if (mappedThreshold == 0.0) {
        throw new Exception ("Failed to compute a new distance threshold. Possible Reasons: didn't get enough samples from the two lists.")
      }

      // increase the threshold slightly since the R-tree join uses "<" while
      // we want to use "<="
      mappedThreshold + 1e-5
    }

    /**
     * Maps a single value.
     */
    def mapValue(s: Set[String]) = {
      var coordinates = Array[Double]()

      for(dim <- 0 until dimensionality) {
        val idA = pivotsA(dim)
        val idB = pivotsB(dim)

        val dab = getDistance(stringVector(idA), coordinatesVector(idA),
                              stringVector(idB), coordinatesVector(idB),
                              dim)

        val x = getDistance(s, coordinates,
                            stringVector(idA), coordinatesVector(idA),
                            dim)

        val y = getDistance(s, coordinates,
                            stringVector(idB), coordinatesVector(idB),
                            dim)

        val coord = (x * x + dab * dab - y * y)/(2 * dab)

        coordinates = coordinates :+ coord
      }

      coordinates
    }

    private def nextsm(kd: Int) {
      if ( kd <= 0 )
        return

      //get the pivot points and record them
      val dim = dimensionality - kd
      choosePivot(dim)

      // get the IDs of the two pivots of the current dimension
      val idA = pivotsA(dim)
      val idB = pivotsB(dim)

      // get the strings of the two pivots of the current dimension
      val stringA = stringVector(idA)
      val stringB = stringVector(idB)

      // get the coordinates of the two pivots of the current dimension
      val coordindatesA = coordinatesVector(idA)
      val coordindatesB = coordinatesVector(idB)

      // compute the distance between the two pivots up to dimension "dim"
      val dab = getDistance(stringA, coordindatesA,
                            stringB, coordindatesB,
                            dim)

      // for each string, compute its dim-th coordinate
      for(i <- 0 until size) {
        val coord =
          if (dab == 0.0)
            0.0
          else {
            val x = getDistance(stringVector(i), coordinatesVector(i),
                 stringA, coordindatesA,
                 dim)
            val y = getDistance(stringVector(i), coordinatesVector(i),
                 stringB, coordindatesB,
                 dim)

            (x * x + dab * dab - y * y)/(2.0 * dab)
          }

        // set the dim-th coordinate of this string
        coordinatesVector(i)(dim) = coord
      }

      nextsm(kd-1)
    }

    private def choosePivot(dim: Int) {
      var seeda: Int = 0
      var seedb: Int = dim //The original algorithm chooses a random index here. We make it deterministic by choosing the same indices each time.

      for (i <- 0 until 5) {
        seeda = findFarthestPoint(seedb, dim)
        seedb = findFarthestPoint(seeda, dim)
      }

      pivotsA(dim) = seeda
      pivotsB(dim) = seedb
    }

    // find a point farthest to the point id using distances up to "dimLimit"
    private def findFarthestPoint(id: Int, dimLimit: Int): Int = {
      var farthest = 0
      var longestdist = 0.0

      for(i <- 0 until size) {
        val dist = getDistance(stringVector(i), coordinatesVector(i),
                               stringVector(id), coordinatesVector(id),
                               dimLimit)
        if(dist > longestdist) {
          farthest = i
          longestdist = dist
        }
      }

      farthest
    }

  // compute the distance between two strings based on the first
  // "dimLimit" (excluding) dimensions.  "coordinates1" and
  // "coordinates2" store there coordinates of the two strings up to
  // "dimLimit - 1"
    private def getDistance(s1: Set[String], coordinates1: Array[Double], s2: Set[String], coordinates2: Array[Double], dimLimit: Int) : Double = {
      var dist = distanceMetric(s1, s2)

      for(i <- 0 until dimLimit) {
        val x = (coordinates1(i) - coordinates2(i)).abs
        dist = math.sqrt((x * x - dist * dist).abs)
      }

      dist
    }
  }
}
