import chaiEnzyme from "chai-enzyme";
import chai from "chai";
import Enzyme from "enzyme/build/index";
import Adapter from "enzyme-adapter-react-15/build/index";
import nock from "nock";

nock('http://localhost:8080/transform/tasks/cmem/transform_datasetresource_Sacramentorealestatetransactions_csv/rules')
    .get('')
    .times(100)
    .reply(200, () => {
        return {

            "type":"root",
            "id":"root",
            "rules":{
                "uriRule":{
                    "type":"uri",
                    "id":"uri",
                    "pattern":"/",
                    "metadata":{
                        "label":"",
                        "description":""
                    }},
                "typeRules":[],
                "propertyRules":[
                    {"type":"direct",
                        "id":"direct",
                        "sourcePath":"city",
                        "mappingTarget":{
                            "uri":"city",
                            "valueType":{
                                "nodeType":"AutoDetectValueType"
                            },
                            "isBackwardProperty":false,
                            "isAttribute":false
                        },
                        "metadata":{
                            "label":"",
                            "description":""
                        }
                    },
                    {
                        "type":"direct",
                        "id":"direct1",
                        "sourcePath":"street",
                        "mappingTarget":{
                            "uri":"street",
                            "valueType":{
                                "nodeType":"AutoDetectValueType"
                            },
                            "isBackwardProperty":false,
                            "isAttribute":false
                        },"metadata":{
                            "label":"",
                            "description":""
                        }
                    }
                ]
            },
            "metadata":{
                "label":"",
                "description":""
            }
        };
    });

nock('http://docker.local/dataintegration/transform/tasks/cmem/transform_datasetresource_Sacramentorealestatetransactions_csv/targetVocabulary/typeOrProperty?uri=city')
    .get('')
    .times(100)
    .reply(404, e => { console.warn("failure"); return{} });

nock('http://docker.local/dataintegration/transform/tasks/cmem/transform_datasetresource_Sacramentorealestatetransactions_csv/targetVocabulary/typeOrProperty?uri=street')
    .get('')
    .times(100)
    .reply(404, e => { console.warn("failure"); return{} });

chai.use(chaiEnzyme());
Enzyme.configure({ adapter: new Adapter() });

