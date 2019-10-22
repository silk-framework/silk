import React from "react";
import { shallow } from 'enzyme';
import { ParentElement } from '../../../src/HierarchicalMapping/Components/ParentElement';
import { ThingName } from '../../../src/HierarchicalMapping/Components/ThingName';



const getWrapper = (renderer = shallow, args = props) => renderer(
    <ParentElement {...args} />
);


describe("ParentElement Component", () => {
    describe("on component mounted, ",() => {
        it("should render ThingName component, when `type` is presented in `parent` prop", () => {
            const wrapper = getWrapper(shallow, {
                parent: {
                    type: 'something'
                }
            });
            expect(wrapper.find(ThingName)).toHaveLength(1);
        });
    
        it("should render html span element, when `type` is NOT presented in `parent` prop", () => {
            const wrapper = getWrapper(shallow, {
                parent: {}
            });
            expect(wrapper.find('span').text()).toEqual('parent element')
        });
    });
});
