import React from "react";
import { shallow } from 'enzyme';
import AutoComplete from '../../../src/HierarchicalMapping/Components/AutoComplete';
import { AutoCompleteBox } from '@eccenca/gui-elements';

const props = {
    input: 'text',
    ruleId: 'rule',
};

const autocompleteAsyncMock = jest.fn();
jest.doMock('../../../../../src/HierarchicalMapping/store', () => autocompleteAsyncMock);

const getWrapper = (renderer = shallow) => renderer(
    <AutoComplete {...props} />
);


describe("AutoComplete Component", () => {
    describe("on component mounted, ",() => {
        let wrapper;
        beforeEach(() => {
            wrapper = getWrapper(shallow);
        });
        
        it("should render AutoCompleteBox component", () => {
            expect(wrapper.find(AutoCompleteBox)).toHaveLength(1);
        });
        
        afterEach(() => {
            wrapper.unmount();
        });
    });
});
