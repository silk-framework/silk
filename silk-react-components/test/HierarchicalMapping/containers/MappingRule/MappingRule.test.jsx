import React from "react";
import { mount, render, shallow } from 'enzyme';
import MappingRule from '../../../../src/HierarchicalMapping/containers/MappingRule/MappingRule';
import {
    Button,
    ConfirmationDialog,
    ContextMenu,
    DismissiveButton,
    DisruptiveButton,
    MenuItem,
    Spinner,
} from '@eccenca/gui-elements';
import ObjectRule from '../../../../src/HierarchicalMapping/containers/MappingRule/ObjectRule/ObjectRule';
import ValueMappingRule from '../../../../src/HierarchicalMapping/containers/MappingRule/ValueRule/ValueRule';
import NavigateButton from '../../../../src/HierarchicalMapping/elements/buttons/NavigateButton';
import ExpandButton from '../../../../src/HierarchicalMapping/elements/buttons/ExpandButton';

const onRuleIdChangeFn = jest.fn();
const onExpandFn = jest.fn();
const props = {
    count: 8,
    expanded: false,
    handleClone: jest.fn(),
    handleCopy: jest.fn(),
    id: "birthdate",
    isPasted: false,
    mappingTarget: {
        uri: "<urn:ruleProperty:birthdate>",
        valueType: {},
        isBackwardProperty: false,
        isAttribute: false
    },
    metadata: {
        label: ""
    },
    onAskDiscardChanges: jest.fn(),
    onClickedRemove: jest.fn(),
    onExpand: onExpandFn,
    onRuleIdChange: onRuleIdChangeFn,
    operator: {
        type: "transformInput",
        id: "normalize",
        function: "DateTypeParser",
        inputs: Array(1),
        parameters: {}
    },
    parentId: "12",
    pos: 4,
    provided: {
        placeholder: null,
        dragHandleProps: jest.fn(),
        draggableStyle: {},
        innerRef: jest.fn()
    },
    scrollElementIntoView: jest.fn(),
    scrollIntoView: jest.fn(),
    snapshot: {isDragging: false},
    sourcePaths: ["birthdate"],
    type: "complex"
};

const selectors = {
    ROW_CLICK: 'div[data-test-id="row-click"]'
};

const getWrapper = (renderer = shallow, args = props) => renderer(
    <MappingRule {...args} />
);

describe("MappingRule Component", () => {
    describe("on component mounted, ", () => {
        let wrapper;
        beforeEach(() => {
            wrapper = getWrapper(shallow);
            wrapper.setState({
                loading: false
            })
        });
        
        it('should loading appear when data not loaded', () => {
            wrapper.setState({
                loading: true
            });
            expect(wrapper.find(Spinner)).toHaveLength(1)
        });
        
        it('should rendered NavigateButton, when type is object', () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                type: 'object'
            });
            expect(wrapper.find(NavigateButton)).toHaveLength(1)
        });
    
        it('should rendered ExpandButton, when type is NOT object', () => {
            expect(wrapper.find(ExpandButton)).toHaveLength(1)
        });
        
        it('should render ObjectMappingRule component, when rule is expanded and rule is object', () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                type: 'root',
                expanded: true
            });
            expect(wrapper.find(ObjectRule)).toHaveLength(1);
        });
        
        it('should render ValueMappingRule component, when rule is expanded and rule is NOT object', () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                expanded: true
            });
            expect(wrapper.find(ValueMappingRule)).toHaveLength(1);
        });
        
        it('should render reorder context menu, when rule is not expanded', () => {
            expect(wrapper.find(ContextMenu)).toHaveLength(1);
        });
        
        afterEach(() => {
            wrapper.unmount();
        });
    });
    
    describe("on user interaction, ", () => {
        let wrapper;
        beforeEach(() => {
            wrapper = getWrapper(shallow);
            wrapper.setState({
                loading: false
            })
        });
        
        it('should on row click navigate to the rule page, when rule type is object', () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                type: 'object'
            });
            wrapper.find(selectors.ROW_CLICK).simulate('click', {
                stopPropagation: jest.fn()
            });
            expect(onRuleIdChangeFn).toBeCalledWith({
                newRuleId: props.id,
                parentId: props.parentId
            });
        });
    
        it('should on row click expand the rule, when rule type is NOT object', () => {
            wrapper.find(selectors.ROW_CLICK).simulate('click', {
                stopPropagation: jest.fn()
            });
            expect(onExpandFn).toBeCalled();
        });
        
        afterEach(() => {
            wrapper.unmount();
        })
        
    });
});
