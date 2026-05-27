import React from "react";
import "@testing-library/jest-dom";
import { fireEvent, render, screen } from "@testing-library/react";
import { createFormGuiElementsModule, mockReactI18next } from "../../../../test/jestTestUtils";
import type { InputPortDialogSubmitValue } from "../InputPortDialog";
import type { IRuleBlockPort } from "../ruleBlock.types";

const loadInputPortDialog = () => {
    jest.resetModules();
    jest.doMock("react", () => React);
    mockReactI18next((key) => key);
    jest.doMock("../../../../../../../libs/gui-elements", createFormGuiElementsModule);
    return require("../InputPortDialog").default as typeof import("../InputPortDialog").default;
};

const existingPort = (overrides: Partial<IRuleBlockPort> = {}): IRuleBlockPort => ({
    id: "inputPortA",
    label: "Input A",
    description: "Existing description",
    exampleValues: "- existing",
    displayOrder: 2,
    deprecated: false,
    ...overrides,
});

const initialPort = (overrides: Partial<InputPortDialogSubmitValue> = {}): InputPortDialogSubmitValue => ({
    label: "Input A",
    description: "Initial description",
    exampleValues: "- initial",
    displayOrder: 2,
    deprecated: false,
    ...overrides,
});

const getLabelField = (): HTMLInputElement => screen.getByRole("textbox", { name: /form\.field\.label/ });

const getDisplayOrderField = (): HTMLInputElement =>
    screen.getByRole("spinbutton", { name: "taskViews.ruleBlock.displayOrder" });

const getDescriptionField = (): HTMLTextAreaElement =>
    screen.getByRole("textbox", { name: "common.words.description" });

const updatePortForm = ({
    label,
    displayOrder,
    description,
}: {
    label?: string;
    displayOrder?: string;
    description?: string;
}) => {
    if (label !== undefined) {
        fireEvent.change(getLabelField(), {
            target: { value: label },
        });
    }
    if (displayOrder !== undefined) {
        fireEvent.change(getDisplayOrderField(), {
            target: { value: displayOrder },
        });
    }
    if (description !== undefined) {
        fireEvent.change(getDescriptionField(), {
            target: { value: description },
        });
    }
};

describe("InputPortDialog", () => {
    afterEach(() => {
        jest.dontMock("react-i18next");
        jest.dontMock("../../../../../../../libs/gui-elements");
    });

    it("should render edit mode values and submit the trimmed result", () => {
        const InputPortDialog = loadInputPortDialog();
        const onSubmit = jest.fn();

        render(
            <InputPortDialog
                isOpen={true}
                mode="edit"
                initialPort={initialPort()}
                existingPorts={[existingPort()]}
                persistedPorts={[existingPort()]}
                isRuleBlockInUse={false}
                editedPortId="inputPortA"
                onClose={jest.fn()}
                onSubmit={onSubmit}
            />,
        );

        updatePortForm({
            label: "  Updated input  ",
            displayOrder: "4",
            description: "Updated description",
        });
        fireEvent.click(screen.getByRole("button", { name: "common.action.update" }));

        expect(onSubmit).toHaveBeenCalledWith({
            label: "Updated input",
            description: "Updated description",
            exampleValues: "- initial",
            displayOrder: 4,
            deprecated: false,
        });
    });

    it("should show duplicate validation errors and disable submit", () => {
        const InputPortDialog = loadInputPortDialog();

        render(
            <InputPortDialog
                isOpen={true}
                mode="create"
                initialPort={initialPort({ label: "", displayOrder: 1 })}
                existingPorts={[
                    existingPort({ id: "inputPortA", label: "Existing label", displayOrder: 3 }),
                    existingPort({ id: "inputPortB", label: "Other label", displayOrder: 5 }),
                ]}
                persistedPorts={[]}
                isRuleBlockInUse={false}
                onClose={jest.fn()}
                onSubmit={jest.fn()}
            />,
        );

        updatePortForm({
            label: "Existing label",
            displayOrder: "5",
        });

        expect(screen.getByText("taskViews.ruleBlock.errors.duplicateInputPortLabel")).toBeInTheDocument();
        expect(screen.getByText("taskViews.ruleBlock.errors.duplicateDisplayOrder")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "common.action.add" })).toBeDisabled();
    });

    it("should not reset typed values when rerendered with a new initialPort object of equal values", () => {
        const InputPortDialog = loadInputPortDialog();
        const initial = initialPort({ label: "Initial input", displayOrder: 7 });
        const props = {
            isOpen: true,
            mode: "create" as const,
            initialPort: initial,
            existingPorts: [] as IRuleBlockPort[],
            persistedPorts: [] as IRuleBlockPort[],
            isRuleBlockInUse: false,
            onClose: jest.fn(),
            onSubmit: jest.fn(),
        };
        const { rerender } = render(<InputPortDialog {...props} />);

        updatePortForm({
            label: "Typed label",
            displayOrder: "11",
        });

        rerender(
            <InputPortDialog
                {...props}
                initialPort={{ ...initial }}
            />,
        );

        expect(getLabelField()).toHaveValue("Typed label");
        expect(getDisplayOrderField()).toHaveValue(11);
    });

    it("should block reordering a persisted input port when the rule block is in use", () => {
        const InputPortDialog = loadInputPortDialog();

        render(
            <InputPortDialog
                isOpen={true}
                mode="edit"
                initialPort={initialPort({ label: "Input A", displayOrder: 1 })}
                existingPorts={[
                    existingPort({ id: "inputPortA", label: "Input A", displayOrder: 1 }),
                    existingPort({ id: "inputPortB", label: "Input B", displayOrder: 2 }),
                ]}
                persistedPorts={[
                    existingPort({ id: "inputPortA", label: "Input A", displayOrder: 1 }),
                    existingPort({ id: "inputPortB", label: "Input B", displayOrder: 2 }),
                ]}
                isRuleBlockInUse={true}
                editedPortId="inputPortA"
                onClose={jest.fn()}
                onSubmit={jest.fn()}
            />,
        );

        updatePortForm({
            displayOrder: "3",
        });

        expect(screen.getByText(/taskViews\.ruleBlock\.errors\.usedPortReordered/)).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "common.action.update" })).toBeDisabled();
    });
});
