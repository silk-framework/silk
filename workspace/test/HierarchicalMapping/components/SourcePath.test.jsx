import React from "react";
import { SourcePath } from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/SourcePath";
import { render } from "@testing-library/react";
import { findAllDOMElements, findElement } from "../../integration/TestHelper";

const getWrapper = (renderer = render, args = {}) => renderer(<SourcePath {...args} />);

describe("SourcePath Component", () => {
    describe("on component mounted, ", () => {
        it("should render NotAvailable component, when `sourcePath` is NOT presented in `rule` prop", () => {
            const wrapper = getWrapper(render, {
                rule: {},
            });
            expect(findAllDOMElements(wrapper, "[class*='__notavailable']").length).toBeGreaterThan(0);
        });

        it("should render NotAvailable component, when `sourcePath` is presented in `rule` prop", () => {
            const wrapper = getWrapper(render, {
                rule: {
                    sourcePath: "text",
                },
            });
            expect(findElement(wrapper, "span").textContent).toEqual("text");
        });
    });
});
