import React from 'react';
import chai, { assert, expect } from 'chai';
import { mount } from 'enzyme';
import sinon from "sinon";
import chaiEnzyme from "chai-enzyme";
import Enzyme from "enzyme/build";
import Adapter from "enzyme-adapter-react-15/build";
import waitUntilReady from '../../test_helper';
import { SessionStorage } from '../../test_helper';

import MappingsWorkview from '../../../src/HierarchicalMapping/Components/MappingsWorkview';
import { propertyRules } from './MappingsWorkview.server';

chai.use(chaiEnzyme());
Enzyme.configure({ adapter: new Adapter() });

/**
 * Setting up the session storage
 * In the MappingsWorkview component the sessionStorage is used to store meta data of a mapping rule when the 'Copy'
 * button is clicked.
 *
 * @type {SessionStorage}
 */
global.sessionStorage = new SessionStorage();

/**
 * HTML Selectors
 *
 * @type {{listItems: string, plusButton: string, actionsMenu: string, copyButton: string, cloneButton: string, row: string}}
 */
const selectors = {
		copyButton: ".ecc-silk-mapping__ruleitem-expanded .ecc-silk-mapping__rulesviewer div.ecc-silk-mapping__ruleseditor__actionrow button.ecc-silk-mapping__ruleseditor__actionrow-copy",
		cloneButton: ".ecc-silk-mapping__ruleitem-expanded .ecc-silk-mapping__rulesviewer div.ecc-silk-mapping__ruleseditor__actionrow",
		row: '.mdl-list .clickable',
		listItems: '.mdl-list',
		plusButton: '.ecc-floatingactionlist__wrapper--fixed button.ecc-floatingactionlist__button',
		actionsMenu: '.ecc-floatingactionlist__wrapper--fixed ul.ecc-floatingactionlist__menu'
	},

	/**
	 * @mock onCopyHandler
	 * @mock onCloneHandler
	 * @mock onPasteHandler
	 */
	onCopyHandler = sinon.spy(),
	onCloneHandler = sinon.spy(),
	onPasteHandler = sinon.spy();
/**
 * Mounting the component
 *
 * @returns {*}
 */
const mountSuggestionsList = () => {
	return mount(
		<MappingsWorkview
			currentRuleId={'root'}
		/>
	);
};

describe('MappingsWorkview', () => {
	// set spy on component did mount to check how oft it is called
	sinon.spy(MappingsWorkview.prototype, 'componentDidMount');
	// mount the MappingsWorkview
	const component = mountSuggestionsList();
	it('mounts once', async () => {
		await waitUntilReady(component);
		expect(MappingsWorkview.prototype.componentDidMount.calledOnce);
	});

	describe('Copy and paste of a mapping rule', () => {
		let item;
		beforeEach(async () => {
			await waitUntilReady(component);
			const items = component.find(selectors.row);
			item = items.at(1);
		});

		const plusButton = component.find(selectors.plusButton),
			actions = component.find(selectors.actionsMenu);
		let copyButton = component.find(selectors.copyButton),
			pasteAction = null;

		it('should copy a rule when clicking the Copy button', () => {
			item.simulate('click');
			if (!copyButton.length) {
				copyButton = component.find(selectors.copyButton);
			}
			expect(copyButton).to.have.lengthOf(1);
			copyButton.simulate('click');
			component.render();
			expect(onCopyHandler.calledOnce);
		});

		it('should paste the rule when clicking on the Paste action', () => {
			// Check for blue plus button
			expect(plusButton).to.have.lengthOf(1);
			plusButton.simulate('click');
			// All actions should be available including the paste action
			expect(actions.children()).to.have.lengthOf(4);
			// find the paste action
			pasteAction = actions.childAt(2).find('button');
			expect(pasteAction).to.have.lengthOf(1);
			pasteAction.simulate('click');
			component.render();
			expect(onPasteHandler.calledOnce);
			item.simulate('click');
		});

		it('should result in containing 3 mapping rules now instead of 2', () => {
			expect(propertyRules).to.have.lengthOf(3);
		});
	});

	describe('Clone a mapping rule', () => {
		const component = mountSuggestionsList();

		let item;
		beforeEach(async () => {
			await waitUntilReady(component);
			const items = component.find(selectors.row);
			item = items.at(0);
		});

		let cloneButton = component.find(selectors.cloneButton);

		it('should click the clone button', () => {
			item.simulate('click');
			if (!cloneButton.length) {
				cloneButton = component.find(selectors.cloneButton);
			}
			expect(cloneButton.childAt(2)).to.have.lengthOf(1);
			cloneButton.childAt(2).simulate('click');
			expect(onCloneHandler.calledOnce);
		});

		it('should now contain 4 mapping rules instead of 3', () => {
			expect(propertyRules).to.have.lengthOf(4);
		});
	});
});
