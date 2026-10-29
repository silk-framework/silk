import React from "react";
import EventEmitter from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/utils/EventEmitter";
import { ValueRuleForm } from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/containers/MappingRule/ValueRule/ValueRuleForm";
import { fireEvent, render, waitFor } from "@testing-library/react";
import { clickFoundElement, findAllDOMElements, findElement } from "../../../../integration/TestHelper";

interface MockRule {
    sourcePath: string;
    mappingTarget: {
        uri: string;
        valueType: {
            nodeType: string;
            lang?: string;
            uri?: string;
        };
    };
}

interface AutoCompleteMockProps {
    className: string;
    entity: string;
    onChange: (value: { value: string }) => void;
    value?: string;
}

let mockRule: MockRule = {
    sourcePath: "sourcePath",
    mappingTarget: {
        uri: "targetProperty",
        valueType: {
            nodeType: "LanguageValueType",
            lang: "en",
        },
    },
};

jest.mock("../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/AutoComplete", () => {
    const React = jest.requireActual<typeof import("react")>("react");
    return {
        __esModule: true,
        default: ({ className, entity, onChange, value }: AutoCompleteMockProps) =>
            React.createElement("button", {
                className,
                "data-testid": `autocomplete-${entity}`,
                onClick: () => onChange({ value: value === "en" ? "de" : "en" }),
                type: "button",
            }),
    };
});

const props = {
    id: "1",
    parentId: "2",
    scrollIntoView: jest.fn(),
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
            get rule() {
                return mockRule;
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
            mockRule = {
                sourcePath: "sourcePath",
                mappingTarget: {
                    uri: "targetProperty",
                    valueType: {
                        nodeType: "LanguageValueType",
                        lang: "en",
                    },
                },
            };
        });

        it("should track changes to the language tag", async () => {
            const wrapper = getWrapper();
            const saveButton = await waitFor(() => findElement(wrapper, selectors.CONFIRM_BUTTON));
            const languageInput = findElement(wrapper, '[data-testid="autocomplete-langTag"]');

            expect(saveButton).toBeDisabled();
            fireEvent.click(languageInput);
            expect(saveButton).toBeEnabled();
            fireEvent.click(languageInput);
            expect(saveButton).toBeDisabled();
        });

        it("should track changes to the custom value type URI", async () => {
            mockRule = {
                sourcePath: "sourcePath",
                mappingTarget: {
                    uri: "targetProperty",
                    valueType: {
                        nodeType: "CustomValueType",
                        uri: "urn:initial",
                    },
                },
            };
            const wrapper = getWrapper();
            const saveButton = await waitFor(() => findElement(wrapper, selectors.CONFIRM_BUTTON));

            expect(saveButton).toBeDisabled();
            fireEvent.change(findElement(wrapper, "#uri"), { target: { value: "urn:changed" } });
            expect(saveButton).toBeEnabled();
            fireEvent.change(findElement(wrapper, "#uri"), { target: { value: "urn:initial" } });
            expect(saveButton).toBeDisabled();
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
