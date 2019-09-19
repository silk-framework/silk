import React from "react";
import { shallow } from 'enzyme';
import ExampleView from '../../../../src/HierarchicalMapping/Components/MappingRule/ExampleView';
import { ErrorView } from '../../../../src/HierarchicalMapping/Components/MappingRule/ErrorView';

const props = {
    id: 'id',
    rawRule: {},
    ruleType: {},
};

jest.mock('../../../../src/HierarchicalMapping/store', () => ({
    __esModule: true,
    childExampleAsync: () => ({
        subscribe: jest.fn()
    }),
    ruleExampleAsync: () => ({
        subscribe: jest.fn()
    })
}));


const getWrapper = (renderer = shallow, args = props) => renderer(
    <ExampleView {...args} />
);

describe("ExampleView Component", () => {
    describe("on component mounted, ",() => {
        let wrapper;
        beforeEach(() => {
            wrapper = getWrapper(shallow);
        });
        
        it("should render ErrorView component, when error presented", () => {
            wrapper.setState({
                error: {}
            });
            expect(wrapper.find(ErrorView)).toHaveLength(1);
        });
    
        it("should render empty div, when `state.example` is undefined", () => {
            wrapper.setState({
                example: undefined
            });
            expect(wrapper.find('div')).toHaveLength(1);
        });
    
        it("should return false, when `example.sourcePaths` size equal to `example.results`", () => {
            wrapper.setState({
                example: {
                    sourcePaths: [],
                    results: []
                }
            });
            expect(wrapper.get(0)).toBeFalsy();
        });
    
        afterEach(() => {
            wrapper.unmount();
        })
        
    });
});
