import React from "react";
import ExampleView from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/containers/MappingRule/ExampleView";
import * as Store from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/store";
import EventEmitter from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/utils/EventEmitter";
import { ValueRuleForm } from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/containers/MappingRule/ValueRule/ValueRuleForm";
import { render, waitFor } from "@testing-library/react";
import { clickFoundElement, findAllDOMElements } from "../../../../integration/TestHelper";

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
const getWrapper = (arg = props) => render(<ValueRuleForm {...arg} openMappingEditor={() => {}} />);

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
            expect(findAllDOMElements(wrapper, `[class*="-spinner"]`).length).toBeGreaterThan(1);
        });

        it("should show the title, when `id` not presented", () => {
            const wrapper = getWrapper({
                ...props,
                id: false,
            });
            expect(findAllDOMElements(wrapper, `[class*="-card__title"]`)).toHaveLength(1);
        });

        it("should render Target property autocomplete", async () => {
            await waitFor(() => {
                expect(findAllDOMElements(wrapper, selectors.TARGET_PROP_AUTOCOMPLETE)).toHaveLength(1);
            });
        });

        it("should render the target cardinality field", async () => {
            await waitFor(() => {
                expect(findAllDOMElements(wrapper, selectors.TARGET_CARDINALITY)).toHaveLength(1);
            });
        });

        it("should render the autocomplete for data types", async () => {
            await waitFor(() => {
                expect(findAllDOMElements(wrapper, selectors.DATA_TYPE_AUTOCOMPLETE)).toHaveLength(1);
            });
        });

        it("should render input for editing label of rule", async () => {
            await waitFor(() => {
                expect(findAllDOMElements(wrapper, selectors.RULE_LABEL_INPUT)).toHaveLength(1);
            });
        });

        it("should render input for editing description of rule", async () => {
            await waitFor(() => {
                expect(findAllDOMElements(wrapper, selectors.RULE_DESC_INPUT)).toHaveLength(1);
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

        it("should cancel button emit the event which will discard the form", async () => {
            const wrapper = getWrapper();
            await waitFor(() => {
                clickFoundElement(wrapper, selectors.CANCEL_BUTTON);
            });
            expect(emitMock).toHaveBeenCalledWith("ruleView.unchanged", {
                id: "1",
            });
            expect(emitMock).toHaveBeenCalledWith("ruleView.close", {
                id: "1",
            });
        });

        afterEach(() => {
            emitMock.mockReset();
        });
    });
});
