package controllers.transform.doc

object TargetVocabularyApiDoc {

  final val typeInfoExample =
    """
      {
        "genericInfo" : {
          "uri" : "foaf:Person",
          "label" : "Person",
          "description" : "A person."
        },
        "parentClasses" : [ "http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing", "foaf:Agent" ]
      }
    """

  final val propertyInfoExample =
    """
      {
        "genericInfo" : {
          "uri" : "foaf:name",
          "label" : "name",
          "description" : "A name for some thing."
        },
        "domain" : "owl:Thing",
        "range" : "rdfs:Literal"
      }
    """

  final val propertiesByClassExample =
    """
      [
        {
          "domain": "https://vocab.eccenca.com/testTarget/Loan",
          "genericInfo": {
            "URI": "https://vocab.eccenca.com/testTarget/zipCode",
            "label": "zip code"
          }
        },
        {
          "domain": "https://vocab.eccenca.com/testTarget/Loan",
          "genericInfo": {
            "URI": "https://vocab.eccenca.com/testTarget/volume",
            "label": "volume"
          }
        }
      ]
    """

  final val relationsOfClassExample =
    """
      {
        "backwardRelations": [
          {
            "property": {
              "domain": "https://vocab.eccenca.com/testTarget/Person",
              "genericInfo": {
                "URI": "https://vocab.eccenca.com/testTarget/hasLoan",
                "label": "hasLoan"
              },
              "range": "https://vocab.eccenca.com/testTarget/Loan"
            },
            "targetClass": {
              "genericInfo": {
                "URI": "https://vocab.eccenca.com/testTarget/Loan",
                "description": "Loans of customers",
                "label": "Loan"
              },
              "parentClasses": []
            }
          }
        ],
        "forwardRelations": [
          {
            "property": {
              "domain": "https://vocab.eccenca.com/testTarget/Loan",
              "genericInfo": {
                "URI": "https://vocab.eccenca.com/testTarget/lendTo",
                "label": "lend to"
              },
              "range": "https://vocab.eccenca.com/testTarget/Person"
            },
            "targetClass": {
              "genericInfo": {
                "URI": "https://vocab.eccenca.com/testTarget/Loan",
                "description": "Loans of customers",
                "label": "Loan"
              },
              "parentClasses": []
            }
          }
        ]
      }
    """

  final val targetVocabularyExample =
    """
      {
        "vocabularies": [
          {
            "nrClasses": 0,
            "nrProperties": 61,
            "uri": "urn:foaf.owl"
          }
        ]
      }
    """

}
