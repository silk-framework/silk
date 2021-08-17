package org.silkframework.rule.util

import org.silkframework.config.Prefixes
import org.silkframework.entity.paths.{PathPositionStatus, UntypedPath}
import org.silkframework.rule.util.UriPatternParser.{ConstantPart, PathPart, UriPatternSegment}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

import java.net.{URI, URISyntaxException}
import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

/** Functions to parser URI patterns, i.e. URI templates. */
object UriPatternParser {
  // The position of a segment in the original URI pattern string. For path parts this is the range of the actual path expression without the curly braces.
  case class SegmentPosition(originalStartIndex: Int, originalEndIndex: Int)
  sealed trait UriPatternSegment {
    def segmentPosition: SegmentPosition
  }
  case class PathPart(serializedPath: String, segmentPosition: SegmentPosition) extends UriPatternSegment
  case class ConstantPart(value: String, segmentPosition: SegmentPosition) extends UriPatternSegment

  /** Exception that is thrown when the URI template pattern could not be parsed.
    *
    * @param msg                    Human-readable error message.
    * @param errorRange             The position (range) where the error happened.
    * @param insidePathExpression   If the error happened while in a path expression.
    * @param parsedStringUntilError The last part that was successfully parsed.
    */
  case class UriPatternParserException(msg: String,
                                       errorRange: (Int, Int),
                                       insidePathExpression: Boolean,
                                       parsedStringUntilError: String) extends RuntimeException(msg)

  /** Segments an URI pattern into constant and path parts. Does not validate the path expressions. */
  //noinspection ScalaStyle
  def parseIntoSegments(uriPattern: String): UriPatternSegments = {
    // The position status of the currently parsed path expression
    var pathPositionStatus: Option[PathPositionStatus] = None
    // Are we inside a curly brackets, i.e. in a path expression
    def insidePathExpression: Boolean = pathPositionStatus.isDefined
    // The segments of the URI pattern
    val pathSegments = new ArrayBuffer[UriPatternSegment]
    // The current string of the next path segment
    var currentValue = new StringBuffer()
    // Keep track of the current index
    var index = 0
    def indexRange: (Int, Int) = (index, index + 1)

    def addConstantPart(): Unit = {
      if (currentValue.length() > 0) {
        pathSegments.append(ConstantPart(currentValue.toString, SegmentPosition(index - currentValue.length(), index)))
        currentValue = new StringBuffer()
      }
    }
    def handleStartPathExpression(): Unit = {
      addConstantPart()
      pathPositionStatus = Some(PathPositionStatus())
    }

    def handleEndPathExpression(): Unit = {
      pathSegments.append(PathPart(currentValue.toString, SegmentPosition(index - currentValue.length(), index)))
      currentValue = new StringBuffer()
      pathPositionStatus = None
    }

    uriPattern foreach { char =>
      char match {
        case '{' if insidePathExpression && !pathPositionStatus.get.insideQuotes =>
          throw UriPatternParserException("Illegal character '{' found inside of path expression of URI template.", indexRange, insidePathExpression, currentValue.toString)
        case '}' if insidePathExpression && !pathPositionStatus.get.insideQuotes =>
          handleEndPathExpression()
        case '{' if !insidePathExpression =>
          handleStartPathExpression()
        case '}' if !insidePathExpression =>
          throw UriPatternParserException("Illegal character '}' found inside of constant part of URI template.", indexRange, insidePathExpression, currentValue.toString)
        case char: Char if insidePathExpression =>
          currentValue.append(char)
          pathPositionStatus.get.update(char)
        case char: Char =>
          currentValue.append(char)
      }
      index += 1
    }
    if(insidePathExpression) {
      throw UriPatternParserException("URI template ends unexpectedly inside a path expression.", (index-1, index), insidePathExpression, currentValue.toString)
    }
    addConstantPart()
    UriPatternSegments(pathSegments.toArray.toIndexedSeq)
  }
}

case class UriPatternSegments(segments: IndexedSeq[UriPatternSegment]) {
  private object ValidationValues {
    val pathValue = "pathValue"
    val entityURI = "htp://t.e.s.t.test/entity/%20"
  }

  /** Throws an validation exception if a validation error has been found. */
  def validateAndThrow()
                      (implicit prefixes: Prefixes): Unit = {
    val result = validationResult()
    result.validationError.foreach { error =>
      throw new ValidationException(s"Invalid URI template found at position (${error.errorRange._1}, ${error.errorRange._2}). Details: " + error.msg)
    }
  }
  /** Validates if the URI pattern will result in a valid URI and if the path expressions can be parsed. */
  def validationResult()
                      (implicit prefixes: Prefixes): UriPatternValidationResult = {
    // 1. Find out if a part of the URI pattern is invalid
    segments.zipWithIndex.foreach{
      case (ConstantPart(constant, position), 0) =>
        // First constant does not only need to be a valid URI part, but must define the scheme
        if(Try(new URI(constant + "schemeSpecificPart")).isFailure || new URI(constant + "schemeSpecificPart").getScheme == null) {
          return UriPatternValidationResult(Some(UriPatternValidationError(s"Start of URI pattern '$constant' does not start like a valid absolute URI, e.g. contains illegal characters or does not start with a scheme etc.", (position.originalStartIndex, position.originalEndIndex))))
        }
      case (ConstantPart(constant, position), _) =>
        // Later constants must be valid URI substrings
        if(Try(new URI("http://" + constant)).isFailure) {
          return UriPatternValidationResult(Some(UriPatternValidationError(s"Constant part '$constant' of URI template is not valid in a URI.", (position.originalStartIndex, position.originalEndIndex))))
        }
      case (PathPart(serializedPath, position), _) =>
        // For paths we only need to check valid path syntax, since a path value is always converted to a valid part of the URI
        UntypedPath.partialParse(serializedPath).error.foreach { error =>
          val globalOffset = position.originalStartIndex + error.offset
          return UriPatternValidationResult(Some(UriPatternValidationError(s"Invalid path expression '$serializedPath' inside URI template (at character $globalOffset). Details: ${error.message}", (globalOffset, globalOffset + 1))))
        }
    }
    // 2. Check if a valid URI is generated with test values
    val uri = testURI
    if(Uri(uri).isValidUri) {
      UriPatternValidationResult(None)
    } else {
      Try(new URI(uri)) match {
        case Success(_) =>
          UriPatternValidationResult(Some(UriPatternValidationError("URI template does not generate an absolute URI.", (0, 0))))
        case Failure(_: URISyntaxException) =>
          UriPatternValidationResult(Some(UriPatternValidationError("URI template does not generate a valid, absolute URI.", (0, 0))))
        case Failure(ex) =>
          throw ex
      }
    }
  }

  // Generates an example URI from the pattern that can be checked for validity
  private def testURI: String = {
    val strParts = segments.zipWithIndex map { case (segment, idx) =>
      segment match {
        case ConstantPart(value, _) =>
          value
        case PathPart(_, _) if idx == 0 =>
          ValidationValues.entityURI
        case PathPart(_, _) =>
          ValidationValues.pathValue
      }
    }
    strParts.mkString
  }
}

case class UriPatternValidationError(msg: String, errorRange: (Int, Int))
case class UriPatternValidationResult(validationError: Option[UriPatternValidationError]) {
  def success: Boolean = validationError.isEmpty
}