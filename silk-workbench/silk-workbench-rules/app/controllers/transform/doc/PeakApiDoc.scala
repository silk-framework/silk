package controllers.transform.doc

object PeakApiDoc {

  final val peakResultDoc =
    """The result JSON consists of the actual values, i.e. all source values and all transformed values for each
      example entity, the source paths of the mapping and a status object. There are as many string arrays in the
      sourceValues array as there are input paths. Besides 'success' there are 2 other status ids, first
      there is 'empty' and second 'empty with exceptions'. In both cases the status message gives more details.
      The response also includes pagination metadata ('nextOffset', 'total', 'totalIsExact') when the caller
      requests it by passing 'includeTotal=true', 'offset' > 0, or a 'search' filter. In that case scanning
      drains the full 'offset + maxTryEntities' budget so 'total' reflects the count of successful results
      within that budget, 'totalIsExact' indicates whether the budget was sufficient to count every result,
      and 'nextOffset' can be passed back as the 'offset' query parameter to retrieve the next page. With
      the default settings (no 'offset', no 'search', 'includeTotal=false') the scan stops once 'limit'
      examples are collected and these fields are omitted.
      An optional 'search' query parameter performs a case-insensitive substring filter across both source values
      and transformed values; only matching rows count toward offset/limit/total.
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
      },
      "nextOffset": 3,
      "total": 12,
      "totalIsExact": true
    }
    """

}
