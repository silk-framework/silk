import React from "react";
import { shallow } from 'enzyme';
import { ThingName } from '../../../src/HierarchicalMapping/components/ThingName';
import { ParentStructure } from '../../../src/HierarchicalMapping/components/ParentStructure';
import { ParentElement } from '../../../src/HierarchicalMapping/components/ParentElement';



const getWrapper = (renderer = shallow, args = props) => renderer(
    <ParentStructure {...args} />
);


describe("ParentStructure Component", () => {
    describe("on component mounted, ",() => {
        it("should render ThingName component, when `property` is presented in `parent` prop", () => {
            const wrapper = getWrapper(shallow, {
                parent: {
                    property: 'something'
                }
            });
            expect(wrapper.find(ThingName)).toHaveLength(1);
        });
    
        it("should render ParentElement component, when `property` is NOT presented in `parent` prop", () => {
            const wrapper = getWrapper(shallow, {
                parent: {}
            });
            expect(wrapper.find(ParentElement)).toHaveLength(1)
        });
    });
});
