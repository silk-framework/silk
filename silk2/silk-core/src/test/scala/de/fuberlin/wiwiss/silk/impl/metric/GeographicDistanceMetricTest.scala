package de.fuberlin.wiwiss.silk.impl.metric

object GeographicDistanceMetricTest
{
	def aboutEquals(x : Double,y : Double) : Boolean =
	{
		val epsilon = 0.0001;
		return (x-y)<epsilon||(y-x)<epsilon;
	}
	
	def main(args: Array[String])
	{
		val distances = Array(0,1,2,3,4,5,6,7,8,9,10)
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
		println(new GeographicDistanceMetric(threshold = 50.0).evaluate("POINT(0 0)","POINT(180 0)", 0.9))
		// distance between London and Berlin in Kilometers
		println(new GeographicDistanceMetric(threshold = 50.0, unit = "kilometer").evaluate("POINT(-0.124722 51.5081)","POINT(13.3989 52.5006)", 0.9))
		// between London and London
		println(new GeographicDistanceMetric(threshold = 50.0).evaluate("POINT(-0.124722 51.5081)","POINT(-0.124722 51.5081)", 0.9))
		// between London and London, some insignificant digits changed
		println(new GeographicDistanceMetric(threshold = 50.0).evaluate("POINT(-0.124 51.4)","POINT(-0.124722 51.5081)", 0.9))

		// distance between London and Berlin with parameter
		println(new GeographicDistanceMetric(threshold = 50.0, unit = "km").evaluate("POINT(-0.124722 51.5081)","POINT(13.3989 52.5006)", 0.9))
		// distance between London and Berlin with threshold 50 km
		println(new GeographicDistanceMetric(threshold = 50.0, unit = "km").evaluate("POINT(-0.124722 51.5081)","POINT(13.3989 52.5006)", 0.9))

	}
}