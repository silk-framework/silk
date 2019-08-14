import React from 'react';
import { expect } from 'chai';
import transformRuleOfObjectMapping from '../../../src/HierarchicalMapping/utils/transformRuleOfObjectMapping';

describe("utils", () => {
    describe("transformRuleOfObjectMapping", () => {
        it("should return empty object", () => {
            const result = transformRuleOfObjectMapping({});
            expect(result).to.deep.equal({});
        });
        it("should return formatted root object", () => {
            const input = {
                "type": "root",
                "id": "root",
                "rules": {
                    "uriRule": null,
                    "typeRules": [
                        {
                            "type": "type",
                            "id": "1499152451019_loans_workflowSource_test_csv",
                            "typeUri": "1499152451019_loans_workflowSource_test_csv",
                            "metadata": {
                                "label": "1499152451019 loans workflow Source test csv"
                            }
                        }
                    ],
                },
                "metadata": {
                    "label": "newlabel"
                }
            };
            const result = transformRuleOfObjectMapping(input);
            const expectedResult = {
                comment: '',
                label: 'newlabel',
                targetEntityType: [
                    '1499152451019_loans_workflowSource_test_csv'
                ],
                entityConnection: 'from',
                pattern: '',
                sourceProperty: undefined,
                targetProperty: undefined,
                type: 'root',
                uriRuleType: 'uri',
                uriRule: null,
            };
            expect(result).to.deep.equal(expectedResult);
        });
        it("should return formatted child object", () => {
            const input = {
                "type": "object",
                "id": "object2",
                "sourcePath": "amount",
                "mappingTarget": {
                    "uri": "<http://example.com/baz3>",
                    "valueType": {
                        "nodeType": "UriValueType"
                    },
                    "isBackwardProperty": false,
                    "isAttribute": false
                },
                "rules": {
                    "uriRule": {
                        "type": "uri",
                        "id": "uri1",
                        "pattern": "urn:foo",
                        "metadata": {
                            "label": "uri"
                        }
                    },
                    "typeRules": [
                        {
                            "type": "type",
                            "id": "targetEntity",
                            "typeUri": "<urn:targetEntity>",
                            "metadata": {
                                "label": "target Entity"
                            }
                        }
                    ],
                },
                "metadata": {
                    "description": "testDescription",
                    "label": "testLabel"
                },
            };
            const result = transformRuleOfObjectMapping(input);
            const expectedResult = {
                comment: 'testDescription',
                label: 'testLabel',
                targetEntityType: [
                    '<urn:targetEntity>'
                ],
                entityConnection: 'from',
                pattern: 'urn:foo',
                sourceProperty: 'amount',
                targetProperty: '<http://example.com/baz3>',
                type: 'object',
                uriRuleType: 'uri',
                uriRule: {
                    id: 'uri1',
                    metadata: {
                        label: 'uri'
                    },
                    pattern: 'urn:foo',
                    type: 'uri',
                }
            };
            expect(result).to.deep.equal(expectedResult);
        });
    });
});
