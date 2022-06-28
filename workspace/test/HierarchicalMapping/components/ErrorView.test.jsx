import React from "react";
import {mount, shallow} from 'enzyme';
import {
    ErrorCause,
    ErrorIssue,
    ErrorView
} from '../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/ErrorView';
import {clickElement, logPageHtml, logWrapperHtml} from "../utils/TestHelpers";

const props = {
    title: 'text',
    detail: 'detail',
    cause: null,
    issues: null
};


const getWrapper = (renderer = mount, args = props) => renderer(
    <ErrorView {...args} />
);


describe("ErrorView Component", () => {
    describe("on component mounted, ",() => {
        it("should render ErrorCause component, when `errorExpanded` and `props.cause` presented", () => {
            const wrapper = getWrapper(mount, {
                title: 'error title',
                detail: 'Error detail',
                cause: [{
                    title: '1',
                    detail: '1'
                }]
            });
            expect(wrapper.find(ErrorCause)).toHaveLength(0);
            clickElement(wrapper, "button")
            expect(wrapper.find(ErrorCause)).toHaveLength(1);
        });

        it("should render ErrorIssue component, when `errorExpanded` and `props.issues` presented", () => {
            const wrapper = getWrapper(mount, {
                issues: [{
                    title: '1',
                    detail: '1'
                }]
            });
            expect(wrapper.find(ErrorIssue)).toHaveLength(0);
            clickElement(wrapper, "button")
            expect(wrapper.find(ErrorIssue)).toHaveLength(1);
        });

    });
});
