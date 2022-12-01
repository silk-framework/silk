import { shallow } from "enzyme";
import React from "react";

import { ParentElement } from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/ParentElement";
import { ParentStructure } from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/ParentStructure";
import { ThingName } from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/ThingName";

const getWrapper = (renderer = shallow, args = {}) => renderer(<ParentStructure {...args} />);

describe("ParentStructure Component", () => {
    describe("on component mounted, ", () => {
        it("should render ThingName component, when `property` is presented in `parent` prop", () => {
            const wrapper = getWrapper(shallow, {
                parent: {
                    property: "something",
                },
            });
            expect(wrapper.find(ThingName)).toHaveLength(1);
        });

        it("should render ParentElement component, when `property` is NOT presented in `parent` prop", () => {
            const wrapper = getWrapper(shallow, {
                parent: {},
            });
            expect(wrapper.find(ParentElement)).toHaveLength(1);
        });
    });
});
