import React from "react";
import "@testing-library/jest-dom";
import { fireEvent, render, screen, within } from "@testing-library/react";
import jestTestUtils from "../../../../test/jestTestUtils";
import ruleTestHelper from "../../shared/rules/tests/ruleTestHelper";
import type { IRuleBlockInputExample, RuleBlockPort } from "../ruleBlock.types";

const createBubblingTagMock = () =>
    ({ children, onClick, onRemove, intent, ...props }) => (
        <span>
            <button onClick={onClick} data-intent={intent} {...jestTestUtils.omitUnsupportedDomProps(props)}>
                {children}
            </button>
            {onRemove ? (
                <button
                    onClick={(event) => {
                        onRemove(event);
                        onClick?.(event);
                    }}
                >
                    remove
                </button>
            ) : null}
        </span>
    );

const createCheckboxMock = () =>
    ({ checked, onChange, id }) => (
        <input
            id={id}
            type="checkbox"
            checked={checked}
            onChange={(event) => onChange(event)}
        />
    );

const createExampleValuesDialogGuiElementsModule = () => {
    const React = require("react");
    return {
        Button: jestTestUtils.createButtonMock((props) => ({
            ...jestTestUtils.omitUnsupportedDomProps(props),
            onClick: props.onClick,
            text: props.text,
        })),
        Checkbox: createCheckboxMock(),
        ClassNames: jestTestUtils.createClassNamesMock(),
        CodeEditor: jestTestUtils.createCodeEditorMock(React),
        FieldSet: jestTestUtils.createFieldSetMock(),
        FieldItem: jestTestUtils.createFieldItemMock({ helperTextProp: "helperText" }),
        Grid: jestTestUtils.createDivPassthroughMock(),
        GridColumn: jestTestUtils.createDivPassthroughMock(),
        GridRow: jestTestUtils.createDivPassthroughMock(),
        IconButton: jestTestUtils.createButtonMock((props) => ({
            ...jestTestUtils.omitUnsupportedDomProps(props),
            onClick: props.onClick,
            text: props.text ?? props.name,
        })),
        OverviewItem: jestTestUtils.createClickableContainerMock(),
        OverviewItemActions: jestTestUtils.createChildrenOnlyMock(),
        OverviewItemDescription: jestTestUtils.createChildrenOnlyMock(),
        OverviewItemLine: jestTestUtils.createChildrenOnlyMock(),
        OverviewItemList: jestTestUtils.createChildrenOnlyMock(),
        OverflowText: jestTestUtils.createDivPassthroughMock("span"),
        PropertyName: jestTestUtils.createDivPassthroughMock("dt"),
        PropertyValue: jestTestUtils.createDivPassthroughMock("dd"),
        PropertyValueList: jestTestUtils.createDivPassthroughMock("dl"),
        PropertyValuePair: jestTestUtils.createDivPassthroughMock(),
        SearchField: jestTestUtils.createSearchFieldMock(),
        SimpleDialog: jestTestUtils.createSimpleDialogMock(),
        Spacing: jestTestUtils.createChildrenOnlyMock(),
        Tag: createBubblingTagMock(),
        TagList: jestTestUtils.createChildrenOnlyMock(),
        TextField: jestTestUtils.createTextFieldMock({ includePlaceholder: true, includeTestId: true }),
        Toolbar: jestTestUtils.createDivPassthroughMock(),
        ToolbarSection: jestTestUtils.createDivPassthroughMock(),
        Tooltip: jestTestUtils.createFragmentMock(),
    };
};

const createExampleValuesDialogHarness = () => {
    jest.resetModules();
    jest.doMock("react", () => React);
    jestTestUtils.mockReactI18next(jestTestUtils.testTranslate);
    jest.doMock("../../../../../../../libs/gui-elements", createExampleValuesDialogGuiElementsModule);

    const { ExampleValuesDialog } = require("../ExampleValuesDialog") as typeof import("../ExampleValuesDialog");
    return { ExampleValuesDialog };
};

const createPort = (overrides: Partial<RuleBlockPort> = {}): RuleBlockPort =>
    ruleTestHelper.createRuleBlockPort(overrides);

const createExample = (overrides: Partial<IRuleBlockInputExample> = {}): IRuleBlockInputExample =>
    ruleTestHelper.createRuleBlockInputExample(overrides);

