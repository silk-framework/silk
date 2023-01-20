package controllers.linking.doc

object LinkingTaskApiDoc {

  final val referenceLinksExample =
    """
      <rdf:RDF xmlns:align="http://knowledgeweb.semanticweb.org/heterogeneity/alignment#" xmlns:xsd="http://www.w3.org/2001/XMLSchema#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns="http://knowledgeweb.semanticweb.org/heterogeneity/alignment#">
        <Alignment>
          <map>
             <Cell>
               <entity1 rdf:resource="http://dbpedia.org/resource/The_Phantom_of_the_Opera_%281925_film%29"/>
               <entity2 rdf:resource="http://data.linkedmdb.org/resource/film/732"/>
               <relation>=</relation>
               <measure rdf:datatype="http://www.w3.org/2001/XMLSchema#float">0.605</measure>
             </Cell>
           </map>
         </Alignment>
       </rdf:RDF>
    """

  final val referenceLinksEvaluatedExample =
    """
      {
        "positive": [
          {
            "source": "http://dbpedia.org/resource/The_River_%281938_film%29",
            "target": "http://data.linkedmdb.org/resource/film/208",
            "confidence": 1,
            "ruleValues": {
              "operatorId": "compareTitles",
              "score": 1,
              "sourceValue": {
                "operatorId": "movieTitle1",
                "values": [
                  "The River"
                ],
                "error": null
              },
              "targetValue": {
                "operatorId": "movieTitle2",
                "values": [
                  "The River"
                ],
                "error": null
              }
            }
          }
        ],
        "negative": [
        ],
        "evaluationScore": {
          "fMeasure": "0.33",
          "precision": "0.88",
          "recall": "0.20",
          "falseNegatives": 88,
          "falsePositives": 3,
          "trueNegatives": 98,
          "truePositives": 22
        }
      }
    """

  final val postLinkDatasourceRequestExample =
    """
      <Link>
        <!-- curl -i -H 'content-type: application/xml' -X POST http://localhost:9000/linking/tasks/SocialAPIMappings/linkPerson/postLinkDatasource -d @linkPersonRequest.xml -->
        <DataSources>
          <Dataset id="sourceDataset">
            <DatasetPlugin type="file">
              <Param name="file" value="source"/> <!-- references the first resource later in this request body -->
              <Param name="format" value="N-Triples"/>
            </DatasetPlugin>
          </Dataset>
          <Dataset id="targetDataset">
            <DatasetPlugin type="file">
              <Param name="file" value="target"/> <!-- references the second resource later in this request body -->
              <Param name="format" value="N-Triples"/>
            </DatasetPlugin>
          </Dataset>
        </DataSources>
        <resource name="source">
          &lt;https://www.example.com/resource/123456&gt; &lt;http://xmlns.com/foaf/0.1/name&gt; "John Doe" .
        </resource>
        <resource name="target">
          &lt;https://www.example2.com/resource/abcdef&gt; &lt;http://xmlns.com/foaf/0.1/name&gt; "Doe, John"
        </resource>
      </Link>
    """

  final val postLinkDatasourceResponseExample =
    """<http://uri1> <http://www.w3.org/2002/07/owl#sameAs> <http://uri2> ."""

  final val evaluateLinkageRuleRequestJsonExample =
    """
    {
      "filter": {
        "limit": null,
        "unambiguous": null
      },
      "linkType": "http://www.w3.org/2002/07/owl#sameAs",
      "operator": {
        "id": "unnamed_3",
        "indexing": true,
        "metric": "equality",
        "parameters": {},
        "required": false,
        "sourceInput": {
          "id": "unnamed_1",
          "path": "group",
          "type": "pathInput"
        },
        "targetInput": {
          "id": "unnamed_2",
          "path": "group",
          "type": "pathInput"
        },
        "threshold": 0,
        "type": "Comparison",
        "weight": 1
      }
    }
    """

  final val evaluateLinkageRuleRequestXmlExample =
    """
      <LinkageRule linkType="&lt;http://www.w3.org/2002/07/owl#sameAs&gt;">
        <Compare id="compareLabels" required="true" weight="1" metric="equality" threshold="0.0" indexing="true">
          <Input id="label1" path="label"/>
          <Input id="label2" path="label"/>
        </Compare>
      </LinkageRule>
    """

  final val evaluateLinkageRuleResponseExample =
    """
      [
        {
          "confidence": 1,
          "ruleValues": {
              "operatorId": "unnamed_6",
              "score": 1,
              "sourceValue": {
                "error": null,
                "operatorId": "unnamed_4",
                "values": [
                  "group 1"
                ]
              },
              "targetValue": {
                "error": null,
                "operatorId": "unnamed_5",
                "values": [
                  "group 1"
                ]
              }
          },
          "source": "urn:instance:simplecsv#1",
          "target": "urn:instance:simplecsv#1"
        }
      ]
    """

  final val evaluateCurrentLinkageRuleRequest =
    """
  {
    "filters" : [
      "positiveLinks"
    ],
    "limit" : 10,
    "offset" : 0,
    "query" : "multi word search query",
    "sortBy" : [
      "scoreAsc"
    ],
    "includeReferenceLinks": true
  }
    """

  final val evaluateCurrentLinkageRuleExample =
    """
{
    "linkRule": {
        "operator": {
            "id": "equality",
            "type": "Comparison",
            "weight": 1,
            "threshold": 0,
            "indexing": true,
            "metric": "equality",
            "parameters": {},
            "sourceInput": {
                "type": "pathInput",
                "id": "sourcePathInput_2",
                "path": "foaf:name"
            },
            "targetInput": {
                "type": "pathInput",
                "id": "targetPathInput",
                "path": "rdfs:label"
            }
        }
    },
    "stats": {
        "nrSourceEntities": 195,
        "nrTargetEntities": 174,
        "nrLinks": 124
    },
    "links": [
        {
            "source": "http://dbpedia.org/resource/The_Score_%28film%29",
            "target": "http://data.linkedmdb.org/resource/film/746",
            "confidence": 1,
            "ruleValues": {
                "operatorId": "equality",
                "score": 1,
                "sourceValue": {
                    "operatorId": "sourcePathInput_2",
                    "values": [
                        "The Score"
                    ],
                    "error": null
                },
                "targetValue": {
                    "operatorId": "targetPathInput",
                    "values": [
                        "The Score"
                    ],
                    "error": null
                }
            },
            "decision": "unlabeled"
        }
        ...
    ]
}
      """
}
