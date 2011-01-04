package de.fuberlin.wiwiss.silk.util

class Uri(val uri : String, val qualifiedName : Option[String])
{
  def toTurtle = qualifiedName match
  {
    case Some(qualifiedName) => qualifiedName
    case None => "<" + uri + ">"
  }

  override def toString = toTurtle
}

object Uri
{
  def parse(str : String, prefixes : Map[String, String]) =
  {
    if(str.startsWith("<"))
    {
      fromURI(str.substring(1, str.length - 1), prefixes)
    }
    else
    {
      fromQualifiedName(str, prefixes)
    }
  }

  def fromURI(uri : String, prefixes : Map[String, String]) : Uri =
  {
    for((prefix, suffix) <- prefixes if uri.startsWith(suffix))
    {
      return new Uri(uri, Some(prefix + ":" + uri.substring(suffix.length)))
    }
    new Uri(uri, None)
  }

  def fromQualifiedName(qualifiedName : String, prefixes : Map[String, String]) =
  {
    new Uri(resolvePrefix(qualifiedName, prefixes), Some(qualifiedName))
  }

  private def resolvePrefix(qualifiedName : String, prefixes : Map[String, String]) = qualifiedName.split(":", 2) match
  {
    case Array(prefix, suffix) => prefixes.get(prefix) match
    {
      case Some(resolvedPrefix) => resolvedPrefix + suffix
      case None => throw new IllegalArgumentException("Unknown prefix: " + prefix)
    }
    case _ => throw new IllegalArgumentException("No prefix found in " + qualifiedName)
  }
}
