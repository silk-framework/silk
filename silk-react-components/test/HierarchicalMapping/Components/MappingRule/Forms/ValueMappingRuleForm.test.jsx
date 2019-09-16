import React from "react";
import { mount, shallow } from 'enzyme';
import { CardTitle, Spinner } from '@eccenca/gui-elements';
import ErrorView from '../../../../../src/HierarchicalMapping/Components/MappingRule/ErrorView';
import ExampleView from '../../../../../src/HierarchicalMapping/Components/MappingRule/ExampleView';
import * as Store from '../../../../../src/HierarchicalMapping/store';
import EventEmitter from '../../../../../src/HierarchicalMapping/utils/EventEmitter';
import { ValueMappingRuleForm } from '../../../../../src/HierarchicalMapping/Components/MappingRule/Forms/ValueMappingRuleForm';

const props = {
    id: '1',
    parentId: '2',
    scrollIntoView: jest.fn(),
    scrollElementIntoView: jest.fn(),
};

const selectors = {
    SOURCE_PROP_AUTOCOMPLETE: '.ecc-silk-mapping__ruleseditor__sourcePath',
    TARGET_PROP_AUTOCOMPLETE: '.ecc-silk-mapping__ruleseditor__targetProperty',
    DATA_TYPE_AUTOCOMPLETE: '.ecc-silk-mapping__ruleseditor__propertyType',
    CHECKBOX: '.ecc-silk-mapping__ruleseditor__isAttribute',
    INPUT_COMPLEX: '[data-id="test-complex-input"]',
    LNG_SELECT_BOX: '[data-id="lng-select-box"]',
    RULE_LABEL_INPUT: '.ecc-silk-mapping__ruleseditor__label',
    RULE_DESC_INPUT: '.ecc-silk-mapping__ruleseditor__comment',
    CONFIRM_BUTTON: 'button.ecc-silk-mapping__ruleseditor__actionrow-save',
    CANCEL_BUTTON: 'button.ecc-silk-mapping__ruleseditor___actionrow-cancel',
};



const getWrapper = (renderer = shallow, arg = props) => renderer(
    <ValueMappingRuleForm {...arg} />
);

describe("ValueMappingRuleForm Component", () => {
    describe("on component mounted, ", () => {
        let wrapper;
        beforeEach(() => {
            wrapper = getWrapper(shallow);
            wrapper.setState({
                loading: false
            })
        });
        
        it("should loading indicator present if data still loading", () => {
            wrapper.setState({
                loading: true
            })
            expect(wrapper.find(Spinner)).toHaveLength(1);
        });
        
        it("should show the error message, when it's happened", () => {
            wrapper.setState({
                error: {
                    response:  {
                        body: 'Error'
                    },
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
    
        it('should render Source property Autocomplete box, when rule type equal to `direct` ', () => {
            wrapper.setState({
                type: 'direct',
                loading: false
            });
            expect(wrapper.find(selectors.SOURCE_PROP_AUTOCOMPLETE)).toHaveLength(1);
        });
    
        it('should render TextField, when rule type equal to `complex` ', () => {
            wrapper.setState({
                type: 'complex',
                loading: false
            });
            expect(wrapper.find(selectors.INPUT_COMPLEX)).toHaveLength(1);
        });
    
        it('should render ExampleView, when sourceProperty not empty', () => {
            wrapper.setState({
                sourceProperty: [
                    '1'
                ],
                loading: false
            });
            expect(wrapper.find(ExampleView)).toHaveLength(1);
        });
    
        it('should render Target property autocomplete', () => {
            expect(wrapper.find(selectors.TARGET_PROP_AUTOCOMPLETE)).toHaveLength(1);
        });
    
        it('should render the checkbox', () => {
            expect(wrapper.find(selectors.CHECKBOX)).toHaveLength(1);
        });
    
        it('should render the autocomplete for data types', () => {
            expect(wrapper.find(selectors.DATA_TYPE_AUTOCOMPLETE)).toHaveLength(1);
        });
    
        it('should render the language select box, when nodeType equal to `LanguageValueType`', () => {
            wrapper.setState({
                valueType: {
                    nodeType: 'LanguageValueType'
                }
            });
            expect(wrapper.find(selectors.LNG_SELECT_BOX)).toHaveLength(1);
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
        
        it("should save button call createMapping function, when value changed and targetProperty presented with language tag", () => {
            const createMappingAsyncMock = jest.spyOn(Store, 'createMappingAsync');
            const wrapper = getWrapper(mount, {
                ...props,
                ruleData: {
                    ...props.ruleData,
                    type: 'root',
                }
            });
            wrapper.setState({
                loading: false,
                changed: true,
                valueType: {
                    nodeType: 'en'
                },
                targetProperty: '2'
            });
            wrapper.find(selectors.CONFIRM_BUTTON).first().simulate("click", {
                stopPropagation: jest.fn(),
                persist: jest.fn()
            });
            expect(createMappingAsyncMock).toBeCalled();
        });
        
        it("should cancel button emit the event which will discard the form", () => {
            const wrapper = getWrapper(mount);
            wrapper.setState({
                loading: false
            });
            
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
