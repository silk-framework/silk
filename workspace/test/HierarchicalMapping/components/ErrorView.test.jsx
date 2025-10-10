import React from "react";
import {
    ErrorView,
    ErrorCause,
    ErrorIssue,
} from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/ErrorView";
import { render } from "@testing-library/react";
import { clickFoundElement, findElement } from "../../integration/TestHelper";

const props = {
    title: "text",
    detail: "detail",
    cause: null,
    issues: null,
};

const getWrapper = (args = props) => render(<ErrorView {...args} />);

const errorClass = ".ecc-hierarchical-mapping-error-list";
describe("ErrorView Component", () => {
    describe("on component mounted, ", () => {
        it("should render ErrorCause component, when `errorExpanded` and `props.cause` presented", () => {
            const wrapper = getWrapper({
                title: "error title",
                detail: "Error detail",
                cause: [
                    {
                        title: "1",
                        detail: "1",
                    },
                ],
            });
            expect(wrapper.container.querySelector(errorClass)).not.toBeInTheDocument();
            clickFoundElement(wrapper, "button");
            expect(findElement(wrapper, errorClass)).toBeInTheDocument();
        });

        it("should render ErrorIssue component, when `errorExpanded` and `props.issues` presented", () => {
            const wrapper = getWrapper({
                issues: [
                    {
                        title: "1",
                        detail: "1",
                    },
                ],
            });
            expect(wrapper.container.querySelector(errorClass)).not.toBeInTheDocument();
            clickFoundElement(wrapper, "button");
            expect(findElement(wrapper, errorClass)).toBeInTheDocument();
        });
    });
});
