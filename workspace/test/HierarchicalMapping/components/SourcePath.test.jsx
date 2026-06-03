import React from "react";
import { SourcePath } from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/SourcePath";
import { render } from "@testing-library/react";
import { checkForNotAvailableElement, findAllDOMElements, findElement } from "../../integration/TestHelper";

const getWrapper = (args = {}) => render(<SourcePath {...args} />);

describe("SourcePath Component", () => {
    describe("on component mounted, ", () => {
        it("should render NotAvailable component, when `sourcePath` is NOT presented in `rule` prop", () => {
            const wrapper = getWrapper({
                rule: {},
            });
            checkForNotAvailableElement(wrapper);
        });

        it("should render NotAvailable component, when `sourcePath` is presented in `rule` prop", () => {
            const wrapper = getWrapper({
                rule: {
                    sourcePath: "text",
                },
            });
            expect(findElement(wrapper, "span").textContent).toEqual("text");
        });
    });
});
