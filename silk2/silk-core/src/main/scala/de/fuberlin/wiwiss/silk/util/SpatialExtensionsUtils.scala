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
 * Useful Utils
 * @author Panayiotis Smeros (Department of Informatics & Telecommunications, National & Kapodistrian University of Athens)
 */


object SpatialExtensionsUtils {
	

 
	val strdf =	  Constants.stRDF

	
	
	def convertToDefaultSRID(){}  

	
	
	
	
	
	
	
	
	
	
	
	private object Constants{
    
		/**
		 * The namespace for stRDF data model
		 */
		val stRDF					= "http://strdf.di.uoa.gr/ontology#"
	
		/**
		 * The namespace for GeoSPARQL ontology
		 */
		val GEO						= "http://www.opengis.net/ont/geosparql#";
	
		/**
		 * The namespace of GML
		 */
		val GML_OGC					= "http://www.opengis.net/gml";  

		/**
		 * The URI for the datatype Well-Known Text (WKT)
		 */
		val WKT 					= stRDF + "WKT";
	
		/**
		 * The URI for the datatype Geography Markup Language (GML)
		 */
		val GML						= stRDF + "GML";
		
		/**
		 * The URI for the datatype wktLiteral
		 */
		val WKTLITERAL				=  GEO + "wktLiteral";
		
		/**
		 * The URI for the datatype gmlLiteral
		 */
		val GMLLITERAL				=  GEO + "gmlLiteral";			
	}
  
	private object Parser{

		val wktr = new WKTReader
		val gmlr = new GMLReader
  
		def getGeometryfromLiteral(){
		  
		  
		}

	}
}