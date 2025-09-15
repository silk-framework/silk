import React from "react";
import { ParentElement } from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/ParentElement";
import { render } from "@testing-library/react";
import { findElement } from "../../integration/TestHelper";

const getWrapper = (args = props) => render(<ParentElement {...args} />);

describe("ParentElement Component", () => {
    describe("on component mounted, ", () => {
        it("should render ThingName component, when `type` is presented in `parent` prop", () => {
            const wrapper = getWrapper({
                parent: {
                    type: "something",
                },
            });
            findElement(wrapper, "span:first-child");
        });

        it("should render html span element, when `type` is NOT presented in `parent` prop", () => {
            const wrapper = getWrapper({
                parent: {},
            });

            expect(findElement(wrapper, "span:first-child").textContent).toEqual("parent element");
        });
    });
});
