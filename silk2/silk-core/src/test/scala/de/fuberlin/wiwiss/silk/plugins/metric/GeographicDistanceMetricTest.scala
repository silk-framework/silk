package de.fuberlin.wiwiss.silk.plugins.metric

object GeographicDistanceMetricTest {
  def aboutEquals(x: Double, y: Double): Boolean = {
    val epsilon = 0.0001;
    return (x - y) < epsilon || (y - x) < epsilon;
  }

  def main(args: Array[String]) {
    val distances = Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val t = 0.9
    //		println(new GeographicDistanceMetric(Map.empty[String,String]).thresholdLogistic(50,40,20));
    //		println(new GeographicDistanceMetric(Map.empty[String,String]).thresholdLogistic(50,60,20));

    //		for(distance <- distances)
    //		{
    //			assert(new GeographicDistanceMetric(Map.empty[String,String]).evaluate(distance)==distance);
    //			assert(aboutEquals(new GeographicDistanceMetric(Map("threshold"->"10","curveStyle"->"linear")).evaluate(distance),(10-distance)/10.0));
    //			println(new GeographicDistanceMetric(Map("threshold"->"10","curveStyle"->"logistic")).evaluate(distance));
    //
    //		}

    // distance between (0,0) and (180,0)
    println(new GeographicDistanceMetric().evaluate("POINT(0 0)", "POINT(180 0)"))
    // distance between London and Berlin in km
    println(new GeographicDistanceMetric(unit = "kilometer").evaluate("POINT(-0.124722 51.5081)", "POINT(13.3989 52.5006)"))
    // between London and London
    println(new GeographicDistanceMetric().evaluate("POINT(-0.124722 51.5081)", "POINT(-0.124722 51.5081)"))
    // between London and London, some insignificant digits changed
    println(new GeographicDistanceMetric().evaluate("POINT(-0.124 51.4)", "POINT(-0.124722 51.5081)"))

    // distance between London and Berlin in km
    val metric = new GeographicDistanceMetric()
    println(metric.evaluate("POINT(-0.124722 51.5081)", "POINT(13.3989 52.5006)"))
    println(metric.indexValue("POINT(-0.124722 51.5081)", 900.0))
    println(metric.indexValue("POINT(13.3989 52.5006)", 900.0))
    println(metric.indexValue("POINT(-0.124722 51.5081)", 300.0))
    println(metric.indexValue("POINT(13.3989 52.5006)", 300.0))
  }
}