//package de.fuberlin.wiwiss.silk.util
//
//class URI(val uri : String, val qualifiedName : Option[String])
//{
////    def toTurtle = shortName match
////    {
////        case Some(shortName) => shortName
////        case None => "<" + uri + ">"
////    }
//}
//
//object URI
//{
//    def fromQualifiedName(qualifiedName : String, prefixes : Map[String, String]) =
//    {
//        new URI(resolvePrefix(qualifiedName, prefixes), Some(qualifiedName))
//    }
//
//    def fromURI(uri : String) = new URI(uri, None)
//
//    private def resolvePrefix(qualifiedName : String, prefixes : Map[String, String]) = qualifiedName.split(":", 2) match
//    {
//        case Array(prefix, suffix) => prefixes.get(prefix) match
//        {
//            case Some(resolvedPrefix) => resolvedPrefix + suffix
//            case None => throw new IllegalArgumentException("Unknown prefix: " + prefix)
//        }
//        case _ => throw new IllegalArgumentException("No prefix found in " + qualifiedName)
//    }
//}
