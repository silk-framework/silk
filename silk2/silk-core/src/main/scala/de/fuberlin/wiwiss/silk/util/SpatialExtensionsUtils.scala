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

package de.fuberlin.wiwiss.silk.util

import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.io.ParseException
import com.vividsolutions.jts.io.WKTReader
import com.vividsolutions.jts.io.gml2.GMLReader


import java.util.logging.Logger

/**
 * Useful utils for the spatial extensions of Silk.
 * @author Panayiotis Smeros (Department of Informatics & Telecommunications, National & Kapodistrian University of Athens)
 */


object SpatialExtensionsUtils {
	



	
	
	def convertToDefaultSRID(literal : String){
	  
	  var index = Parser.separeteGeomeryFromSRID(literal)
	  index
	}  

	
	
	
	
	
	
	
	
	

	/**
  	 * A parser that separates the geometry literal from the SRID.
  	 */	
	private object Parser{

	  	/**
	  	 * Reader for WKT
	  	 */
		val wktr = new WKTReader

	  	/**
	  	 * Reader for GML
	  	 */
		val gmlr = new GMLReader
  
		def separeteGeomeryFromSRID(literal : String){
		var index = literal.lastIndexOf(Constants.STRDF_SRID_DELIM)
	
		index
		  
		}		
	}
	
  	/**
  	 * An object that contains all needed constants (namespaces, URIs, prefixes).
  	 */	
	private object Constants{
    
		/**
		 * The namespace for stRDF data model.
		 */
		val stRDF = "http://strdf.di.uoa.gr/ontology#"
	
		/**
		 * The namespace for GeoSPARQL ontology.
		 */
		val GEO	= "http://www.opengis.net/ont/geosparql#"
	
		/**
		 * The namespace for GML.
		 */
		val GML_OGC	= "http://www.opengis.net/gml"

		/**
		 * The URI for the datatype Well-Known Text (WKT).
		 */
		val WKT	= stRDF + "WKT"
	
		/**
		 * The URI for the datatype Geography Markup Language (GML).
		 */
		val GML	= stRDF + "GML"
		
		/**
		 * The URI for the datatype wktLiteral.
		 */
		val WKTLITERAL =  GEO + "wktLiteral"
		
		/**
		 * The URI for the datatype gmlLiteral.
		 */
		val GMLLITERAL = GEO + "gmlLiteral"

		/**
		 * WKT representation for an empty geometry.
		 */
		val EMPTY_GEOM = "MULTIPOLYGON EMPTY"

		/**
		 * EPSG:4326 (default for stSPARQL).
		 */
		val WGS84_LAT_LON_SRID = 4326
		
		/**
		 * EPSG:3857 (default for GeoSPARQL).
		 */
		val WGS84_LON_LAT_SRID = 3857

		/**
		 * EPSG SRID prefix.
		 */
		val EPSG_SRID_PREFIX = "http://www.opengis.net/def/crs/EPSG/0/"

		/**
		 * stRDF SRID delimiter.
		 */		  
		val STRDF_SRID_DELIM 	= ";"	
	}	
}