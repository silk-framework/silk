import React from "react";
import { mount, shallow } from 'enzyme';
import * as Store from '../../../../src/HierarchicalMapping/store';
import ObjectMappingRule from '../../../../src/HierarchicalMapping/Containers/MappingRule/ObjectMappingRule/ObjectMappingRule';
import ObjectMappingRuleForm from '../../../../src/HierarchicalMapping/Containers/MappingRule/ObjectMappingRule/ObjectMappingRuleForm';
import ObjectEntityRelation
    from '../../../../src/HierarchicalMapping/Containers/MappingRule/ObjectMappingRule/views/ObjectEntityRelation';
import ObjectTargetProperty
    from '../../../../src/HierarchicalMapping/Containers/MappingRule/ObjectMappingRule/views/ObjectTargetProperty';
import ObjectTypeRules
    from '../../../../src/HierarchicalMapping/Containers/MappingRule/ObjectMappingRule/views/ObjectTypeRules';
import ObjectSourcePath
    from '../../../../src/HierarchicalMapping/Containers/MappingRule/ObjectMappingRule/views/ObjectSourcePath';
import EditButton
    from '../../../../src/HierarchicalMapping/Containers/MappingRule/ObjectMappingRule/buttons/EditButton';
import CopyButton
    from '../../../../src/HierarchicalMapping/Containers/MappingRule/ObjectMappingRule/buttons/CopyButton';
import CloneButton
    from '../../../../src/HierarchicalMapping/Containers/MappingRule/ObjectMappingRule/buttons/CloneButton';
import DeleteButton
    from '../../../../src/HierarchicalMapping/Containers/MappingRule/ObjectMappingRule/buttons/DeleteButton';

const handleCopyFn = jest.fn();
const handleCloneFn = jest.fn();
const onClickedRemoveFn = jest.fn();

const props = {
    parentId: '',
    parent: undefined,
    edit: false,
    type: 'root',
    ruleData: {
        parentId: '',
        breadcrumbs: [],
        id: "root",
        metadata: {
            label: ""
        },
        sourcePath: [
            '..', '/test'
        ],
        rules: {
            uriRule: null,
            typeRules: [
                {
                    uri: 'test'
                }
            ],
            propertyRules: []
        },
        mappingTarget: {
            uri: 'uri'
        },
        type: "root"
    },
    handleCopy: handleCopyFn,
    handleClone: handleCloneFn,
    onClickedRemove: onClickedRemoveFn,
};

const getWrapper = (renderer = shallow, arg = props) => renderer(
    <ObjectMappingRule {...arg} />
);

describe("ObjectMappingRule Component", () => {
    describe("on component mounted, ", () => {
        let wrapper;
        
        beforeEach(() => {
            wrapper = getWrapper(shallow);
        });
        
        it("should ObjectMappingRuleForm rendered, when `props.edit` is true", () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                edit: {}
            });
            expect(wrapper.find(ObjectMappingRuleForm)).toHaveLength(1);
        });
        
        it('should render ObjectTargetProperty components, when `props.type` is NOT `root`', () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                type: 'complex'
            });
           expect(wrapper.find(ObjectTargetProperty)).toHaveLength(1);
        });
    
        it('should render ObjectEntityRelation components, when `props.type` is NOT `root`', () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                type: 'complex'
            });
            expect(wrapper.find(ObjectEntityRelation)).toHaveLength(1);
        });
        
        it('should ObjectTypeRules component rendered, when `props.typeRules` first object have `uri` property', () => {
            expect(wrapper.find(ObjectTypeRules)).toHaveLength(1);
        });
        
        it('should ObjectSourcePath component rendered, when `props.type` is Object and sourcePath presented in `ruleData`,', () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                type: 'object'
            });
            expect(wrapper.find(ObjectSourcePath)).toHaveLength(1);
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
            expect(handleCopyFn).toBeCalledWith(props.ruleData.id, props.ruleData.type);
        });
    
        it("should CopyButton button call `handleClone` function from props, with right arguments", () => {
            const wrapper = getWrapper(mount, {
                ...props,
                ruleData: {
                    ...props.ruleData,
                    type: 'object'
                }
            });
            wrapper.find(CloneButton).simulate('click');
            expect(handleCloneFn).toBeCalledWith(props.ruleData.id, 'object', props.ruleData.parentId);
        });
    
        it("should DeleteButton button call `onClickedRemove` function from props, with right arguments", () => {
            const wrapper = getWrapper(mount, {
                ...props,
                type: 'object'
            });
            wrapper.find(DeleteButton).simulate('click');
            expect(onClickedRemoveFn).toBeCalledWith({
                id: "root",
                uri: 'uri',
                type: 'root',
                parent: ''
            });
        });
        
    });
});
