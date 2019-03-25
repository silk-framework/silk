import React from 'react';
import chai, { expect, assert } from 'chai';
import { mount } from 'enzyme';
import sinon from "sinon";
import chaiEnzyme from "chai-enzyme";
import Enzyme from "enzyme/build";
import Adapter from "enzyme-adapter-react-15/build";

import store from './../../../src/HierarchicalMapping/store';
import waitUntilReady from '../../test_helper';

import MappingsList from '../../../src/HierarchicalMapping/Components/MappingsList';
import './MappingList.server';

chai.use(chaiEnzyme());
Enzyme.configure({ adapter: new Adapter() });

function SessionStorage() {
	this.data = {};
	this.setItem = (key, value) => {
		this.data[key] = value;
	};
	this.getItem = (key) => this.data[key];
}

global.sessionStorage = new SessionStorage();

const selectors = {
		copyButton: ".ecc-silk-mapping__ruleitem-expanded .ecc-silk-mapping__rulesviewer div.ecc-silk-mapping__ruleseditor__actionrow button.ecc-silk-mapping__ruleseditor__actionrow-copy",
		cloneButton: ".ecc-silk-mapping__ruleitem-expanded .ecc-silk-mapping__rulesviewer div.ecc-silk-mapping__ruleseditor__actionrow button.ecc-silk-mapping__ruleseditor__actionrow-clone",
		row: '.mdl-list .clickable',
		listItems: '.mdl-list',
		plusButton: '.ecc-floatingactionlist__wrapper--fixed button.ecc-floatingactionlist__button',
		actionsMenu: '.ecc-floatingactionlist__wrapper--fixed ul.ecc-floatingactionlist__menu'
	},
	mockRules = [
		{
			id: "country",
			mappingTarget: {
				isAttribute: false,
				isBackwardProperty: false,
				uri: "<urn:ruleProperty:country>",
				valueType: {
					nodeType: "StringValueType"
				}
			},
			metadata: {
				label: ""
			},
			sourcePath: "dbpediaowl:country",
			type: "direct"
		},
		{
			id: "basedOn",
			mappingTarget: {
				isAttribute: false,
				isBackwardProperty: false,
				uri: "<urn:ruleProperty:basedOn>",
				valueType: {
					nodeType: "StringValueType"
				}
			},
			metadata: {
				label: ""
			},
			sourcePath: "dbpediaowl:basedOn",
			type: 'direct'
		}
	],
	onCopyHandler = sinon.spy(),
	onCloneHandler = sinon.spy(),
	onPastHandler = sinon.spy();
let isCopying = false;

const mountSuggestionsList = () => {
	return mount(
		<MappingsList
			currentRuleId={'root'}
			rules={mockRules}
			handleCopy={onCopyHandler}
			handlePaste={onPastHandler}
			handleClone={onCloneHandler}
			isCopying={isCopying}
		/>
	);
};

describe('MappingList copy and past', () => {
	// set spy on component did mount to check how oft it is called
	sinon.spy(MappingsList.prototype, 'componentDidMount');
	// mount the MappingList
	const component = mountSuggestionsList();

	it('mounts once', async () => {
		await waitUntilReady(component);
		expect(MappingsList.prototype.componentDidMount.calledOnce);
	});

	describe('should simulate click on plus and do copy and past', () => {
		let item;
		beforeEach(async () => {
			await waitUntilReady(component);
			const items = component.find(selectors.row);
			item = items.at(1);
		});

		let copyButton = component.find(selectors.copyButton),
			plusButton = component.find(selectors.plusButton),
			actions = component.find(selectors.actionsMenu),
			pastAction = null;

		it('should copy button have length', () => {
			item.simulate('click');
			if (!copyButton.length) {
				copyButton = component.find(selectors.copyButton);
			}
			expect(copyButton).to.have.lengthOf(1);
		});

		it('should simulate click of copy button', () => {
			copyButton.simulate('click');
			isCopying = true;
			component.render();
			expect(onCopyHandler.calledOnce);
		});

		it('should plus button have length', () => {
			expect(plusButton).to.have.lengthOf(1);
			plusButton.simulate('click');
		});

		it('should actions menu have children of length 4', () => {
			expect(actions.children()).to.have.lengthOf(4);
		});

		it('should find the past action', () => {
			pastAction = actions.childAt(2);
			expect(pastAction).to.have.lengthOf(1);
		});

		it('should simulate click on past action', () => {
			pastAction.simulate('click');
			isCopying = false;
			component.render();
			expect(onPastHandler.calledOnce);
			item.simulate('click');
		});
	});
});

describe('MappingList clone and past', () => {
	const component = mountSuggestionsList();

	it('mounts once', async () => {
		await waitUntilReady(component);
	});

	describe('should simulate click on plus and do clone and past', () => {
		let item;
		beforeEach(async () => {
			await waitUntilReady(component);
			const items = component.find(selectors.row);
			item = items.at(0);
		});

		let cloneButton = component.find(selectors.cloneButton),
			plusButton = component.find(selectors.plusButton),
			actions = component.find(selectors.actionsMenu),
			pastAction = null;

		it('should clone button have length', () => {
			item.simulate('click');
			if (!cloneButton.length) {
				cloneButton = component.find(selectors.copyButton);
			}
			expect(cloneButton).to.have.lengthOf(1);
		});

		it('should simulate click of clone button', () => {
			cloneButton.simulate('click');
			expect(onCloneHandler.calledOnce);
		});

		it('should plus button have length', () => {
			expect(plusButton).to.have.lengthOf(1);
			plusButton.simulate('click');
		});

		it('should actions menu have children of length 4', () => {
			expect(actions.children()).to.have.lengthOf(4);
		});

		it('should find the past action', () => {
			pastAction = actions.childAt(2);
			expect(pastAction).to.have.lengthOf(1);
		});

		it('should simulate click on past action', () => {
			pastAction.simulate('click');
			expect(onPastHandler.calledOnce);
			item.simulate('click');
		});
	});
});

