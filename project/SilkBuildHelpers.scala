/**
  * Helper methods for the general Silk build.
  */
object SilkBuildHelpers {
  def isSnapshotVersion(silkVersion: String): Boolean = {
    silkVersion.reverse.takeWhile(c => c != '-' && c != '.').length >= 5
  }
}
