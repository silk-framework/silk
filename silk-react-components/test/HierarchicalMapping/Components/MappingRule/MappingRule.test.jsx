import React from 'react';
import chai, {expect, assert} from 'chai';
import { mount } from 'enzyme';
import chaiEnzyme from "chai-enzyme";
import Enzyme from "enzyme/build";
import Adapter from "enzyme-adapter-react-15/build";
import MappingRule from '../../../../src/HierarchicalMapping/Components/MappingRule/MappingRule';
import {getRuleLabel} from '../../../../src/HierarchicalMapping/helpers';
import waitUntilReady from '../../../test_helper'
import {DragDropContext, Droppable} from "react-beautiful-dnd";
import sinon from 'sinon';

chai.use(chaiEnzyme());
Enzyme.configure({ adapter: new Adapter() });

const props = (uri, label) => {
    return {
        "pos": 4,
        "parentId": "obj",
        "count": 6,
        "type": "direct",
        "id": "hasName1",
        "sourcePath": "name",
        "mappingTarget": {
            "uri": uri,
            "valueType": {
                "nodeType": "AutoDetectValueType"
            },
            "isBackwardProperty": false,
            "isAttribute": false
        },
        "metadata": {
            "label": label
        },
        "provided": true,
        "snapshot": true
    }
};

const selectors = {
    label : ".ecc-silk-mapping__ruleitem-label",
    uri : ".ecc-silk-mapping__ruleitem-extraline",
};

const sampleData = {
    uri : "fibo-be-lex-nam:hasName",
    uriRelative : "name",
    label : "myLabel",
    uriLabel : "Has Name",
    uriLabelRelative : "Name",
};

const mountMappingRule = (uri, label) => mount(
    <DragDropContext
        onDragStart={() => {}}
        onDragEnd={() => {}}>
        <Droppable droppableId="droppable">
            {(provided, snapshot) => (
                <div>
                    <MappingRule
                        {...props(uri, label)}
                    />
                    {provided.placeholder}
                </div>
            )}
        </Droppable>
    </DragDropContext>
);

const label = (component) => {
    return component.find(selectors.label)
};

const uri = (component) => {
    return component.find(selectors.uri)
};

describe("MappingRule", () => {

    it('should correctly display label and target URI in the UI', async () => {
        const component = mountMappingRule(sampleData.uri, sampleData.label);

        sinon.spy(MappingRule.prototype, 'componentDidMount');
        await waitUntilReady(component);
        expect(MappingRule.prototype.componentDidMount.calledOnce);
        expect(label(component).text()).to.equal(sampleData.label);
        expect(uri(component).text()).to.equal(sampleData.uri);
    });

    const labelUri = (label, uri) => getRuleLabel({label, uri});

    it('should show label and URI if both are specified', () => {
        expect(labelUri(sampleData.label, sampleData.uri)).to.include({displayLabel: sampleData.label, uri: sampleData.uri});
    });

    it('should generate a label out of the last part of the (camel-case) URI if no label was given', () => {
        expect(labelUri('', 'labelCamelCase')).to.include({displayLabel: 'Label Camel Case', uri: 'labelCamelCase'});
        expect(labelUri(null, 'http://some/path/label')).to.include({displayLabel: 'Label', uri: 'http://some/path/label'});
        expect(labelUri(null, 'http://some/path/labelCamelCase')).to.include({displayLabel: 'Label Camel Case', uri: 'http://some/path/labelCamelCase'});
        expect(labelUri(undefined, 'urn:test:labelCamelCase')).to.include({displayLabel: 'Label Camel Case', uri: 'urn:test:labelCamelCase'});
        expect(labelUri(undefined, 'http://uriWithHash/dontUseThis#labelCamelCase')).to.include({displayLabel: 'Label Camel Case', uri: 'http://uriWithHash/dontUseThis#labelCamelCase'});
    });

    it('should only show the URI as display label if the URI is the same (case-insensitive) as the label', () => {
        expect(labelUri('urn:test:Test', 'urn:test:Test')).to.include({displayLabel: 'urn:test:Test', uri: null});
        expect(labelUri('urn:test:Test', 'urn:test:test')).to.include({displayLabel: 'urn:test:test', uri: null});
        expect(labelUri('http://domain.suffix/Test', 'http://domain.suffix/Test')).to.include({displayLabel: 'http://domain.suffix/Test', uri: null});
        expect(labelUri('http://domain.suffix/test', 'http://domain.suffix/Test')).to.include({displayLabel: 'http://domain.suffix/Test', uri: null});
        expect(labelUri('urn:test:Test', 'urn:test:test')).to.include({displayLabel: 'urn:test:test', uri: null});
        expect(labelUri('someTest', 'someTest')).to.include({displayLabel: 'someTest', uri: null});
        expect(labelUri('someTest', 'sometest')).to.include({displayLabel: 'sometest', uri: null});
    });

    it('should only show the URI as display label if the generated display label does not differ case-insensitive', () => {
        expect(labelUri(null, 'Text')).to.include({displayLabel: 'Text', uri: null});
        expect(labelUri(null, 'text')).to.include({displayLabel: 'text', uri: null});
    });
});
