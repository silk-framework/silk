import React from "react";
import "@testing-library/jest-dom";
import { fireEvent, render, screen } from "@testing-library/react";
import { createFormGuiElementsModule, mockReactI18next } from "../../../test/jestTestUtils";
import type { InputPortDialogSubmitValue } from "./InputPortDialog";
import type { IRuleBlockPort } from "./ruleBlock.types";

const loadInputPortDialog = () => {
    jest.resetModules();
    jest.doMock("react", () => React);
    mockReactI18next((key) => key);
    jest.doMock("@eccenca/gui-elements", createFormGuiElementsModule);
    return require("./InputPortDialog").default as typeof import("./InputPortDialog").default;
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

describe("InputPortDialog", () => {
    afterEach(() => {
        jest.dontMock("react-i18next");
        jest.dontMock("@eccenca/gui-elements");
    });

    it("should render edit mode values and submit the trimmed result", () => {
        const InputPortDialog = loadInputPortDialog();
        const onSubmit = jest.fn();

        const { container } = render(
            <InputPortDialog
                isOpen={true}
                mode="edit"
                initialPort={initialPort()}
                existingPorts={[existingPort()]}
                editedPortId="inputPortA"
                onClose={jest.fn()}
                onSubmit={onSubmit}
            />,
        );

        fireEvent.change(container.querySelector("#input-port-label") as HTMLInputElement, {
            target: { value: "  Updated input  " },
        });
        fireEvent.change(container.querySelector("#input-port-display-order") as HTMLInputElement, {
            target: { value: "4" },
        });
        fireEvent.change(container.querySelector("#input-port-description") as HTMLTextAreaElement, {
            target: { value: "Updated description" },
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

        const { container } = render(
            <InputPortDialog
                isOpen={true}
                mode="create"
                initialPort={initialPort({ label: "", displayOrder: 1 })}
                existingPorts={[
                    existingPort({ id: "inputPortA", label: "Existing label", displayOrder: 3 }),
                    existingPort({ id: "inputPortB", label: "Other label", displayOrder: 5 }),
                ]}
                onClose={jest.fn()}
                onSubmit={jest.fn()}
            />,
        );

        fireEvent.change(container.querySelector("#input-port-label") as HTMLInputElement, {
            target: { value: "Existing label" },
        });
        fireEvent.change(container.querySelector("#input-port-display-order") as HTMLInputElement, {
            target: { value: "5" },
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
            onClose: jest.fn(),
            onSubmit: jest.fn(),
        };
        const { container, rerender } = render(<InputPortDialog {...props} />);

        fireEvent.change(container.querySelector("#input-port-label") as HTMLInputElement, {
            target: { value: "Typed label" },
        });
        fireEvent.change(container.querySelector("#input-port-display-order") as HTMLInputElement, {
            target: { value: "11" },
        });

        rerender(
            <InputPortDialog
                {...props}
                initialPort={{ ...initial }}
            />,
        );

        expect(container.querySelector("#input-port-label")).toHaveValue("Typed label");
        expect(container.querySelector("#input-port-display-order")).toHaveValue(11);
    });
});
