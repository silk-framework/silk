import React from 'react';
import {expect, assert} from 'chai';
import { mount } from 'enzyme';
import SuggestionsList from '../../../src/HierarchicalMapping/Components/SuggestionsList';
import './SuggestionsList.server';
import sinon from "sinon";
// Required for the store to work.
import SilkStore from './../../../src/SilkStore/silkStore';

import chai from "chai";
import chaiEnzyme from "chai-enzyme";
import Enzyme from "enzyme/build";
import Adapter from "enzyme-adapter-react-15/build";

chai.use(chaiEnzyme());
Enzyme.configure({ adapter: new Adapter() });

const selectors = require('./SuggestionsList.spec');

// async magic FIXME move to helper

const flushPromises = () => {
    return new Promise(resolve => setImmediate(resolve));
};

function wait() {
    return new Promise(resolve => {
        setTimeout(resolve, timeout)
    });
}

// generator of tests cases

function mountSuggestionsList(testCase1, testCase2) {

    return mount(
        <SuggestionsList
            onClose={() => {}}
            parent={{}}
            ruleId={testCase1}
            targetClassUris={[testCase2]}
        />
    );
}

const timeout = 5;

describe('SuggestionsList render with correct responses ( both 200 )', async () => {

    // set spy on component did mount to check how oft it is called
    sinon.spy(SuggestionsList.prototype, 'componentDidMount');
    // mount the SuggestionsList
    const component = mountSuggestionsList("200", "200");

    it('mounts once', async () => {
        // wait until the component having time to start doing promises
        await wait();
        // wait until all initialized promises finished
        await flushPromises();
        // an update is required to ensure the last render is called
        component.update();
        expect(SuggestionsList.prototype.componentDidMount.calledOnce);
    });

    it('has a cancel button', async () => {
        const cancelButton = component.find(selectors.cancel);
        expect(cancelButton).to.have.lengthOf(1);
    });

    it('does not load anymore', async () => {
        const loadingSpinner = component.find(selectors.loader);
        expect(loadingSpinner).to.have.lengthOf(0);
    });

    it('contains 4 elements', async () => {
        // The list show 4 elements
        const suggestionsList = component.find(selectors.suggestions);
        expect(suggestionsList).to.have.lengthOf(4);
    });

});

describe('SuggestionsList render with wrong responses ( both 404 mit errors )', async () => {

    const component = mountSuggestionsList("404", "404");

    it('mounts', async () => {
        // wait until the component having time to start doing promises
        await wait();
        // wait until all initialized promises finished
        await flushPromises();
        // an update is required to ensure the last render is called
        component.update();
    });

    it('contains two error message boxes', async () => {
        expect(component.find(selectors.errors)).to.have.lengthOf(2);
        expect(component.find(selectors.errorsContainer)).to.have.lengthOf(1);
    })
});

describe('SuggestionsList render with wrong responses ( 404 {title: "Not Found", detail: "Not found"}), 404 {title: "Not Found", detail: "Not found"}', async () => {

    const component = mountSuggestionsList("404NF", "404NF");

    it('mounts', async () => {
        // FIXME: move to a helper function
        // wait until the component having time to start doing promises
        await wait();
        // wait until all initialized promises finished
        await flushPromises();
        // an update is required to ensure the last render is called
        component.update();
    });

    it('contains one error message box with 1 error', async () => {
        expect(component.find(selectors.errors)).to.have.lengthOf(1);
        expect(component.find(selectors.errorsContainer)).to.have.lengthOf(1);
    })
});


describe('SuggestionsList render with wrong responses ( both 500 )', async () => {

    const component = mountSuggestionsList("500", "500");

    it('mounts', async () => {
        // wait until the component having time to start doing promises
        await wait();
        // wait until all initialized promises finished
        await flushPromises();
        // an update is required to ensure the last render is called
        component.update();

    });

    it('contains 1 error box with 2 errors', async () => {
        expect(component.find(selectors.errors)).to.have.lengthOf(2);
        expect(component.find(selectors.errorsContainer)).to.have.lengthOf(1);

        const errorStrings = [
            "first error title",
            "second error title",
        ];

        component.find(selectors.errors).forEach((node, id) => {
            expect(node.text()).to.contain(errorStrings[id]);
        });

    })
});


// FIXME TEST: add more cases?

