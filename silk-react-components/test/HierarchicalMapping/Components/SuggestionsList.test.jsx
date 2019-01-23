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

// async magic

const flushPromises = () => {
    return new Promise(resolve => setImmediate(resolve));
};

function wait() {
    return new Promise(resolve => {
        setTimeout(resolve, timeout)
    });
}

// tests

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
        const cancelButton = component.find('button.ecc-hm-suggestions-cancel');
        expect(cancelButton).to.have.lengthOf(1);
    });

    it('does not load anymore', async () => {
        const loadingSpinner = component.find('div.mdl-spinner');
        expect(loadingSpinner).to.have.lengthOf(0);
    });

    it('contains 4 elements', async () => {
        // The list show 4 elements
        const suggestionsList = component.find('.ecc-silk-mapping__suggestionlist .mdl-list .ecc-silk-mapping__ruleitem--literal');
        expect(suggestionsList).to.have.lengthOf(4);
    });

});

describe('SuggestionsList render with wrong responses ( both 404 )', async () => {

    const component = mountSuggestionsList("404", "404");

    it('mounts', async () => {
        // wait until the component having time to start doing promises
        await wait();
        // wait until all initialized promises finished
        await flushPromises();
        // an update is required to ensure the last render is called
        component.update();
    });

    it('contains no error message boxes', async () => {
        const errors = component.find('div.mdl-alert--danger');
        expect(errors).to.have.lengthOf(0);
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

    it('contains 2 error box with text', async () => {
        const errors = component.find('.mdl-alert--danger');
        expect(errors).to.have.lengthOf(2);

        const errorStrings = [
            "first error title",
            "second error title",
        ];

        errors.forEach((node, id) => {
            expect(node.text()).to.contain(errorStrings[id]);
        });

    })
});



