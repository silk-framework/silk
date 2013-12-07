package de.fuberlin.wiwiss.silk.plugins.transformer

import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import java.net.{URLEncoder, HttpURLConnection, URL}
import xml.Elem
import collection.mutable.{ArrayBuffer, HashSet, Set => MSet}
import java.lang.ConditionalSpecialCasing
import de.fuberlin.wiwiss.silk.plugins.distance.tokenbased.CosineDistanceMetric

/**
 * Created by IntelliJ IDEA.
 * User: andreas
 * Date: 5/16/12
 * Time: 12:22 PM
 * To change this template use File | Settings | File Templates.
 */

@Plugin(
  id = "spotlight",
  label = "Spotlight",
  description = "Concatenates all values to a string and gets a weighted entity vector from the Spotlight service."
)
class SpotlightTextVectorTransformer extends Transformer {
  def apply(values: Seq[Set[String]]): Set[String] = {
    val stringSet = values.reduce(_ union _)
    if(stringSet.size==0)
      return Set[String]()
    val query = if(stringSet.size>1)
      stringSet.reduceLeft(_ + " " + _)
    else
      stringSet.toSet.head
    SpotlightClient.querySpotlight(query)
  }
}

object SpotlightClient {
  val baseURL = "http://160.45.137.71:2222/extract?text="

  def querySpotlight(query: String): Set[String] = {
    val url = new URL(baseURL + URLEncoder.encode(query, "UTF-8"))
    val conn = url.openConnection().asInstanceOf[HttpURLConnection]
    conn.setRequestMethod("GET")
    conn.setRequestProperty("Accept", "text/xml")
    conn.connect()
    val rc = conn.getResponseCode
    if (rc < 200 || rc >= 300) {
      System.err.println("Query execution: Received error code " + rc + " from server")
      System.err.println("Error message: " + conn.getResponseMessage + "\n\nFor query: \n")
      System.err.println(query + "\n")
    }
    val is = conn.getInputStream
    if(is==null)
      return Set[String]()
    val root = xml.XML.load(is)
    Set(createEntityString(root))
  }

  // Converts the elements to "resource simScore;resource simScore..." strings
  private def createEntityString(root: Elem): String = {
    val tempResources = new ArrayBuffer[(String, Double)]
    val sb = new StringBuilder

    for(resource <- root \ "Resources" \ "Resource")
      tempResources += Pair(resource.text, (resource \ "@similarityScore").text.toDouble)
    var first = true
    for((resource, score) <- normalize(tempResources)) {
      if(!first)
        sb.append(";")
      else
        first = false
      sb.append(resource).append(" ").append(score.toString)
    }
    sb.toString
  }

  def normalize(vector: Seq[(String, Double)]): Seq[(String, Double)] = {
    var factor = 0.0
    for((resource, weight) <- vector)
      factor += math.pow(weight, 2.0)
    factor = math.sqrt(factor)
    for((resource, weight) <- vector)
      yield (resource, weight/factor)
  }
}
