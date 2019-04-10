import React from 'react';
import chai, {expect, assert} from 'chai';
import { mount } from 'enzyme';
import chaiEnzyme from "chai-enzyme";
import Enzyme from "enzyme/build";
import Adapter from "enzyme-adapter-react-15/build";
import MappingRule from '../../../../src/HierarchicalMapping/Components/MappingRule/MappingRule';
import waitUntilReady from '../../../test_helper'
import {DragDropContext, Droppable} from "react-beautiful-dnd";
import sinon from 'sinon';
import SuggestionsList from '../../../../src/HierarchicalMapping/Components/SuggestionsList';

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

describe("MappingRule with label existing and not empty", () => {
    const component = mountMappingRule(sampleData.uri, sampleData.label);

    sinon.spy(MappingRule.prototype, 'componentDidMount');

    beforeEach(async () => {
        await waitUntilReady(component);
    });

    it('mounts', async () => {
        expect(MappingRule.prototype.componentDidMount.calledOnce);
    });

    it('shows the right label text', () => {
        expect(label(component).text()).to.equal(sampleData.label);
    });

    it('shows the right uri text', () => {
        expect(uri(component).text()).to.equal(sampleData.uri);
    });
});

describe("MappingRule with label equals URI", () => {
    const component = mountMappingRule(sampleData.uri, sampleData.uri);

    beforeEach(async () => {
        await waitUntilReady(component);
    });

    it('mounts', async () => {
        await waitUntilReady(component);
    });

    it('shows the right label text', () => {
        expect(label(component).text()).to.equal(sampleData.uri);
    });

    it('does not show uri text', () => {
        expect(uri(component)).to.lengthOf(0);
    });
});

describe("MappingRule with no label and absolute URI", () => {
    const component = mountMappingRule(sampleData.uri, null);

    beforeEach(async () => {
        await waitUntilReady(component);
    });

    it('mounts', async () => {
        await waitUntilReady(component);
    });

    it('shows the right label text', () => {
        expect(label(component).text()).to.equal(sampleData.uriLabel);
    });

    it('shows the right uri text', () => {
        expect(uri(component).text()).to.equal(sampleData.uri);
    });
});

describe("MappingRule with no label and relative URI", () => {
    const component = mountMappingRule(sampleData.uriRelative, null);

    beforeEach(async () => {
        await waitUntilReady(component);
    });

    it('mounts', async () => {
        await waitUntilReady(component);
    });

    it('shows the right label text', () => {
        expect(label(component).text()).to.equal(sampleData.uriLabelRelative);
    });

    it('does not show uri text', () => {
        expect(uri(component)).to.lengthOf(0);
    });
});
