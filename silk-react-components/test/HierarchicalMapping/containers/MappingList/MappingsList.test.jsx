import React from "react";
import { shallow, mount } from 'enzyme';
import {
    ContextMenu,
    BreadcrumbList,
    BreadcrumbItem,
    MenuItem,
} from '@eccenca/gui-elements';

import MappingsList from '../../../../src/HierarchicalMapping/containers/MappingsList/MappingsList';
import EmptyList from '../../../../src/HierarchicalMapping/containers/MappingsList/EmptyList';
import { DragDropContext } from 'react-beautiful-dnd';
import DraggableItem from '../../../../src/HierarchicalMapping/containers/MappingRule/DraggableItem';
import ListActions from '../../../../src/HierarchicalMapping/containers/MappingsList/ListActions';

const onClickedRemoveFn = jest.fn();

const props = {
    rules: [
        {
            "type":"complex",
            "id":"country",
            "operator":{
                "type":"transformInput",
                "id":"normalize",
                "function":"GeoLocationParser",
                "inputs":[
                    {
                        "type":"pathInput",
                        "id":"country",
                        "path":"country"
                    }
                ],
                "parameters":{
                    "parseTypeId":"Country",
                    "fullStateName":"true"
                }
            },
            "sourcePaths":[
                "country"
            ],
            "metadata":{
                "description":"sss",
                "label":""
            },
            "mappingTarget":{
                "uri":"<urn:ruleProperty:country>",
                "valueType":{
                    "nodeType":"UriValueType"
                },
                "isBackwardProperty":false,
                "isAttribute":false
            }
        },{
            "type":"complex",
            "id":"1",
            "operator":{
                "type":"transformInput",
                "id":"buildUri",
                "function":"concat",
                "inputs":[
                    {
                        "type":"transformInput",
                        "id":"constant0",
                        "function":"constant",
                        "inputs":[
                        ],
                        "parameters":{
                            "value":""
                        }
                    },
                    {
                        "type":"transformInput",
                        "id":"fixUri1",
                        "function":"uriFix",
                        "inputs":[
                            {
                                "type":"pathInput",
                                "id":"path1",
                                "path":"city"
                            }
                        ],
                        "parameters":{
                            "uriPrefix":"urn:url-encoded-value:"
                        }
                    },
                    {
                        "type":"transformInput",
                        "id":"constant2",
                        "function":"constant",
                        "inputs":[
                        ],
                        "parameters":{
                            "value":"/1"
                        }
                    }
                ],
                "parameters":{
                    "glue":"",
                    "missingValuesAsEmptyStrings":"false"
                }
            },
            "sourcePaths":[
                "city"
            ],
            "metadata":{
                "description":"33",
                "label":"2"
            },
            "mappingTarget":{
                "uri":"1",
                "valueType":{
                    "nodeType":"UriValueType"
                },
                "isBackwardProperty":false,
                "isAttribute":false
            }
        }],
    parentRuleId: 'root',
};

const getWrapper = (renderer = shallow, args = props) => renderer(
    <MappingsList {...args} />
);

describe("MappingsList Component", () => {
    describe("on component mounted, ",() => {
        let wrapper;
        beforeEach(() => {
            wrapper = getWrapper(shallow);
        });
        
        it('should render EmptyList component, when rules array is empty', () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                rules: []
            });
            expect(wrapper.find(EmptyList)).toHaveLength(1);
        });
        
        it('should render DragDropContext component, when rules is NOT empty', () => {
            expect(wrapper.find(DragDropContext)).toHaveLength(1);
        });
    
        it('should render DraggableItem component, the right count', () => {
            const wrapper = getWrapper(mount);
            expect(wrapper.find(DraggableItem)).toHaveLength(2);
        });
        
        it('should render ListActions component', () => {
           expect(wrapper.find(ListActions)).toHaveLength(1);
        });
        
        afterEach(() => {
            wrapper.unmount();
        })
    });
    
    describe('on component receive updates', () => {
        let wrapper;
        beforeEach(() => {
            wrapper = getWrapper(shallow);
        });
        
        it('should update the rules, when new data is passed', () => {
            const wrapper = getWrapper(mount);
            wrapper.setProps({
                ...props,
                rules: [
                    {
                        "type":"complex",
                        "id":"country",
                        "operator":{
                            "type":"transformInput",
                            "id":"normalize",
                            "function":"GeoLocationParser",
                            "inputs":[
                                {
                                    "type":"pathInput",
                                    "id":"country",
                                    "path":"country"
                                }
                            ],
                            "parameters":{
                                "parseTypeId":"Country",
                                "fullStateName":"true"
                            }
                        },
                        "sourcePaths":[
                            "country"
                        ],
                        "metadata":{
                            "description":"sss",
                            "label":""
                        },
                        "mappingTarget":{
                            "uri":"<urn:ruleProperty:country>",
                            "valueType":{
                                "nodeType":"UriValueType"
                            },
                            "isBackwardProperty":false,
                            "isAttribute":false
                        }
                    }
                ]
            });
            expect(wrapper.find(DraggableItem)).toHaveLength(1);
        });
        
        afterEach(() => {
            wrapper.unmount();
        })
    })
});
