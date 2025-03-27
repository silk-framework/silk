import React from "react";
import { mount, shallow } from "enzyme";
import * as Store from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/store";
import ObjectRule from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/containers/MappingRule/ObjectRule/ObjectRule";
import ObjectMappingRuleForm from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/containers/MappingRule/ObjectRule/ObjectRuleForm";
import ObjectEntityRelation from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/ObjectMapping/ObjectEntityRelation";
import TargetProperty from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/TargetProperty";
import ObjectTypeRules from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/ObjectMapping/ObjectTypeRules";
import ObjectSourcePath from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/ObjectMapping/ObjectSourcePath";
import EditButton from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/elements/buttons/EditButton";
import CopyButton from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/elements/buttons/CopyButton";
import CloneButton from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/elements/buttons/CloneButton";
import DeleteButton from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/elements/buttons/DeleteButton";
import MetadataLabel from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/Metadata/MetadataLabel";
import MetadataDesc from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/Metadata/MetadataDesc";
import ExampleTarget from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/ExampleTarget";
import {
    MAPPING_ROOT_RULE_ID
} from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/HierarchicalMapping";

const handleCopyFn = jest.fn();
const handleCloneFn = jest.fn();
const onClickedRemoveFn = jest.fn();

const props = {
    parentId: "",
    parent: { id: "", type: false },
    edit: false,
    type: "root",
    viewActions: {
        savedChanges: jest.fn(),
    },
    ruleData: {
        metadata: {
            label: "label",
            description: "description",
        },
        parentId: "",
        breadcrumbs: [],
        id: MAPPING_ROOT_RULE_ID,
        sourcePath: ["..", "/test"],
        rules: {
            uriRule: null,
            typeRules: [
                {
                    typeUri: "test",
                },
            ],
            propertyRules: [],
        },
        mappingTarget: {
            uri: "uri",
        },
        type: "root",
    },
    handleCopy: handleCopyFn,
    handleClone: handleCloneFn,
    onClickedRemove: onClickedRemoveFn,
};

const getWrapper = (renderer = shallow, arg = props) => renderer(<ObjectRule {...arg} />);

describe("ObjectMappingRule Component", () => {
    describe("on component mounted, ", () => {
        let wrapper;

        beforeEach(() => {
            wrapper = getWrapper(shallow);
        });

        it("should ObjectMappingRuleForm rendered, when `props.edit` is true", () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                edit: {},
            });
            expect(wrapper.find(ObjectMappingRuleForm)).toHaveLength(1);
        });

        it("should render ObjectTargetProperty components, when `props.type` is NOT `root`", () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                type: "complex",
            });
            expect(wrapper.find(TargetProperty)).toHaveLength(1);
        });

        it("should render ObjectEntityRelation components, when `props.type` is NOT `root`", () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                type: "complex",
            });
            expect(wrapper.find(ObjectEntityRelation)).toHaveLength(1);
        });

        it("should ObjectTypeRules component rendered, when `props.typeRules` first object have `uri` property", () => {
            expect(wrapper.find(ObjectTypeRules)).toHaveLength(1);
        });

        it("should ObjectSourcePath component rendered, when `props.type` is Object and sourcePath presented in `ruleData`,", () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                type: "object",
            });
            expect(wrapper.find(ObjectSourcePath)).toHaveLength(1);
        });

        it("should ExampleTarget component rendered, when `props.rules.uriRule.id` is presented", () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                ruleData: {
                    ...props.ruleData,
                    rules: {
                        uriRule: {
                            id: "id",
                        },
                    },
                },
            });
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
            expect(handleCopyFn).toBeCalledWith(props.ruleData.id, props.ruleData.type);
        });

        it("should CloneButton button call `handleClone` function from props, with right arguments", () => {
            const wrapper = getWrapper(mount, {
                ...props,
                ruleData: {
                    ...props.ruleData,
                    type: "object",
                },
            });
            wrapper.find(CloneButton).simulate("click");
            expect(handleCloneFn).toBeCalledWith(props.ruleData.id, "object", props.ruleData.parentId);
        });

        it("should DeleteButton button call `onClickedRemove` function from props, with right arguments", () => {
            const wrapper = getWrapper(mount, {
                ...props,
                type: "object",
            });
            wrapper.find(DeleteButton).simulate("click");
            expect(onClickedRemoveFn).toBeCalledWith({
                id: "root",
                uri: "uri",
                type: "root",
                parent: "",
            });
        });
    });
});
