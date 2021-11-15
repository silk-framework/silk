package controllers.workspaceApi.doc

object WorkspaceUriPatternApiDoc {

  final val uriPatternsRequest =
"""
{
  "projectId": "someProject",
  "targetClassUris": [
    "urn:class:A",
    "urn:class:B"
  ]
}
"""
  final val uriPatternsExample =
"""
{
  "results": [
    {
      "targetClassUri": :urn:class:A",
      "label": "https://constant/{...localPart}",
      "value": "https://constant/{<urn:path:localPart>}"
    },
    {
      "targetClassUri": :urn:class:B",
      "label": "https://constant/{...variable}/constantPath",
      "value": "https://constant/{/some/long/path/that/is/variable}/constantPath"
    }
  ]
}
"""
}
