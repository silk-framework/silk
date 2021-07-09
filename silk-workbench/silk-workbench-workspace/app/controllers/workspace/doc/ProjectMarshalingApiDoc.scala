package controllers.workspace.doc

object ProjectMarshalingApiDoc {

  final val marshallingPluginsExample =
    """
      [
        {
          "id": "rdfTurtle",
          "label": "RDF Turtle",
          "description": "RDF Turtle meta data without resource files.",
          "fileExtension": "ttl",
          "mediaType": "text/turtle"
        },
        {
          "id": "xmlZip",
          "label": "XML/ZIP file",
          "description": "ZIP archive, which includes XML meta data and resource files.",
          "fileExtension": "zip",
          "mediaType": "application/zip"
        }
      ]
    """

}
