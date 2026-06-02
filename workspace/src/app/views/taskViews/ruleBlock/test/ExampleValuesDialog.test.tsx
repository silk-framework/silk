import React from "react";
import "@testing-library/jest-dom";
import { fireEvent, render, screen } from "@testing-library/react";
import { mockReactI18next, testTranslate } from "../../../../test/jestTestUtils";
import type { IRuleBlockInputExample, IRuleBlockPort } from "../ruleBlock.types";

const createExampleValuesDialogHarness = () => {
    jest.resetModules();
    jest.doMock("react", () => React);
    mockReactI18next(testTranslate);
    jest.doMock("../../../../../../../libs/gui-elements", () => {
        const React = require("react");
        const omitUnsupportedDomProps = (props) => {
            const {
                "data-test-id": dataTestId,
                affirmative,
                boxed,
                canGrow,
                canShrink,
                disruptive,
                hasDivider,
                hasSpacing,
                hideOverflow,
                intent,
                interactive,
                large,
                medium,
                minimal,
                noWrap,
                rightIcon,
                size,
                singleColumn,
                title,
                useAbsoluteSpace,
                verticalStretchable,
                verticalStretched,
                ...domProps
            } = props;
            return dataTestId ? { ...domProps, "data-testid": dataTestId } : domProps;
        };
        return {
            Button: ({ children, text, onClick, ...props }) => (
                <button onClick={onClick} {...omitUnsupportedDomProps(props)}>
                    {children ?? text}
                </button>
            ),
            ClassNames: {
                Intent: {
                    ACCENT: "eccgui-intent--accent",
                },
            },
            CodeEditor: ({ defaultValue, onChange, id, name, "data-test-id": dataTestId }) => {
                const [value, setValue] = React.useState(defaultValue ?? "");
                React.useEffect(() => {
                    setValue(defaultValue ?? "");
                }, [defaultValue]);
                return (
                    <textarea
                        id={id}
                        name={name}
                        data-testid={dataTestId}
                        value={value}
                        onChange={(event) => {
                            setValue(event.target.value);
                            onChange(event.target.value);
                        }}
                    />
                );
            },
            FieldSet: ({ children, title, ...props }) => (
                <fieldset {...omitUnsupportedDomProps(props)}>
                    {title ? <legend>{title}</legend> : null}
                    {children}
                </fieldset>
            ),
            FieldItem: ({ children, labelProps, helperText, ...props }) => (
                <div {...omitUnsupportedDomProps(props)}>
                    {labelProps?.text ? <label>{labelProps.text}</label> : null}
                    {helperText ? <div>{helperText}</div> : null}
                    {children}
                </div>
            ),
            Grid: ({ children, ...props }) => <div {...omitUnsupportedDomProps(props)}>{children}</div>,
            GridColumn: ({ children, ...props }) => <div {...omitUnsupportedDomProps(props)}>{children}</div>,
            GridRow: ({ children, ...props }) => <div {...omitUnsupportedDomProps(props)}>{children}</div>,
            IconButton: ({ text, name, onClick, ...props }) => (
                <button onClick={onClick} {...omitUnsupportedDomProps(props)}>
                    {text ?? name}
                </button>
            ),
            OverviewItem: ({ children, onClick }) => <div onClick={onClick}>{children}</div>,
            OverviewItemActions: ({ children }) => <div>{children}</div>,
            OverviewItemDescription: ({ children }) => <div>{children}</div>,
            OverviewItemLine: ({ children }) => <div>{children}</div>,
            OverviewItemList: ({ children }) => <div>{children}</div>,
            OverflowText: ({ children, ...props }) => <span {...omitUnsupportedDomProps(props)}>{children}</span>,
            PropertyName: ({ children, ...props }) => <dt {...omitUnsupportedDomProps(props)}>{children}</dt>,
            PropertyValue: ({ children, ...props }) => <dd {...omitUnsupportedDomProps(props)}>{children}</dd>,
            PropertyValueList: ({ children, ...props }) => <dl {...omitUnsupportedDomProps(props)}>{children}</dl>,
            PropertyValuePair: ({ children, ...props }) => <div {...omitUnsupportedDomProps(props)}>{children}</div>,
            SearchField: ({ value, onChange, emptySearchInputMessage }) => (
                <input
                    aria-label={emptySearchInputMessage}
                    value={value}
                    onChange={onChange}
                />
            ),
            SimpleDialog: ({ title, children, actions }) => (
                <div>
                    <h1>{title}</h1>
                    <div>{children}</div>
                    <div>{actions}</div>
                </div>
            ),
            Spacing: () => <div />,
            Tag: ({ children, onClick, onRemove, intent, ...props }) => (
                <span>
                    <button onClick={onClick} data-intent={intent} {...omitUnsupportedDomProps(props)}>
                        {children}
                    </button>
                    {onRemove ? <button onClick={onRemove}>remove</button> : null}
                </span>
            ),
            TagList: ({ children }) => <div>{children}</div>,
            TextField: ({ value, onChange, placeholder, "data-test-id": dataTestId }) => (
                <input value={value} onChange={onChange} placeholder={placeholder} data-testid={dataTestId} />
            ),
            Toolbar: ({ children, ...props }) => <div {...omitUnsupportedDomProps(props)}>{children}</div>,
            ToolbarSection: ({ children, ...props }) => <div {...omitUnsupportedDomProps(props)}>{children}</div>,
            Tooltip: ({ children }) => <>{children}</>,
        };
    });

    const { ExampleValuesDialog } = require("../ExampleValuesDialog") as typeof import("../ExampleValuesDialog");
    return { ExampleValuesDialog };
};

const createPort = (overrides: Partial<IRuleBlockPort> = {}): IRuleBlockPort => ({
    id: "inputPortA",
    label: "Input A",
    description: "",
    displayOrder: 1,
    deprecated: false,
    ...overrides,
});

const createExample = (overrides: Partial<IRuleBlockInputExample> = {}): IRuleBlockInputExample => ({
    id: "example-1",
    label: "",
    inputs: {
        inputPortA: ["Original value"],
    },
    ...overrides,
});

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
                onClose={onClose}
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
                onClose={jest.fn()}
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
                onClose={jest.fn()}
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
                onClose={jest.fn()}
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
                onClose={jest.fn()}
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

    it("should highlight the targeted port row and label", () => {
        const harness = createExampleValuesDialogHarness();

        render(
            <harness.ExampleValuesDialog
                ports={[createPort(), createPort({ id: "inputPortB", label: "Input B", displayOrder: 2 })]}
                inputExamples={[createExample({ inputs: { inputPortA: ["Original value"], inputPortB: ["Other value"] } })]}
                highlightedPortId="inputPortB"
                onClose={jest.fn()}
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
});
