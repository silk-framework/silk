import React from "react";
import { mount, shallow } from 'enzyme';
import TargetProperty from '../../../../../src/HierarchicalMapping/components/TargetProperty';
import ObjectSourcePath from '../../../../../src/HierarchicalMapping/components/ObjectMapping/ObjectSourcePath';
import EditButton from '../../../../../src/HierarchicalMapping/elements/buttons/EditButton';
import CopyButton from '../../../../../src/HierarchicalMapping/elements/buttons/CopyButton';
import CloneButton from '../../../../../src/HierarchicalMapping/elements/buttons/CloneButton';
import DeleteButton from '../../../../../src/HierarchicalMapping/elements/buttons/DeleteButton';
import ValueRule from '../../../../../src/HierarchicalMapping/containers/MappingRule/ValueRule/ValueRule';
import ValueRuleForm from '../../../../../src/HierarchicalMapping/containers/MappingRule/ValueRule/ValueRuleForm';
import ValueNodeType from '../../../../../src/HierarchicalMapping/components/ValueMapping/ValueNodeType';
import ValueSourcePaths from '../../../../../src/HierarchicalMapping/components/ValueMapping/ValueSourcePaths';
import MetadataLabel from '../../../../../src/HierarchicalMapping/components/Metadata/MetadataLabel';
import ExampleTarget from '../../../../../src/HierarchicalMapping/components/ExampleTarget';
import MetadataDesc from '../../../../../src/HierarchicalMapping/components/Metadata/MetadataDesc';

const handleCopyFn = jest.fn();
const handleCloneFn = jest.fn();
const onClickedRemoveFn = jest.fn();

const props = {
    id: '1',
    parentId: '',
    parent: undefined,
    edit: false,
    type: 'direct',
    sourcePath: [],
    sourcePaths: [],
    mappingTarget: {
        uri: 'uri',
        valueType: {
            nodeType: 'div'
        }
    },
    metadata: {
        label: 'label',
        description: 'description',
    },
    handleCopy: handleCopyFn,
    handleClone: handleCloneFn,
    onClickedRemove: onClickedRemoveFn,
};

const getWrapper = (renderer = shallow, arg = props) => renderer(
    <ValueRule {...arg} />
);

describe("ValueRule Component", () => {
    describe("on component mounted, ", () => {
        let wrapper;
        
        beforeEach(() => {
            wrapper = getWrapper(shallow);
        });
        
        it("should ValueRuleForm rendered, when `props.edit` is true", () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                edit: {}
            });
            expect(wrapper.find(ValueRuleForm)).toHaveLength(1);
        });
        
        it('should render TargetProperty component, when `mappingTarget.uri` is presented', () => {
            expect(wrapper.find(TargetProperty)).toHaveLength(1);
        });
        
        it('should render ValueNodeType component, when `nodeType` is presented', () => {
            const wrapper = getWrapper(shallow);
            expect(wrapper.find(ValueNodeType)).toHaveLength(1);
        });
        
        it('should ObjectSourcePath component rendered, when `props.type` equal to `direct` and sourcePath presented', () => {
            expect(wrapper.find(ObjectSourcePath)).toHaveLength(1);
        });
    
        it('should ValueSourcePaths component rendered, when `props.type` is NOT equal to `direct` and sourcePaths presented', () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                type: 'object'
            });
            expect(wrapper.find(ValueSourcePaths)).toHaveLength(1);
        });
        
        it('should ExampleTarget component rendered, when `props.id` is presented', () => {
            expect(wrapper.find(ExampleTarget)).toHaveLength(1);
        });
    
        it('should MetadataLabel component rendered, when `props.metadata.label` is presented', () => {
            expect(wrapper.find(MetadataLabel)).toHaveLength(1);
        });
    
        it('should ExampleTarget component rendered, when `props.metadata.description` is presented', () => {
            expect(wrapper.find(MetadataDesc)).toHaveLength(1);
        });
        
        afterEach(() => {
            wrapper.unmount();
        });
    });
    
    describe("on user interaction", () => {
        it("should EDIT button toggle the `edit` property in state", () => {
            const wrapper = getWrapper(mount);
            wrapper.find(EditButton).simulate('click');
            expect(wrapper.state().edit).toBe(true);
        });
        
        it("should CopyButton button call `handleCopy` function from props, with right arguments", () => {
            const wrapper = getWrapper(mount);
            wrapper.find(CopyButton).simulate('click');
            expect(handleCopyFn).toBeCalledWith(props.id, props.type);
        });
        
        it("should CloneButton button call `handleClone` function from props, with right arguments", () => {
            const wrapper = getWrapper(mount);
            wrapper.find(CloneButton).simulate('click');
            expect(handleCloneFn).toBeCalledWith(props.id, 'direct');
        });
        
        it("should DeleteButton button call `onClickedRemove` function from props, with right arguments", () => {
            const wrapper = getWrapper(mount, {
                ...props,
                type: 'object'
            });
            wrapper.find(DeleteButton).simulate('click');
            expect(onClickedRemoveFn).toBeCalledWith({
                id: "1",
                uri: 'uri',
                type: 'object',
                parent: ''
            });
        });
        
    });
});
