import React from "react";
import ExampleView from "../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/containers/MappingRule/ExampleView";
import { render, waitFor } from "@testing-library/react";
import { findAllDOMElements, findElement } from "../../../integration/TestHelper";

const props = {
    id: "id",
    rawRule: {},
    ruleType: {},
    updateDelay: 0,
};

let mockExampleResponse = undefined;
let mockError = undefined;
jest.mock("../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/store", () => ({
    __esModule: true,
    childExampleAsync: () => ({
        subscribe: (successFn, errorFn) => {
            if (mockError) {
                errorFn(mockError);
            } else {
                successFn({ example: mockExampleResponse });
            }
        },
    }),
    ruleExampleAsync: () => ({
        subscribe: jest.fn(),
    }),
}));

const getWrapper = (args = props) => render(<ExampleView {...args} />);

const setMockExampleResponse = (example) => {
    mockExampleResponse = example;
    mockError = undefined;
};

const setMockError = (error) => {
    mockExampleResponse = undefined;
    mockError = error;
};

describe("ExampleView Component", () => {
    describe("on component mounted, ", () => {
        it("should render ErrorView component, when error presented", async () => {
            setMockError({});
            const wrapper = getWrapper();
            await waitFor(() => {
                expect(findElement(wrapper, ".mdl-alert--narrowed")).toBeInTheDocument();
            });
        });

        it("should render empty div, when `state.example` is undefined", () => {
            setMockExampleResponse(undefined);
            const wrapper = getWrapper();
            expect(findAllDOMElements(wrapper, "div")).toHaveLength(1);
        });

        it("should return false, when `example.sourcePaths` size equal to `example.results`", () => {
            setMockExampleResponse({
                sourcePaths: [],
                results: [],
                status: {
                    id: "success",
                },
            });
            const wrapper = getWrapper();
            // Match the translation key
            expect(wrapper.container.textContent).toMatch("Preview has returned no results.");
        });
    });
});
