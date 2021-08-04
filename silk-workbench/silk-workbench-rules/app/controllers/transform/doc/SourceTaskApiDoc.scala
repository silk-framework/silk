package controllers.transform.doc

object SourceTaskApiDoc {

  final val valueSourcePathsExample =
    """
      [
        "ID",
        "Properties/Property",
        "Name",
        "Events/@count",
        "Events/Birth",
        "Events/Death",
        "Properties/Property/Key",
        "Properties/Property/Value"
      ]
    """

  final val valueSourcePathInfoExample =
    """
      [
        {
            "alreadyMapped": true,
            "path": "ID",
            "pathType": "value"
        },
        {
            "alreadyMapped": false,
            "path": "Name",
            "pathType": "value"
        },
        {
            "alreadyMapped": true,
            "objectInfo": {
                "dataTypeSubPaths": [
                    "@count",
                    "Birth",
                    "Death"
                ],
                "objectSubPaths": []
            },
            "path": "Events",
            "pathType": "object"
        },
        {
            "alreadyMapped": false,
            "path": "Events/@count",
            "pathType": "value"
        }
      ]
    """
}
