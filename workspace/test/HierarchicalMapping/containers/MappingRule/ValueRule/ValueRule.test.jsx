import React from "react";
import ValueRule from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/containers/MappingRule/ValueRule/ValueRule";

import { fireEvent, render, waitFor } from "@testing-library/react";
import { byTestId, findAllDOMElements, findElement } from "../../../../integration/TestHelper";

const handleCopyFn = jest.fn();
const handleCloneFn = jest.fn();
const onClickedRemoveFn = jest.fn();

const props = {
    id: "1",
    parentId: "",
    parent: undefined,
    edit: false,
    type: "direct",
    sourcePath: [],
    sourcePaths: ["fields/customfield_12408", "fields/summary"],
    mappingTarget: {
        uri: "uri",
        valueType: {
            nodeType: "div",
        },
    },
    viewActions: {
        savedChanges: jest.fn(),
    },
    metadata: {
        label: "label",
        description: "description",
    },
    handleCopy: handleCopyFn,
    handleClone: handleCloneFn,
    onClickedRemove: onClickedRemoveFn,
};

const getWrapper = (renderer = render, arg = props) => renderer(<ValueRule {...arg} />);

describe("ValueRule Component", () => {
    describe("on component mounted, ", () => {
        let wrapper;

        beforeEach(() => {
            wrapper = getWrapper(render);
        });

        it("should ValueRuleForm rendered, when `props.edit` is true", async () => {
            await waitFor(() => {
                const wrapper = getWrapper(render, {
                    ...props,
                    edit: true,
                });
                expect(findAllDOMElements(wrapper, ".ecc-silk-mapping__ruleseditor").length).toBeGreaterThan(0);
            });
        });

        it("should render TargetProperty component, when `mappingTarget.uri` is presented", () => {
            findElement(wrapper, ".ecc-silk-mapping__rulesviewer__targetProperty");
        });

        it("should render ValueNodeType component, when `nodeType` is presented", () => {
            const wrapper = getWrapper(render);
            findElement(wrapper, ".ecc-silk-mapping__rulesviewer__propertyType");
        });

        it("should ObjectSourcePath component rendered, when `props.type` equal to `direct` and sourcePath presented", () => {
            const wrapper = getWrapper(render, { ...props, sourcePath: "path", sourcePaths: undefined });
            const objectSourcePathClass = ".ecc-silk-mapping__rulesviewer__sourcePath"; //component -> ObjectSourcePath
            findElement(wrapper, objectSourcePathClass);
        });

        it("should ValueSourcePaths component rendered, when `props.type` is NOT equal to `direct` and sourcePaths presented", () => {
            const wrapper = getWrapper(render, {
                ...props,
                type: "object",
            });
            const valueSourcePaths = findElement(wrapper, ".ecc-silk-mapping__rulesviewer__sourcePath");
            expect(valueSourcePaths.innerHTML).toContain("Formula uses 2 value paths");
        });

        it("should ExampleTarget component rendered, when `props.id` is presented", () => {
            findElement(wrapper, ".ecc-silk-mapping__rulesviewer__examples");
        });

        it("should MetadataLabel component rendered, when `props.metadata.label` is presented", () => {
            findElement(wrapper, ".ecc-silk-mapping__rulesviewer__label");
        });

        it("should ExampleTarget component rendered, when `props.metadata.description` is presented", () => {
            findElement(wrapper, ".ecc-silk-mapping__rulesviewer__comment");
        });

        afterEach(() => {
            wrapper.unmount();
        });
    });

    describe("on user interaction", () => {
        it("should EDIT button toggle the `edit` property in state", () => {
            const wrapper = getWrapper(render);
            fireEvent.click(findElement(wrapper, byTestId("mapping-rule-edit-btn")));
        });

        it("should CopyButton button call `handleCopy` function from props, with right arguments", () => {
            const wrapper = getWrapper(render);
            fireEvent.click(findElement(wrapper, byTestId("mapping-rule-copy-btn")));
            expect(handleCopyFn).toHaveBeenCalledWith(props.id, props.type);
        });

        it("should CloneButton button call `handleClone` function from props, with right arguments", () => {
            const wrapper = getWrapper(render);
            fireEvent.click(findElement(wrapper, byTestId("mapping-rule-clone-btn")));
            expect(handleCloneFn).toHaveBeenCalledWith(props.id, "direct");
        });

        it("should DeleteButton button call `onClickedRemove` function from props, with right arguments", () => {
            const wrapper = getWrapper(render, {
                ...props,
                type: "object",
            });
            fireEvent.click(findElement(wrapper, byTestId("mapping-rule-delete-btn")));
            expect(onClickedRemoveFn).toHaveBeenCalledWith({
                id: "1",
                uri: "uri",
                type: "object",
                parent: "",
            });
        });
    });
});
