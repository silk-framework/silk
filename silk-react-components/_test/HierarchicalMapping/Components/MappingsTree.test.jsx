import React from 'react';
import { shallow } from "enzyme";
import { expect } from 'chai';
import sinon from "sinon";
import Enzyme from 'enzyme/build';
import Adapter from 'enzyme-adapter-react-15/build';
import MappingsTree from '../../../src/HierarchicalMapping/Components/MappingsTree';

Enzyme.configure({ adapter: new Adapter() });

const infoEmpty = 'Info[data-test-id="ecc-silk-mapping__treenav-norules"]';
const infoLoading = 'Info[data-test-id="ecc-silk-mapping__treenav-loading"]';
const rootTree = '.ecc-silk-mapping__treenav--maintree';
const rootElement = '.ecc-silk-mapping__treenav--maintree > li > div > .ecc-silk-mapping__treenav--item';
const childTree = '.ecc-silk-mapping__treenav--subtree';
const childElement = '.ecc-silk-mapping__treenav--subtree > li > div > .ecc-silk-mapping__treenav--item';
const childElementExpander = 'Button[data-test-id="ecc-silk-mapping__treenav--item-toggler-object1"]';

const tree = {
    "type": "root",
    "id": "root",
    "rules": {
        "uriRule": null,
        "typeRules": [
            {
                "type": "type",
                "id": "type",
                "typeUri": "1499152451019_loans_workflowSource_test_csv",
                "metadata": {
                    "label": ""
                }
            }
        ],
        "propertyRules": [
            {
                "type": "object",
                "id": "object1",
                "sourcePath": "loanId",
                "mappingTarget": {
                    "uri": "<http://example.com/bar2>",
                    "valueType": {
                        "nodeType": "UriValueType"
                    },
                    "isBackwardProperty": false,
                    "isAttribute": false
                },
                "rules": {
                    "uriRule": null,
                    "typeRules": [],
                    "propertyRules": [
                        {
                            "type": "object",
                            "id": "object4",
                            "sourcePath": "",
                            "mappingTarget": {
                                "uri": "<http://example.com/bar2/bar2>",
                                "valueType": {
                                    "nodeType": "UriValueType"
                                },
                                "isBackwardProperty": false,
                                "isAttribute": false
                            },
                            "rules": {
                                "uriRule": null,
                                "typeRules": [],
                                "propertyRules": [
                                    {
                                        "type": "object",
                                        "id": "object7",
                                        "sourcePath": "",
                                        "mappingTarget": {
                                            "uri": "<http://example.com/bar2/bar2/foo1>",
                                            "valueType": {
                                                "nodeType": "UriValueType"
                                            },
                                            "isBackwardProperty": false,
                                            "isAttribute": false
                                        },
                                        "rules": {
                                            "uriRule": null,
                                            "typeRules": [],
                                            "propertyRules": []
                                        },
                                        "metadata": {
                                            "label": ""
                                        }
                                    }
                                ]
                            },
                            "metadata": {
                                "label": ""
                            }
                        },
                        {
                            "type": "object",
                            "id": "object3",
                            "sourcePath": "",
                            "mappingTarget": {
                                "uri": "<http://example.com/bar2/foo1>",
                                "valueType": {
                                    "nodeType": "UriValueType"
                                },
                                "isBackwardProperty": false,
                                "isAttribute": false
                            },
                            "rules": {
                                "uriRule": null,
                                "typeRules": [],
                                "propertyRules": []
                            },
                            "metadata": {
                                "label": ""
                            }
                        }
                    ]
                },
                "metadata": {
                    "label": ""
                }
            },
        ]
    },
    "metadata": {
        "label": ""
    }
};

const props = {
    currentRuleId: undefined,
    navigationTree: tree,
    navigationExpanded: undefined,
    navigationLoading: false,
    handleRuleNavigation: () => {},
    handleToggleExpanded: () => {},
};

