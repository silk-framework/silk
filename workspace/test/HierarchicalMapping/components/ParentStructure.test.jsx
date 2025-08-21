import React from "react";
import { ParentStructure } from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/ParentStructure";
import { render } from "@testing-library/react";
import { findElement } from "../../integration/TestHelper";

const getWrapper = (renderer = render, args = props) => renderer(<ParentStructure {...args} />);

describe("ParentStructure Component", () => {
    describe("on component mounted, ", () => {
        it("should render ThingName component, when `property` is presented in `parent` prop", () => {
            const wrapper = getWrapper(render, {
                parent: {
                    property: "something",
                },
            });
            findElement(wrapper, "span:first-child");
        });

        it("should render ParentElement component, when `property` is NOT presented in `parent` prop", () => {
            const wrapper = getWrapper(render, {
                parent: {},
            });
            findElement(wrapper, "span:first-child");
        });
    });
});
