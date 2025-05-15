package controllers.transform.doc

object PeakApiDoc {

  final val peakResultDoc =
    """The result JSON consists of the actual values, i.e. all source values and all transformed values for each
      example entity, the source paths of the mapping and a status object. There are as many string arrays in the
      sourceValues array as there are input paths. Besides 'success' there are 2 other status ids, first
      there is 'empty' and second 'empty with exceptions'. In both cases the status message gives more details.
    """

  final val peakExample =
    """
    {
      "results": [
        {
          "sourceValues": [
            [
              "Olaf",
              "Ralf"
            ],
            [
              "Müller",
              "Schmidt"
            ]
          ],
          "transformedValues": [
            " Olaf  M%C3%BCller",
            " Olaf  Schmidt",
            " Ralf  M%C3%BCller",
            " Ralf  Schmidt"
          ]
        }
      ],
      "sourcePaths": [
        [
          "/<http://firstName>"
        ],
        [
          "/<http://lastName>"
        ]
      ],
      "status": {
        "id": "success",
        "msg": ""
      }
    }
    """

}
