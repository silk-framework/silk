import React from "react";
import { mount, shallow } from 'enzyme';
import { NotAvailable } from '@eccenca/gui-elements';

import RootMappingRule from '../../../src/HierarchicalMapping/containers/RootMappingRule';
import ObjectRule from '../../../src/HierarchicalMapping/containers/MappingRule/ObjectRule/ObjectRule';


const props = {
    rule: {
        "type": "complex",
        "id": "country",
        "operator": {
            "type": "transformInput",
            "id": "normalize",
            "function": "GeoLocationParser",
            "inputs": [
                {
                    "type": "pathInput",
                    "id": "country",
                    "path": "country"
                }
            ],
            "parameters": {
                "parseTypeId": "Country",
                "fullStateName": "true"
            }
        },
        "sourcePaths": [
            "country"
        ],
        "metadata": {
            "description": "sss",
            "label": ""
        },
        "mappingTarget": {
            "uri": "<urn:ruleProperty:country>",
            "valueType": {
                "nodeType": "UriValueType"
            },
            "isBackwardProperty": false,
            "isAttribute": false
        },
        rules: {
            uriRule: {
                pattern: 'pattern'
            }
        }
    },
    parentRuleId: 'root',
};

const selectors = {
    URI_PATTERN: '.ecc-silk-mapping__rulesobject__title-uripattern'
};

const getWrapper = (renderer = shallow, args = props) => renderer(
    <RootMappingRule {...args} />
);

describe("RootMappingRule Component", () => {
    describe("on component mounted, ", () => {
        let wrapper;
        beforeEach(() => {
            wrapper = getWrapper(shallow);
        });
        
        it('should return null, when rule is empty', () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                rule: {}
            });
            expect(wrapper.get(0)).toBeFalsy();
        });
        
        it('should NotAvailable rendered in uriPattern, when rule type is not Uri or Complex', () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                rule: {
                    ...props.rule,
                    type: 'root'
                }
            });
            expect(wrapper.find(NotAvailable)).toHaveLength(1);
        });
        
        it('should uriPattern is equal to `rules.uriRule.pattern`, when uriRule type is uri', () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                rule: {
                    ...props.rule,
                    rules: {
                        ...props.rule.rules,
                        uriRule: {
                            type: 'uri',
                            pattern: 'pattern'
                        }
                    }
                }
            });
            expect(wrapper.find(selectors.URI_PATTERN).text()).toBe('pattern');
        });
    
        it('should uriPattern is equal to `URI formula`, when uriRule type is complex', () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                rule: {
                    ...props.rule,
                    rules: {
                        ...props.rule.rules,
                        uriRule: {
                            type: 'complexUri',
                        }
                    }
                }
            });
            expect(wrapper.find(selectors.URI_PATTERN).text()).toBe('URI formula');
        });
        
        it('should render the ObjectRule component, when rule is expanded', () => {
            wrapper.setState({
                expanded: true
            });
            expect(wrapper.find(ObjectRule)).toHaveLength(1);
        });
        
        afterEach(() => {
            wrapper.unmount();
        })
    })
});
