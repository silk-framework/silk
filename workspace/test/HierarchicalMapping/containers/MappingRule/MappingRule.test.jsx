import React from "react";
import MappingRule from "../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/containers/MappingRule/MappingRule";
import { render } from "@testing-library/react";
import { byTestId, clickFoundElement, findAllDOMElements } from "../../../integration/TestHelper";

const onRuleIdChangeFn = jest.fn();
const onExpandFn = jest.fn();
const updateHistoryFn = jest.fn();
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
        isAttribute: false,
    },
    metadata: {
        label: "",
    },
    onAskDiscardChanges: jest.fn(),
    onClickedRemove: jest.fn(),
    onExpand: onExpandFn,
    onRuleIdChange: onRuleIdChangeFn,
    updateHistory: updateHistoryFn,
    operator: {
        type: "transformInput",
        id: "normalize",
        function: "DateTypeParser",
        inputs: Array(1),
        parameters: {},
    },
    parentId: "12",
    pos: 4,
    provided: {
        placeholder: null,
        dragHandleProps: jest.fn(),
        draggableStyle: {},
        innerRef: jest.fn(),
    },
    scrollElementIntoView: jest.fn(),
    scrollIntoView: jest.fn(),
    snapshot: { isDragging: false },
    sourcePaths: ["birthdate"],
    type: "complex",
    onOrderRules: jest.fn(),
};

const selectors = {
    ROW_CLICK: 'div[data-test-id="row-click"]',
};

const getWrapper = (args = props) => render(<MappingRule {...args} />);

describe("MappingRule Component", () => {
    describe("on component mounted, ", () => {
        let wrapper;
        beforeEach(() => {
            wrapper = getWrapper();
        });

        it("should rendered NavigateButton, when type is object", () => {
            const wrapper = getWrapper({
                ...props,
                type: "object",
            });
            expect(wrapper.container.querySelector(byTestId(`button-${props.id}`))).toBeInTheDocument();
        });

        it("should rendered ExpandButton, when type is NOT object", () => {
            expect(wrapper.container.querySelector(byTestId(`button-${props.id}`))).toBeInTheDocument();
        });

        it("should render ObjectMappingRule component, when rule is expanded and rule is object", () => {
            const wrapper = getWrapper({
                ...props,
                type: "root",
                expanded: true,
            });
            expect(wrapper.container.querySelector(".ecc-silk-mapping__rulesviewer")).toBeInTheDocument();
        });

        it("should render ValueMappingRule component, when rule is expanded and rule is NOT object", () => {
            const wrapper = getWrapper({
                ...props,
                expanded: true,
            });
            expect(wrapper.container.querySelector(".ecc-silk-mapping__rulesviewer")).toBeInTheDocument();
        });

        it("should render reorder context menu, when rule is not expanded", () => {
            expect(findAllDOMElements(wrapper, ".eccgui-contextmenu")).toHaveLength(1);
        });

        afterEach(() => {
            wrapper.unmount();
        });
    });

    describe("on user interaction, ", () => {
        let wrapper;
        beforeEach(() => {
            wrapper = getWrapper();
        });

        it("should on row click navigate to the rule page, when rule type is object", () => {
            const wrapper = getWrapper({
                ...props,
                type: "object",
            });
            clickFoundElement(wrapper, selectors.ROW_CLICK);
            expect(onRuleIdChangeFn).toHaveBeenCalledWith({
                newRuleId: props.id,
                parentId: props.parentId,
            });
        });

        it("should on row click expand the rule, when rule type is NOT object", () => {
            clickFoundElement(wrapper, selectors.ROW_CLICK);
            expect(onExpandFn).toHaveBeenCalled();
        });

        afterEach(() => {
            wrapper.unmount();
        });
    });
});
