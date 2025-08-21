import React from "react";
import ObjectRule from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/containers/MappingRule/ObjectRule/ObjectRule";

import { MAPPING_ROOT_RULE_ID } from "../../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/HierarchicalMapping";

import { fireEvent, render } from "@testing-library/react";
import { byTestId, findAllDOMElements, findElement } from "../../../../integration/TestHelper";

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

const getWrapper = (renderer = render, arg = props) => renderer(<ObjectRule {...arg} />);

describe("ObjectMappingRule Component", () => {
    describe("on component mounted, ", () => {
        let wrapper;

        beforeEach(() => {
            wrapper = getWrapper(render);
        });

        it("should ObjectMappingRuleForm rendered, when `props.edit` is true", () => {
            const wrapper = getWrapper(render, {
                ...props,
                edit: {},
            });
            expect(findAllDOMElements(wrapper, ".ecc-silk-mapping__ruleseditor").length).toBeGreaterThan(0);
        });

        it("should render ObjectTargetProperty components, when `props.type` is NOT `root`", () => {
            const wrapper = getWrapper(render, {
                ...props,
                type: "complex",
            });
            findElement(wrapper, ".ecc-silk-mapping__rulesviewer__targetProperty");
        });

        it("should render ObjectEntityRelation components, when `props.type` is NOT `root`", () => {
            const wrapper = getWrapper(render, {
                ...props,
                type: "complex",
            });
            findElement(wrapper, ".mdl-radio-group");
        });

        it("should ObjectTypeRules component rendered, when `props.typeRules` first object have `uri` property", () => {
            const objectTypeRuleClass = ".ecc-silk-mapping__rulesviewer__targetEntityType"; //component -> ObjectTypeRules
            findElement(wrapper, objectTypeRuleClass);
        });

        it("should ObjectSourcePath component rendered, when `props.type` is Object and sourcePath presented in `ruleData`,", () => {
            const wrapper = getWrapper(render, {
                ...props,
                type: "object",
            });
            const objectSourcePathClass = ".ecc-silk-mapping__rulesviewer__sourcePath"; //component -> ObjectSourcePath
            findElement(wrapper, objectSourcePathClass);
        });

        it("should ExampleTarget component rendered, when `props.rules.uriRule.id` is presented", () => {
            const wrapper = getWrapper(render, {
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
            const exampleTargetClass = ".ecc-silk-mapping__rulesviewer__examples"; //component ->ExampleTarget
            findElement(wrapper, exampleTargetClass);
        });

        it("should MetadataLabel component rendered, when `props.metadata.label` is presented", () => {
            const metadataLabelClass = ".ecc-silk-mapping__rulesviewer__label";
            findElement(wrapper, metadataLabelClass);
        });

        it("should ExampleTarget component rendered, when `props.metadata.description` is presented", () => {
            const metadataDescClass = ".ecc-silk-mapping__rulesviewer__comment";
            expect(findAllDOMElements(wrapper, metadataDescClass).length).toBeGreaterThan(0);
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
            expect(handleCopyFn).toHaveBeenCalledWith(props.ruleData.id, props.ruleData.type);
        });

        it("should CloneButton button call `handleClone` function from props, with right arguments", () => {
            const wrapper = getWrapper(render, {
                ...props,
                ruleData: {
                    ...props.ruleData,
                    type: "object",
                },
            });
            fireEvent.click(findElement(wrapper, byTestId("mapping-rule-clone-btn")));
            expect(handleCloneFn).toHaveBeenCalledWith(props.ruleData.id, "object", props.ruleData.parentId);
        });

        it("should DeleteButton button call `onClickedRemove` function from props, with right arguments", () => {
            const wrapper = getWrapper(render, {
                ...props,
                type: "object",
            });
            fireEvent.click(findElement(wrapper, byTestId("mapping-rule-delete-btn")));
            expect(onClickedRemoveFn).toHaveBeenCalledWith({
                id: "root",
                uri: "uri",
                type: "root",
                parent: "",
                displayLabel: "label",
            });
        });
    });
});
