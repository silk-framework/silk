package de.fuberlin.wiwiss.silk.util

import de.fuberlin.wiwiss.silk.config.ValidationException
import de.fuberlin.wiwiss.silk.util.StringUtils._

/**
 * A strategy which can have different implementations.
 */
trait Strategy
{
    val params : Map[String, String]

    protected def readOptionalParam(name : String) =
    {
        params.get(name)
    }

    protected def readRequiredParam(name : String) =
    {
        readOptionalParam(name) match
        {
            case Some(value) => value
            case None => throw new ValidationException("Parameter " + name + " is required")
        }
    }

    protected def readOptionalIntParam(name : String) =
    {
        readOptionalParam(name) match
        {
            case Some(IntLiteral(value)) => Some(value)
            case Some(_) => throw new ValidationException("Parameter " + name + " must be an integer")
            case None => None
        }
    }

    protected def readRequiredIntParam(name : String) =
    {
        readRequiredParam(name) match
        {
            case IntLiteral(value) => value
            case _ => throw new ValidationException("Parameter " + name + " must be an integer")
        }
    }

    protected def readOptionalDoubleParam(name : String) =
    {
        readOptionalParam(name) match
        {
            case Some(DoubleLiteral(value)) => Some(value)
            case Some(_) => throw new ValidationException("Parameter " + name + " must be a number")
            case None => None
        }
    }

    protected def readRequiredDoubleParam(name : String) =
    {
        readRequiredParam(name) match
        {
            case DoubleLiteral(value) => value
            case _ => throw new ValidationException("Parameter " + name + " must be a number")
        }
    }
}
