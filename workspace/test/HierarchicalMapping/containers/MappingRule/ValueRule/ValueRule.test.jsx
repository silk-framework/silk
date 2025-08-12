import React from "react";
import { mount, shallow } from "enzyme";
import TargetProperty from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/TargetProperty";
import ObjectSourcePath from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/ObjectMapping/ObjectSourcePath";
import EditButton from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/elements/buttons/EditButton";
import CopyButton from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/elements/buttons/CopyButton";
import CloneButton from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/elements/buttons/CloneButton";
import DeleteButton from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/elements/buttons/DeleteButton";
import ValueRule from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/containers/MappingRule/ValueRule/ValueRule";
import ValueRuleForm from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/containers/MappingRule/ValueRule/ValueRuleForm";
import ValueNodeType from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/ValueMapping/ValueNodeType";
import ValueSourcePaths from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/ValueMapping/ValueSourcePaths";
import MetadataLabel from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/Metadata/MetadataLabel";
import ExampleTarget from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/ExampleTarget";
import MetadataDesc from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/Metadata/MetadataDesc";

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

const getWrapper = (renderer = shallow, arg = props) => renderer(<ValueRule {...arg} />);

describe("ValueRule Component", () => {
    describe("on component mounted, ", () => {
        let wrapper;

        beforeEach(() => {
            wrapper = getWrapper(shallow);
        });

        it("should ValueRuleForm rendered, when `props.edit` is true", () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                edit: {},
            });
            expect(wrapper.find(ValueRuleForm)).toHaveLength(1);
        });

        it("should render TargetProperty component, when `mappingTarget.uri` is presented", () => {
            expect(wrapper.find(TargetProperty)).toHaveLength(1);
        });

        it("should render ValueNodeType component, when `nodeType` is presented", () => {
            const wrapper = getWrapper(shallow);
            expect(wrapper.find(ValueNodeType)).toHaveLength(1);
        });

        it("should ObjectSourcePath component rendered, when `props.type` equal to `direct` and sourcePath presented", () => {
            const wrapper = getWrapper(shallow, { ...props, sourcePath: "path", sourcePaths: undefined });
            expect(wrapper.find(ObjectSourcePath)).toHaveLength(1);
        });

        it("should ValueSourcePaths component rendered, when `props.type` is NOT equal to `direct` and sourcePaths presented", () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                type: "object",
            });
            const valueSourcePaths = wrapper.find(ValueSourcePaths);
            expect(valueSourcePaths).toHaveLength(1);
            expect(valueSourcePaths.html()).toContain("Formula uses 2 value paths");
        });

        it("should ExampleTarget component rendered, when `props.id` is presented", () => {
            expect(wrapper.find(ExampleTarget)).toHaveLength(1);
        });

        it("should MetadataLabel component rendered, when `props.metadata.label` is presented", () => {
            expect(wrapper.find(MetadataLabel)).toHaveLength(1);
        });

        it("should ExampleTarget component rendered, when `props.metadata.description` is presented", () => {
            expect(wrapper.find(MetadataDesc)).toHaveLength(1);
        });

        afterEach(() => {
            wrapper.unmount();
        });
    });

    describe("on user interaction", () => {
        it("should EDIT button toggle the `edit` property in state", () => {
            const wrapper = getWrapper(mount);
            wrapper.find(EditButton).simulate("click");
            expect(wrapper.state().edit).toBe(true);
        });

        it("should CopyButton button call `handleCopy` function from props, with right arguments", () => {
            const wrapper = getWrapper(mount);
            wrapper.find(CopyButton).simulate("click");
            expect(handleCopyFn).toHaveBeenCalledWith(props.id, props.type);
        });

        it("should CloneButton button call `handleClone` function from props, with right arguments", () => {
            const wrapper = getWrapper(mount);
            wrapper.find(CloneButton).simulate("click");
            expect(handleCloneFn).toHaveBeenCalledWith(props.id, "direct");
        });

        it("should DeleteButton button call `onClickedRemove` function from props, with right arguments", () => {
            const wrapper = getWrapper(mount, {
                ...props,
                type: "object",
            });
            wrapper.find(DeleteButton).simulate("click");
            expect(onClickedRemoveFn).toHaveBeenCalledWith({
                id: "1",
                uri: "uri",
                type: "object",
                parent: "",
            });
        });
    });
});
