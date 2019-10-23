import React from "react";
import { mount, shallow } from 'enzyme';
import { ObjectMappingRuleForm } from '../../../../src/HierarchicalMapping/Containers/MappingRule/ObjectMappingRule/ObjectMappingRuleForm';
import { CardTitle, Spinner } from '@eccenca/gui-elements';
import ErrorView from '../../../../src/HierarchicalMapping/Components/ErrorView';
import ExampleView from '../../../../src/HierarchicalMapping/Containers/MappingRule/ExampleView';
import * as Store from '../../../../src/HierarchicalMapping/store';
import EventEmitter from '../../../../src/HierarchicalMapping/utils/EventEmitter';

const props = {
    id: '1',
    parentId: '2',
    parent: {
        id: '2',
        type: 'object',
    },
    scrollIntoView: jest.fn(),
    scrollElementIntoView: jest.fn(),
    ruleData: {
        type: 'object',
        targetProperty: '',
        entityConnection: '',
        uriRuleType: 'uri',
        pattern: "pattern"
    },
};

const selectors = {
    TARGET_PROP_AUTOCOMPLETE: '[data-id="autocomplete_target_prop"]',
    ENTITY_CON_RADIO: '[data-id="entity_radio_group"]',
    SOURCE_PROP_AUTOCOMPLETE: '[data-id="autocomplete_source_prop"]',
    URI_INPUT: '.ecc-silk-mapping__ruleseditor__pattern',
    RULE_LABEL_INPUT: '.ecc-silk-mapping__ruleseditor__label',
    RULE_DESC_INPUT: '.ecc-silk-mapping__ruleseditor__comment',
    CONFIRM_BUTTON: 'button.ecc-silk-mapping__ruleseditor__actionrow-save',
    CANCEL_BUTTON: 'button.ecc-silk-mapping__ruleseditor__actionrow-cancel',
};



const getWrapper = (renderer = shallow, arg = props) => renderer(
    <ObjectMappingRuleForm {...arg} />
);

describe("ObjectMappingRuleForm Component", () => {
    describe("on component mounted, ", () => {
        let wrapper;
        beforeEach(() => {
            wrapper = getWrapper(shallow);
        });
        
        it("should loading indicator present if data still loading", () => {
            wrapper.setState({
                loading: true
            });
            expect(wrapper.find(Spinner)).toHaveLength(1);
        });
        
        it("should show the error message, when it's happened", () => {
            wrapper.setState({
                saveObjectError: {
                    title: 'Error',
                }
            });
            expect(wrapper.find(ErrorView)).toHaveLength(1);
        });
        
        it("should show the title, when `id` not presented", () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                id: false
            });
            expect(wrapper.find(CardTitle)).toHaveLength(1);
        });
        
        describe('when `ruleData.type` Not equal to `root` ', () => {
            it('should render Target property Autocomplete box', () => {
                expect(wrapper.find(selectors.TARGET_PROP_AUTOCOMPLETE)).toHaveLength(1);
            });
        
            it('should render Radio group of entity connections', () => {
                expect(wrapper.find(selectors.ENTITY_CON_RADIO)).toHaveLength(1);
            });
        
            it('should render Source property Autocomplete box', () => {
                expect(wrapper.find(selectors.SOURCE_PROP_AUTOCOMPLETE)).toHaveLength(1);
            });
        });
    
        it('should render URI pattern input box, when `id` presented', () => {
            expect(wrapper.find(selectors.URI_INPUT)).toHaveLength(1);
        });
    
        it('should render ExampleView component, when pattern or uriRule presented', () => {
            expect(wrapper.find(ExampleView)).toHaveLength(1);
        });
    
        it('should render input for editing label of rule', () => {
            expect(wrapper.find(selectors.RULE_LABEL_INPUT)).toHaveLength(1);
        });
    
        it('should render input for editing description of rule', () => {
            expect(wrapper.find(selectors.RULE_DESC_INPUT)).toHaveLength(1);
        });
        
        afterEach(() => {
            wrapper.unmount();
        });
    });
    
    describe("on user interaction", () => {
        let emitMock;
        beforeEach(() => {
            emitMock = jest.spyOn(EventEmitter, 'emit');
        });
        
        it("should save button call createMapping function, when value changed", () => {
            const createMappingAsyncMock = jest.spyOn(Store, 'createMappingAsync');
            const wrapper = getWrapper(mount, {
                ...props,
                ruleData: {
                    ...props.ruleData,
                    type: 'root',
                }
            });
            wrapper.setState({
                changed: true
            });
            wrapper.find(selectors.CONFIRM_BUTTON).first().simulate("click");
            expect(createMappingAsyncMock).toBeCalled();
        });
        
        it("should cancel button emit the event which will discard the form", () => {
            const wrapper = getWrapper(mount);
            wrapper.find(selectors.CANCEL_BUTTON).first().simulate("click", {
                stopPropagation: jest.fn()
            });
            expect(emitMock).toBeCalledWith('ruleView.unchanged', {
                id: '1'
            });
            expect(emitMock).toBeCalledWith('ruleView.close', {
                id: '1'
            });
        });
        
        afterEach(() => {
            emitMock.mockReset();
        })
    });
});
