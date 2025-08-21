import React from "react";
import {
    ErrorView,
    ErrorCause,
    ErrorIssue,
} from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/ErrorView";
import { fireEvent, render } from "@testing-library/react";
import { findElement } from "../../integration/TestHelper";

const props = {
    title: "text",
    detail: "detail",
    cause: null,
    issues: null,
};

const getWrapper = (renderer = render, args = props) => renderer(<ErrorView {...args} />);

const errorClass = ".ecc-hierarchical-mapping-error-list";
describe("ErrorView Component", () => {
    describe("on component mounted, ", () => {
        it("should render ErrorCause component, when `errorExpanded` and `props.cause` presented", () => {
            const wrapper = getWrapper(render, {
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
            fireEvent.click(wrapper.container.querySelector("button"));
            expect(findElement(wrapper, errorClass)).toBeInTheDocument();
        });

        it("should render ErrorIssue component, when `errorExpanded` and `props.issues` presented", () => {
            const wrapper = getWrapper(render, {
                issues: [
                    {
                        title: "1",
                        detail: "1",
                    },
                ],
            });
            expect(wrapper.container.querySelector(errorClass)).not.toBeInTheDocument();
            fireEvent.click(wrapper.container.querySelector("button"));
            expect(findElement(wrapper, errorClass)).toBeInTheDocument();
        });
    });
});
