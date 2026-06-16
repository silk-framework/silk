package org.silkframework.runtime.users

/**
  * User-safe authentication diagnostics that may be attached to execution reports.
  */
trait AuthDiagnostics {
  def jsonString: String
}

/**
  * Implemented by users that can expose sanitized authentication diagnostics.
  */
trait AuthDiagnosticsProvider {
  def authDiagnostics: Option[AuthDiagnostics]
}
