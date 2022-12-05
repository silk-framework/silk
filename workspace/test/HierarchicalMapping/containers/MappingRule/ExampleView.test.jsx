import { waitFor } from "@testing-library/react";
import { mount } from "enzyme";
import React from "react";

import { ErrorView } from "../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/ErrorView";
import ExampleView from "../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/containers/MappingRule/ExampleView";
import { findAll } from "../../utils/TestHelpers";

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

const getWrapper = (args = props) => mount(<ExampleView {...args} />);

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
                expect(wrapper.find(ErrorView)).toHaveLength(1);
            });
        });

        it("should render empty div, when `state.example` is undefined", () => {
            setMockExampleResponse(undefined);
            const wrapper = getWrapper();
            expect(findAll(wrapper, "div")).toHaveLength(1);
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
            expect(wrapper.text()).toMatch("Preview has returned no results.");
        });
    });
});
