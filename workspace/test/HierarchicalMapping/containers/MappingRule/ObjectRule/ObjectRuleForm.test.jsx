import React from "react";
import {mount, shallow} from 'enzyme';
import {ObjectRuleForm} from '../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/containers/MappingRule/ObjectRule/ObjectRuleForm';
import {CardTitle} from 'gui-elements-deprecated';
import ExampleView from '../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/containers/MappingRule/ExampleView';
import * as Store from '../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/store';
import EventEmitter from '../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/utils/EventEmitter';
import {byTestId, changeValue, findAll, findSingleElement, logWrapperHtml} from "../../../utils/TestHelpers";
import {waitFor} from "@testing-library/react";

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
        pattern: "pattern",
        isAttribute: false
    },
};

const selectors = {
    TARGET_PROP_AUTOCOMPLETE: '[data-id="autocomplete_target_prop"]',
    ENTITY_CON_RADIO: 'ul[data-id="entity_radio_group"]',
    OBJECT_VALUE_PATH: '#object-value-path-auto-suggestion',
    URI_INPUT: '#uri-pattern-auto-suggestion',
    RULE_LABEL_INPUT: '.ecc-silk-mapping__ruleseditor__label',
    RULE_DESC_INPUT: '.ecc-silk-mapping__ruleseditor__comment',
    CONFIRM_BUTTON: 'button.ecc-silk-mapping__ruleseditor__actionrow-save',
    CANCEL_BUTTON: 'button.ecc-silk-mapping__ruleseditor__actionrow-cancel',
};



const getWrapper = (arg = props) => mount(
    <ObjectRuleForm {...arg} />
);

describe("ObjectMappingRuleForm Component", () => {
    describe("on component mounted, ", () => {
        let wrapper;
        beforeEach(() => {
            wrapper = getWrapper();
        });

        it("should show the title, when `id` not presented", () => {
            const wrapper = getWrapper({
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
                expect(findAll(wrapper, selectors.ENTITY_CON_RADIO)).toHaveLength(1);
            });

            it('should render Source property Autocomplete box', () => {
                expect(findAll(wrapper, selectors.OBJECT_VALUE_PATH)).toHaveLength(1);
            });
        });

        it('should render URI pattern input box, when `id` presented', () => {
            expect(findAll(wrapper, selectors.URI_INPUT)).toHaveLength(1);
        });

        it('should render ExampleView component, when pattern or uriRule presented', () => {
            expect(findAll(wrapper, byTestId("object-rule-form-example-preview"))).toHaveLength(1);
        });

        it('should render input for editing label of rule', () => {
            expect(findAll(wrapper, selectors.RULE_LABEL_INPUT)).toHaveLength(1);
        });

        it('should render input for editing description of rule', () => {
            expect(findAll(wrapper, selectors.RULE_DESC_INPUT)).toHaveLength(1);
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

        it("should save button call createMapping function, when value changed", async () => {
            const createMappingAsyncMock = jest.spyOn(Store, 'createMappingAsync');
            const wrapper = getWrapper({
                ...props,
                ruleData: {
                    ...props.ruleData,
                    type: 'root',
                    label: "initial label"
                }
            });
            const input = findSingleElement(wrapper, "[data-test-id=\"object-rule-form-label-input\"]")
            changeValue(input, "new label")
            await waitFor(() => {
                wrapper.update()
                findSingleElement(wrapper, selectors.CONFIRM_BUTTON).simulate("click");
                expect(createMappingAsyncMock).toBeCalled();
            })
        });

        it("should cancel button emit the event which will discard the form", () => {
            const wrapper = getWrapper();
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
