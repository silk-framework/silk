package org.silkframework.util

/**
  * Defines HTTP status codes.
  */
trait StatusCodeTestTrait {
  // Success codes
  final val OK = 200
  final val CREATED: Int = 201
  final val NO_CONTENT = 204

  // Request error
  final val BAD_REQUEST: Int = 400
  final val UNAUTHORIZED = 401
  final val FORBIDDEN = 403
  final val NOT_FOUND: Int = 404
  final val NOT_ACCEPTABLE: Int = 406
  final val CONFLICT: Int = 409

  // Server error
  final val INTERNAL_ERROR: Int = 500
  final val SERVICE_UNAVAILABLE: Int = 503
}
