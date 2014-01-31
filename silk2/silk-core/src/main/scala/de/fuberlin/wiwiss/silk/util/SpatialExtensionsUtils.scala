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
	  
	  var (geometry, fromSRID) = Parser.separeteGeomeryFromSRID(literal)
	  println(geometry, fromSRID)
	  
	}  

	
	
	
	
	
	
	
	
	

	/**
  	 * A parser that separates the geometry literal from the SRID.
  	 */	
	private object Parser{

	  	/**
	  	 * Reader for WKT
	  	 */
		private val wktr = new WKTReader

	  	/**
	  	 * Reader for GML
	  	 */
		private val gmlr = new GMLReader
  
		def separeteGeomeryFromSRID(literal : String) : (String, Int) = {
			val trimmedLiteral=literal.trim
			
			var geometry : String = ""
			var srid : Int = -1
			
			// TODO: error handling
			
			if(trimmedLiteral.length() != 0)
			{
				val index = trimmedLiteral.lastIndexOf(Constants.STRDF_SRID_DELIM)
			
				if(index > 0) {	// strdf:WKT with SRID
					geometry = trimmedLiteral.substring(0, index)		
					srid = augmentString(trimmedLiteral.substring((trimmedLiteral.lastIndexOf("/")+1))).toInt
				}
				else{
					if (trimmedLiteral.startsWith("<")){ // starts with a URI => assume EPSG URI, geo:wktLiteral
						val index = trimmedLiteral.indexOf(">")
						if (index > 0) {
							geometry = trimmedLiteral.substring(index+1).trim
							srid = augmentString(trimmedLiteral.substring(trimmedLiteral.indexOf("/")+1, index-1)).toInt
						}
					}
					else{ // cannot guess, only WKT representation was given => assume strdf:WKT
						geometry = trimmedLiteral
						srid = Constants.DEFAULT_SRID
					}
				}
			}
			else{ // empty geometry => assume strdf:WKT
				geometry = Constants.EMPTY_GEOM
				srid  = Constants.DEFAULT_SRID
			}

			(geometry, srid)
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
		 * Default SRID
		 */
		val DEFAULT_SRID 	= WGS84_LAT_LON_SRID;
		
		/**
		 * stRDF SRID delimiter.
		 */		  
		val STRDF_SRID_DELIM 	= ";"  
	}	
}