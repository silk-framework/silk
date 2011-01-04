package de.fuberlin.wiwiss.silk

import math._

object Test
{
  def main(args : Array[String])
  {
    val distInMeters = 6371.0
    val earthCircumferenceEquatorial = 40075160.0
    val earthCircumferenceMeridional = 40008000.0

    val offset = 45.0

    val maxLatDiff = 360.0 * (distInMeters / earthCircumferenceEquatorial)
    val maxLongDiff = 360.0 * (distInMeters / earthCircumferenceMeridional) * cos(deg2rad(offset))

    val dLat = getGeometricDistance(GeoPoint(0.0, offset), GeoPoint(maxLatDiff, offset))
    val dLong = getGeometricDistance(GeoPoint(offset, 0.0), GeoPoint(offset, maxLongDiff))

    println(dLat)
    println(dLong)
  }


  /**
   * Computes the distance between two geo points.
   */
  private def getGeometricDistance(loc1 : GeoPoint, loc2 : GeoPoint): Double =
  {
    // formula from http://www.zipcodeworld.com/samples/distance.java.html
    val theta = loc1.long - loc2.long // 180
    var dist = sin(deg2rad(loc1.lat)) * sin(deg2rad(loc2.lat)) + cos(deg2rad(loc1.lat)) * cos(deg2rad(loc2.lat)) * cos(deg2rad(theta))
    dist = acos(dist)
    dist = rad2deg(dist)
    dist = dist * 60 * 1.1515 * 1.609344 * 1000; // in meters
    return (dist);
  }

  /**
   * Converts decimal degrees to radians.
   */
  private def deg2rad(deg : Double) : Double =
  {
    return (deg * java.lang.Math.PI / 180.0)
  }

  /**
   * Converts radians to decimal degrees.
   */
  private def rad2deg(rad : Double) : Double =
  {
    return (rad * 180.0 / java.lang.Math.PI)
  }

  /**
   * Represents a geographical point.
   */
  private case class GeoPoint(lat : Double, long : Double)
}