import React from "react";
import ErrorView from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/ErrorView";
import ExampleView from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/containers/MappingRule/ExampleView";
import * as Store from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/store";
import EventEmitter from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/utils/EventEmitter";
import { ValueRuleForm } from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/containers/MappingRule/ValueRule/ValueRuleForm";
import { clickElement, findAll, withMount } from "../../../utils/TestHelpers";
import { waitFor } from "@testing-library/react";
import { CardTitle, Spinner } from "@eccenca/gui-elements";

const props = {
    id: "1",
    parentId: "2",
    scrollIntoView: jest.fn(),
    scrollElementIntoView: jest.fn(),
    viewActions: {
        savedChanges: jest.fn(),
    },
};

const selectors = {
    SOURCE_PROP_AUTOCOMPLETE: ".ecc-silk-mapping__ruleseditor__sourcePath",
    TARGET_PROP_AUTOCOMPLETE: ".ecc-silk-mapping__ruleseditor__targetProperty",
    DATA_TYPE_AUTOCOMPLETE: ".ecc-silk-mapping__ruleseditor__propertyType",
    TARGET_CARDINALITY: ".ecc-silk-mapping__ruleseditor__isAttribute",
    INPUT_COMPLEX: '[data-id="test-complex-input"]',
    LNG_SELECT_BOX: '[data-id="lng-select-box"]',
    RULE_LABEL_INPUT: ".ecc-silk-mapping__ruleseditor__label",
    RULE_DESC_INPUT: ".ecc-silk-mapping__ruleseditor__comment",
    CONFIRM_BUTTON: "button.ecc-silk-mapping__ruleseditor__actionrow-save",
    CANCEL_BUTTON: "button.ecc-silk-mapping__ruleseditor___actionrow-cancel",
};
const getWrapper = (arg = props) => withMount(<ValueRuleForm {...arg} openMappingEditor={() => {}} />);

jest.mock("../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/store", () => {
    const asyncMockFn =
        (returnObject: any = {}) =>
        () => {
            return {
                // Simulate async behavior via setTimeout
                subscribe: (resultCallback: (result) => any) => setTimeout(() => resultCallback(returnObject), 1),
            };
        };
    const functionMock = {
        getHierarchyAsync: asyncMockFn(),
        getRuleAsync: asyncMockFn({
            rule: {
                sourcePath: "sourcePath",
            },
        }),
    };
    return {
        ...jest.requireActual("../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/store"),
        ...functionMock,
    };
});

describe("ValueMappingRuleForm Component", () => {
    // FIXME: Many tests don't work anymore, since they rely on changing state of a React class component.
    describe("ValueMappingRuleForm Component when mounted", () => {
        let wrapper;
        beforeEach(() => {
            wrapper = getWrapper();
        });

        it("should loading indicator present if data still loading", () => {
            expect(wrapper.find(Spinner)).toHaveLength(1);
        });

        xit("should show the error message, when it's happened", () => {
            wrapper.setState({
                error: {
                    response: {
                        body: "Error",
                    },
                },
            });
            expect(wrapper.find(ErrorView)).toHaveLength(1);
        });

        it("should show the title, when `id` not presented", () => {
            const wrapper = getWrapper({
                ...props,
                id: false,
            });
            expect(wrapper.find(CardTitle)).toHaveLength(1);
        });

        xit("should render Source property Autocomplete box, when rule type equal to `direct` ", () => {
            wrapper.setState({
                type: "direct",
                loading: false,
            });
            expect(wrapper.find(selectors.SOURCE_PROP_AUTOCOMPLETE)).toHaveLength(1);
        });

        xit("should render TextField, when rule type equal to `complex` ", () => {
            wrapper.setState({
                type: "complex",
                loading: false,
            });
            expect(wrapper.find(selectors.INPUT_COMPLEX)).toHaveLength(1);
        });

        xit("should render ExampleView, when sourceProperty not empty", () => {
            wrapper.setState({
                sourceProperty: ["1"],
                loading: false,
            });
            expect(wrapper.find(ExampleView)).toHaveLength(1);
        });

        it("should render Target property autocomplete", async () => {
            await waitFor(() => {
                expect(findAll(wrapper, selectors.TARGET_PROP_AUTOCOMPLETE)).toHaveLength(1);
            });
        });

        it("should render the target cardinality field", async () => {
            await waitFor(() => {
                expect(findAll(wrapper, selectors.TARGET_CARDINALITY)).toHaveLength(1);
            });
        });

        it("should render the autocomplete for data types", async () => {
            await waitFor(() => {
                expect(findAll(wrapper, selectors.DATA_TYPE_AUTOCOMPLETE)).toHaveLength(1);
            });
        });

        xit("should render the language select box, when nodeType equal to `LanguageValueType`", () => {
            wrapper.setState({
                valueType: {
                    nodeType: "LanguageValueType",
                },
            });
            expect(wrapper.find(selectors.LNG_SELECT_BOX)).toHaveLength(1);
        });

        it("should render input for editing label of rule", async () => {
            await waitFor(() => {
                expect(findAll(wrapper, selectors.RULE_LABEL_INPUT)).toHaveLength(1);
            });
        });

        it("should render input for editing description of rule", async () => {
            await waitFor(() => {
                expect(findAll(wrapper, selectors.RULE_DESC_INPUT)).toHaveLength(1);
            });
        });

        afterEach(() => {
            wrapper.unmount();
        });
    });

    describe("ValueMappingRuleForm Component on user interaction", () => {
        let emitMock;
        beforeEach(() => {
            emitMock = jest.spyOn(EventEmitter, "emit");
        });

        xit("should save button call createMapping function, when value changed and targetProperty presented with language tag", () => {
            const createMappingAsyncMock = jest.spyOn(Store, "createMappingAsync");
            const wrapper = getWrapper({
                ...props,
                ruleData: {
                    ...props.ruleData,
                    type: "root",
                },
            });
            wrapper.setState({
                loading: false,
                changed: true,
                valueType: {
                    nodeType: "en",
                },
                targetProperty: "2",
            });
            wrapper.find(selectors.CONFIRM_BUTTON).first().simulate("click", {
                stopPropagation: jest.fn(),
                persist: jest.fn(),
            });
            expect(createMappingAsyncMock).toBeCalled();
        });

        it("should cancel button emit the event which will discard the form", async () => {
            const wrapper = getWrapper();
            await waitFor(() => {
                clickElement(wrapper, selectors.CANCEL_BUTTON);
            });
            expect(emitMock).toBeCalledWith("ruleView.unchanged", {
                id: "1",
            });
            expect(emitMock).toBeCalledWith("ruleView.close", {
                id: "1",
            });
        });

        afterEach(() => {
            emitMock.mockReset();
        });
    });
});
