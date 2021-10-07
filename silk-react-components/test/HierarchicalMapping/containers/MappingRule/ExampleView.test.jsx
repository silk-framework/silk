import React from "react";
import ExampleView from '../../../../src/HierarchicalMapping/containers/MappingRule/ExampleView';
import {ErrorView} from '../../../../src/HierarchicalMapping/components/ErrorView';
import {mount} from "enzyme";
import {findAll} from "../../utils/TestHelpers";

const props = {
    id: 'id',
    rawRule: {},
    ruleType: {},
};

let mockExampleResponse = undefined
let mockError = undefined
jest.mock('../../../../src/HierarchicalMapping/store', () => ({
    __esModule: true,
    childExampleAsync: () => ({
        subscribe: (successFn, errorFn) => {
            if(mockError) {
                errorFn(mockError)
            } else {
                successFn({example: mockExampleResponse})
            }
        }
    }),
    ruleExampleAsync: () => ({
        subscribe: jest.fn()
    })
}));


const getWrapper = (args = props) => mount(
    <ExampleView {...args} />
);

const setMockExampleResponse = (example) => {
    mockExampleResponse = example
    mockError = undefined
}

const setMockError = (error) => {
    mockExampleResponse = undefined
    mockError = error
}

describe("ExampleView Component", () => {
    describe("on component mounted, ",() => {
        it("should render ErrorView component, when error presented", () => {
            setMockError({})
            const wrapper = getWrapper()
            expect(wrapper.find(ErrorView)).toHaveLength(1);
        });
    
        it("should render empty div, when `state.example` is undefined", () => {
            setMockExampleResponse(undefined)
            const wrapper = getWrapper()
            expect(findAll(wrapper, 'div')).toHaveLength(1);
        });
    
        it("should return false, when `example.sourcePaths` size equal to `example.results`", () => {
            setMockExampleResponse({
                sourcePaths: [],
                results: []
            })
            const wrapper = getWrapper()
            expect(wrapper.getDOMNode()).toBeNull();
        });
    });
});
