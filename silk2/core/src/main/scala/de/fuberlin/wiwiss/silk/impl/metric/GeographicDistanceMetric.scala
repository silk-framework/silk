package de.fuberlin.wiwiss.silk.impl.metric

import math._
import de.fuberlin.wiwiss.silk.linkspec.Metric
import de.fuberlin.wiwiss.silk.util.StringUtils._

/**
 * This metric takes geographical coordinates of two points,
 * given in degrees of longitude and latitude, and creates a score measuring
 * the nearness of those two points in air line distance. The default metric is the distance of the two points in meters but its behaviour is configurable
 * via the following parameters:
 * unit = "meter" or "m" (default) , "kilometer" or "km" - the unit in which the distance is measured
 * threshold = t -  will result in a 0 for all bigger values than t, values below are varying with the curveStyle
 * curveStyle = "discreet" (default) "linear" gives a linear transition, "logistic" uses the logistic function f(x)=1/(1+e^(x) gives a more soft curve with a slow slope 
 * at the start and the end of the curve but a  steep one in the middle.
 * longitudeFirst = "true" (default) or "false" specifies if the longitude or latitude is given first
 * @autor Konrad Höffner (AKSW, Uni Leipzig)
 */
class GeographicDistanceMetric(val params : Map[String, String]) extends Metric
{
  val multipliers = Map("km"->0.001,"kilometer"->0.001,"meter"->1.0,"m"->1.0)

  val unitMultiplier : Double = multipliers.get(params.get("unit").getOrElse("")).getOrElse(1)

  val threshold : Option[Double] = params.get("threshold").map(_.toDouble)
  val curveStyle : Option[String] = params.get("curveStyle");

  val longitudeFirst = params.get("longitudeFirst").getOrElse("true").equals("true")

  override def evaluate(str1 : String, str2 : String, threshold : Double) : Double =
  {
    //Parse the coordinates and return a similarity value if both coordinates could be extracted.
    (getCoordinates(str1), getCoordinates(str2)) match
    {
      case (Some(loc1), Some(loc2)) => scale(getGeometricDistance(loc1, loc2))
      case _ => 0.0
    }
  }

  override def index(str : String, threshold : Double) : Set[Seq[Int]] =
  {
    getCoordinates(str) match
    {
      case Some(coords) =>
      {
        val latIndex = (coords.lat + 90.0) / 180.0
        val longIndex = (coords.long + 180.0) / 360.0

        Set(Seq((latIndex * 45).toInt, (longIndex * 90).toInt))
      }
      case None => Set.empty
    }
  }

  override val blockCounts : Seq[Int] =
  {
    Seq(45, 90)
  }

  /**
   * Extracts the latitude and longitude from a string and takes their order from the parameter "longitudeFirst".
   *
   * @param s The WGS84 coordinate string. Examples from DBpedia are:
   *          - POINT(-0.124722 51.5081) // first longitude then latitude, used by <http://www.w3.org/2003/01/geo/wgs84_pos#geometry>
   *          - 51.50805555555556 -0.12472222222222222 // first latitude then longitude, used by <http://www.georss.org/georss/point>
   * @return  The coordinate if it could be extracted. None otherwise.
   */
  private def getCoordinates(s : String) : Option[GeoPoint] =
  {
    val allowedCharacters = Set('0','1','2','3','4','5','6','7','8','9','.','-','+',' ')
    val numbers = s.filter(allowedCharacters contains _)

    val geoPoint = numbers.split(" ") match
    {
      case Array(DoubleLiteral(c1), DoubleLiteral(c2)) =>
      {
        if(longitudeFirst)
          Some(new GeoPoint(c2, c1))
        else
          Some(new GeoPoint(c1, c2))
      }
      case _ => None
    }

    geoPoint
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
    dist = dist * unitMultiplier
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
   * Scales the input distance to a similarity value between 0.0 and 1.0.
   */
  private def scale(distance : Double) : Double =
  {
    // no threshold defined -> just return the distance
    if(!threshold.isDefined) {return distance;}

    // a threshold is used, first check for boundary cases
    if(distance==0) {return 1}
    if(distance>=threshold.get) {return 0}

    // curveStyle = "discreet" or no curveStyle set
    // -> no curve, just return 1
    if(!curveStyle.isDefined||curveStyle.get.equals("discreet")) {return 1}

    // other curveStyle specified -> use a transition

    // linear transition
    if(curveStyle.get.equals("linear"))
    {

      val m = - 1/threshold.get;
      return 1 + m * distance;
    }

    // logistic transition
    return 1 / (1+ exp((distance)*10/threshold.get-5));
  }

  /**
   * Represents a geographical point.
   */
  private class GeoPoint(val lat : Double, val long : Double)
}