describe("HierarchicalMapping", () => {
    describe("MappingsTree", () => {
        it("should show empty message", () => {
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                    navigationTree={undefined}
                />
            );
            expect(wrapper.find(infoEmpty)).to.have.lengthOf(1);
        });
        it("should not show empty message because of loading", () => {
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                    navigationLoading={true}
                />
            );
            expect(wrapper.find(infoEmpty)).to.have.lengthOf(0);
        });
        it("should not show empty message because of navigation tree data ", () => {
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                />
            );
            expect(wrapper.find(infoEmpty)).to.have.lengthOf(0);
        });
        it("should show loading spinner", () => {
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                    navigationLoading={true}
                />
            );
            expect(wrapper.find('Spinner')).to.have.lengthOf(1);
        });
        it("should not show loading spinner", () => {
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                    navigationLoading={false}
                />
            );
            expect(wrapper.find('Spinner')).to.have.lengthOf(0);
        });

        it("should show loading message", () => {
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                    navigationLoading={true}
                    navigationTree={undefined}
                />
            );
            expect(wrapper.find(infoLoading)).to.have.lengthOf(1);
        });
        it("should not show loading message because of not loading", () => {
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                    navigationTree={undefined}
                />
            );
            expect(wrapper.find(infoLoading)).to.have.lengthOf(0);
        });
        it("should not show loading message because of navigation tree data", () => {
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                    navigationLoading={true}
                />
            );
            expect(wrapper.find(infoLoading)).to.have.lengthOf(0);
        });
        it("should not show navigation tree", () => {
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                    navigationTree={{}}
                />
            );
            expect(wrapper.find(rootTree)).to.have.lengthOf(0);
        });
        it("should show navigation tree", () => {
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                />
            );
            expect(wrapper.find(rootTree)).to.have.lengthOf(1);
        });
        it("should show highlighted root element", () => {
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                />
            );
            expect(wrapper.find(`${rootElement}.ecc-silk-mapping__treenav--item-active`)).to.have.lengthOf(1);
        });
        it("should show expand button on root element with icon 'arrow_nextpage'", () => {
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                />
            );
            expect(wrapper.find(`${rootElement} Button`).props().iconName).to.equal('arrow_nextpage');
        });
        it("should show expand button on root element with icon 'expand_more'", () => {
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                    navigationExpanded={{ root: true }}
                />
            );
            expect(wrapper.find(`${rootElement} Button`).props().iconName).to.equal('expand_more');
        });
        it("should show expand button on root element with working onClick handler", () => {
            const handleToggleExpanded = sinon.spy();
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                    handleToggleExpanded={handleToggleExpanded}
                />
            );
            wrapper.find(`${rootElement} Button`).simulate('click');
            expect(handleToggleExpanded.getCall(0).args[0]).to.equal('root');
        });
        it("should show root element with working onClick handler", () => {
            const resultArgument = { newRuleId: 'root', parentId: undefined };
            const handleRuleNavigation = sinon.spy();
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                    handleRuleNavigation={handleRuleNavigation}
                />
            );
            wrapper.find(`button[data-test-id="ecc-silk-mapping__treenav__button-root"]`).simulate('click');
            expect(handleRuleNavigation.getCall(0).args[0]).to.deep.equal(resultArgument);
        });
        it("should show root title element", () => {
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                />
            );
            expect(wrapper.find('RuleTitle')).to.have.lengthOf(1);
        });
        it("should show root title element with formatted rule object", () => {
            const resultObject = { ...tree };
            resultObject.expanded = false;
            resultObject.isHighlighted = true;
            resultObject.rules.propertyRules[0].expanded = false;
            resultObject.rules.propertyRules[0].isHighlighted = false;
            resultObject.rules.propertyRules[0].rules.propertyRules[0].expanded = false;
            resultObject.rules.propertyRules[0].rules.propertyRules[0].isHighlighted = false;
            resultObject.rules.propertyRules[0].rules.propertyRules[1].expanded = false;
            resultObject.rules.propertyRules[0].rules.propertyRules[1].isHighlighted = false;
            resultObject.rules.propertyRules[0].rules.propertyRules[0].rules.propertyRules[0].expanded = false;
            resultObject.rules.propertyRules[0].rules.propertyRules[0].rules.propertyRules[0].isHighlighted = false;
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                />
            );
            expect(wrapper.find('RuleTitle').props().rule).to.deep.equal(resultObject);
        });
        it("should show root element", () => {
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                    navigationExpanded={{ root: true }}
                />
            );
            expect(wrapper.find(rootTree)).to.have.lengthOf(1);
        });
        it("should show expand button on child element with icon 'arrow_nextpage'", () => {
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                    navigationExpanded={{ root: true }}
                />
            );
            expect(wrapper.find(`${childElement} Button`).props().iconName).to.equal('arrow_nextpage');
        });
        it("should show expand button on child element with icon 'expand_more'", () => {
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                    navigationExpanded={{
                        root: true,
                        object1: true,
                    }}
                />
            );
            expect(wrapper.find(childElementExpander).props().iconName).to.equal('expand_more');
        });
        it("should show expand button on child element with working onClick handler", () => {
            const handleToggleExpanded = sinon.spy();
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                    navigationExpanded={{ root: true }}
                    handleToggleExpanded={handleToggleExpanded}
                />
            );
            wrapper.find(childElementExpander).simulate('click');
            expect(handleToggleExpanded.getCall(0).args[0]).to.equal('object1');
        });
        it("should show child element with working onClick handler", () => {
            const resultArgument = { newRuleId: 'object1', parentId: undefined };
            const handleRuleNavigation = sinon.spy();
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                    navigationExpanded={{ root: true }}
                    handleRuleNavigation={handleRuleNavigation}
                />
            );
            wrapper.find(`button[data-test-id="ecc-silk-mapping__treenav__button-object1"]`).simulate('click');
            expect(handleRuleNavigation.getCall(0).args[0]).to.deep.equal(resultArgument);
        });
        it("should show child title element", () => {
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                    navigationExpanded={{ root: true }}
                />
            );
            expect(wrapper.find(`${childTree} RuleTitle`)).to.have.lengthOf(1);
        });
        it("should show child title element with formatted rule object", () => {
            const resultObject = { ...tree.rules.propertyRules[0] };
            resultObject.expanded = false;
            resultObject.isHighlighted = false;
            resultObject.rules.propertyRules[0].expanded = false;
            resultObject.rules.propertyRules[0].isHighlighted = false;
            resultObject.rules.propertyRules[1].expanded = false;
            resultObject.rules.propertyRules[1].isHighlighted = false;
            resultObject.rules.propertyRules[0].rules.propertyRules[0].expanded = false;
            resultObject.rules.propertyRules[0].rules.propertyRules[0].isHighlighted = false;
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                    navigationExpanded={{ root: true }}
                />
            );
            expect(wrapper.find(`${childTree} RuleTitle`).props().rule).to.deep.equal(resultObject);
        });
        it("should show child type element with formatted rule object", () => {
            const resultObject = { ...tree.rules.propertyRules[0] };
            resultObject.expanded = false;
            resultObject.isHighlighted = false;
            resultObject.rules.propertyRules[0].expanded = false;
            resultObject.rules.propertyRules[0].isHighlighted = false;
            resultObject.rules.propertyRules[1].expanded = false;
            resultObject.rules.propertyRules[1].isHighlighted = false;
            resultObject.rules.propertyRules[0].rules.propertyRules[0].expanded = false;
            resultObject.rules.propertyRules[0].rules.propertyRules[0].isHighlighted = false;
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                    navigationExpanded={{ root: true }}
                />
            );
            expect(wrapper.find(`${childTree} RuleTypes`).props().rule).to.deep.equal(resultObject);
        });
        it("should show Icon on sub child element with icon 'radio_button_unchecked'", () => {
            const wrapper = shallow(
                <MappingsTree
                    { ...props }
                    navigationExpanded={{
                        root: true,
                        object1: true,
                    }}
                />
            );
            expect(
                wrapper
                    .find('Icon[data-test-id="ecc-silk-mapping__treenav--item-toggler-object3"]')
                    .props()
                    .name
            ).to.equal('radio_button_unchecked');
        });
    });
});

