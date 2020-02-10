import React from "react";
import { shallow } from 'enzyme';
import {
    ErrorCause,
    ErrorIssue,
    ErrorView
} from '../../../src/HierarchicalMapping/components/ErrorView';

const props = {
    title: 'text',
    detail: 'detail',
    cause: null,
    issues: null
};


const getWrapper = (renderer = shallow, args = props) => renderer(
    <ErrorView {...args} />
);


describe("ErrorView Component", () => {
    describe("on component mounted, ",() => {
        it("should render ErrorCause component, when `errorExpanded` and `props.cause` presented", () => {
            const wrapper = getWrapper(shallow, {
                cause: [{
                    title: '1',
                    detail: '1'
                }]
            });
            wrapper.setState({
                errorExpanded: true
            });
            expect(wrapper.find(ErrorCause)).toHaveLength(1);
        });
    
        it("should render ErrorIssue component, when `errorExpanded` and `props.issues` presented", () => {
            const wrapper = getWrapper(shallow, {
                issues: [{
                    title: '1',
                    detail: '1'
                }]
            });
            wrapper.setState({
                errorExpanded: true
            });
            expect(wrapper.find(ErrorIssue)).toHaveLength(1);
        });
        
    });
});