describe("ExampleValuesDialog", () => {
    it("should update the active chip preview immediately while editing and apply the changed examples", () => {
        const harness = createExampleValuesDialogHarness();
        const onApply = jest.fn();
        const onClose = jest.fn();

        render(
            <harness.ExampleValuesDialog
                ports={[createPort()]}
                inputExamples={[createExample()]}
                highlightedPortId={undefined}
                selectedExampleIdsForEvaluation={[]}
                onClose={onClose}
                onSelectedExampleIdsForEvaluationChange={jest.fn()}
                onApply={onApply}
            />,
        );

        fireEvent.click(screen.getByRole("button", { name: "Original value" }));

        const editor = screen.getByTestId("example-values-editor");
        expect(editor).toHaveValue("Original value");
        expect(screen.getByRole("button", { name: "Original value" })).toBeInTheDocument();

        fireEvent.change(editor, { target: { value: "Updated value" } });

        expect(screen.getByRole("button", { name: "Updated value" })).toBeInTheDocument();

        fireEvent.click(screen.getByRole("button", { name: "Apply and close" }));

        expect(onApply).toHaveBeenCalledWith([
            {
                id: "example-1",
                inputs: {
                    inputPortA: ["Updated value"],
                },
            },
        ]);
        expect(onClose).not.toHaveBeenCalled();
    });

    it("should open without a pre-selected value", () => {
        const harness = createExampleValuesDialogHarness();

        render(
            <harness.ExampleValuesDialog
                ports={[createPort()]}
                inputExamples={[createExample()]}
                highlightedPortId={undefined}
                selectedExampleIdsForEvaluation={[]}
                onClose={jest.fn()}
                onSelectedExampleIdsForEvaluationChange={jest.fn()}
                onApply={jest.fn()}
            />,
        );

        expect(screen.queryByTestId("example-values-editor")).not.toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Original value" })).not.toHaveAttribute("data-intent", "accent");
    });

    it("should use the optional example label and fall back to Example N when the label is empty", () => {
        const harness = createExampleValuesDialogHarness();
        const onApply = jest.fn();

        render(
            <harness.ExampleValuesDialog
                ports={[createPort()]}
                inputExamples={[createExample(), createExample({ id: "example-2", label: "Named example" })]}
                highlightedPortId={undefined}
                selectedExampleIdsForEvaluation={[]}
                onClose={jest.fn()}
                onSelectedExampleIdsForEvaluationChange={jest.fn()}
                onApply={onApply}
            />,
        );

        const labelInput = screen.getByTestId("example-values-label");
        expect(labelInput).toHaveValue("");
        expect(labelInput.getAttribute("placeholder")).toContain("Example");
        expect(screen.getAllByText("Example {{index}}")).toHaveLength(2);
        expect(screen.getByText("Named example")).toBeInTheDocument();

        fireEvent.change(labelInput, { target: { value: "  First example  " } });

        expect(labelInput).toHaveValue("  First example  ");
        expect(screen.getAllByText("First example")).toHaveLength(2);

        fireEvent.change(labelInput, { target: { value: "   " } });

        expect(screen.getAllByText("Example {{index}}")).toHaveLength(2);

        fireEvent.click(screen.getByRole("button", { name: "Apply and close" }));

        expect(onApply).toHaveBeenCalledWith([
            {
                id: "example-1",
                label: undefined,
                inputs: {
                    inputPortA: ["Original value"],
                },
            },
            {
                id: "example-2",
                label: "Named example",
                inputs: {
                    inputPortA: ["Original value"],
                },
            },
        ]);
    });

    it("should close and reopen the bottom editor pane", () => {
        const harness = createExampleValuesDialogHarness();

        render(
            <harness.ExampleValuesDialog
                ports={[createPort()]}
                inputExamples={[createExample()]}
                highlightedPortId={undefined}
                selectedExampleIdsForEvaluation={[]}
                onClose={jest.fn()}
                onSelectedExampleIdsForEvaluationChange={jest.fn()}
                onApply={jest.fn()}
            />,
        );

        fireEvent.click(screen.getByRole("button", { name: "Original value" }));

        expect(screen.getByTestId("example-values-editor")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Original value" })).toHaveAttribute("data-intent", "accent");

        fireEvent.click(screen.getByTestId("example-values-editor-close"));

        expect(screen.queryByTestId("example-values-editor")).not.toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Original value" })).not.toHaveAttribute("data-intent", "accent");

        fireEvent.click(screen.getByRole("button", { name: "Original value" }));

        expect(screen.getByTestId("example-values-editor")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Original value" })).toHaveAttribute("data-intent", "accent");
    });

    it("should clear the active value selection when switching examples", () => {
        const harness = createExampleValuesDialogHarness();

        render(
            <harness.ExampleValuesDialog
                ports={[createPort()]}
                inputExamples={[
                    createExample(),
                    createExample({
                        id: "example-2",
                        label: "Second example",
                        inputs: {
                            inputPortA: ["Second value"],
                        },
                    }),
                ]}
                highlightedPortId={undefined}
                selectedExampleIdsForEvaluation={[]}
                onClose={jest.fn()}
                onSelectedExampleIdsForEvaluationChange={jest.fn()}
                onApply={jest.fn()}
            />,
        );

        fireEvent.click(screen.getByRole("button", { name: "Original value" }));

        expect(screen.getByRole("button", { name: "Original value" })).toHaveAttribute("data-intent", "accent");
        expect(screen.getByTestId("example-values-editor")).toBeInTheDocument();

        fireEvent.click(screen.getByText("Second example"));

        expect(screen.getByRole("button", { name: "Second value" })).not.toHaveAttribute("data-intent", "accent");
        expect(screen.queryByTestId("example-values-editor")).not.toBeInTheDocument();
    });

    it("should keep the current editor selection when deleting a different value", () => {
        const harness = createExampleValuesDialogHarness();

        render(
            <harness.ExampleValuesDialog
                ports={[createPort()]}
                inputExamples={[
                    createExample({
                        inputs: {
                            inputPortA: ["First value", "Second value"],
                        },
                    }),
                ]}
                highlightedPortId={undefined}
                selectedExampleIdsForEvaluation={[]}
                onClose={jest.fn()}
                onSelectedExampleIdsForEvaluationChange={jest.fn()}
                onApply={jest.fn()}
            />,
        );

        fireEvent.click(screen.getByRole("button", { name: "Second value" }));

        expect(screen.getByTestId("example-values-editor")).toHaveValue("Second value");
        expect(screen.getByRole("button", { name: "Second value" })).toHaveAttribute("data-intent", "accent");

        fireEvent.click(screen.getAllByRole("button", { name: "remove" })[0]);

        expect(screen.getByTestId("example-values-editor")).toHaveValue("Second value");
        expect(screen.getByRole("button", { name: "Second value" })).toHaveAttribute("data-intent", "accent");
    });

    it("should close the editor without activating another value when deleting the active value", () => {
        const harness = createExampleValuesDialogHarness();

        render(
            <harness.ExampleValuesDialog
                ports={[createPort()]}
                inputExamples={[
                    createExample({
                        inputs: {
                            inputPortA: ["First value", "Second value"],
                        },
                    }),
                ]}
                highlightedPortId={undefined}
                selectedExampleIdsForEvaluation={[]}
                onClose={jest.fn()}
                onSelectedExampleIdsForEvaluationChange={jest.fn()}
                onApply={jest.fn()}
            />,
        );

        fireEvent.click(screen.getByRole("button", { name: "Second value" }));

        expect(screen.getByTestId("example-values-editor")).toHaveValue("Second value");
        expect(screen.getByRole("button", { name: "Second value" })).toHaveAttribute("data-intent", "accent");

        fireEvent.click(screen.getAllByRole("button", { name: "remove" })[1]);

        expect(screen.queryByTestId("example-values-editor")).not.toBeInTheDocument();
        expect(screen.getByRole("button", { name: "First value" })).not.toHaveAttribute("data-intent", "accent");
    });

    it("should highlight the targeted port row and label", () => {
        const harness = createExampleValuesDialogHarness();

        render(
            <harness.ExampleValuesDialog
                ports={[createPort(), createPort({ id: "inputPortB", label: "Input B", displayOrder: 2 })]}
                inputExamples={[
                    createExample({ inputs: { inputPortA: ["Original value"], inputPortB: ["Other value"] } }),
                ]}
                highlightedPortId="inputPortB"
                selectedExampleIdsForEvaluation={[]}
                onClose={jest.fn()}
                onSelectedExampleIdsForEvaluationChange={jest.fn()}
                onApply={jest.fn()}
            />,
        );

        expect(screen.getByTestId("example-values-row-inputPortB")).toHaveClass(
            "ecc-silk-rule-block-example-values-dialog__port-pair--highlighted",
        );
        expect(screen.getByText("Input B").closest("dt")).toHaveClass(
            "ecc-silk-rule-block-example-values-dialog__port-name--highlighted",
        );
        expect(screen.getByTestId("example-values-row-inputPortA")).not.toHaveClass(
            "ecc-silk-rule-block-example-values-dialog__port-pair--highlighted",
        );
    });

    it("should commit the non-persistent evaluation selection on close and clear it on demand", () => {
        const harness = createExampleValuesDialogHarness();
        const onSelectedExampleIdsForEvaluationChange = jest.fn();

        render(
            <harness.ExampleValuesDialog
                ports={[createPort()]}
                inputExamples={[
                    createExample(),
                    createExample({ id: "example-2", label: "Second example" }),
                ]}
                highlightedPortId={undefined}
                selectedExampleIdsForEvaluation={["example-1"]}
                onClose={jest.fn()}
                onSelectedExampleIdsForEvaluationChange={onSelectedExampleIdsForEvaluationChange}
                onApply={jest.fn()}
            />,
        );

        const checkboxes = screen.getAllByRole("checkbox");
        expect(checkboxes[0]).toBeChecked();
        expect(screen.getByText("{{count}} selected for evaluation")).toBeInTheDocument();

        fireEvent.click(
            within(screen.getByTestId("example-values-clear-selection").parentElement as HTMLElement).getByRole(
                "button",
                { name: "remove" },
            ),
        );

        expect(checkboxes[0]).not.toBeChecked();
        expect(screen.queryByText("{{count}} selected for evaluation")).not.toBeInTheDocument();

        fireEvent.click(checkboxes[1]);
        fireEvent.click(screen.getByRole("button", { name: "common.action.close" }));

        expect(onSelectedExampleIdsForEvaluationChange).toHaveBeenCalledWith(["example-2"]);
    });
});
