import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";

import "@testing-library/jest-dom";

import { FileSelectionMenu } from "./FileSelectionMenu";

const mockResourceUploadHandle = {
    cancelAll: jest.fn(),
    reset: jest.fn(),
    upload: jest.fn(() => Promise.resolve()),
};

jest.mock("./ProjectResourceUpload", () => {
    const MockReact = require("react");
    return MockReact.forwardRef((props, ref) => {
        MockReact.useImperativeHandle(ref, () => mockResourceUploadHandle);
        return MockReact.createElement("div", { "data-testid": "project-resource-upload" });
    });
});

const props = {
    insideModal: true,
    onChange: jest.fn(),
    projectId: "project",
    required: true,
    t: (key: string) => key,
};

beforeEach(() => jest.clearAllMocks());

describe("FileSelectionMenu", () => {
    it("renders the shared project-resource uploader and preserves the imperative adapter", () => {
        const getInstance = jest.fn();
        render(<FileSelectionMenu {...props} getInstance={getInstance} />);

        expect(screen.getByTestId("project-resource-upload")).toBeInTheDocument();
        getInstance.mock.calls[0][0].reset();
        expect(mockResourceUploadHandle.reset).toHaveBeenCalledTimes(1);
    });

    it("retains existing-resource selection and empty-resource creation", () => {
        const autocomplete = {
            itemRenderer: (item: string) => item,
            itemValueRenderer: (item: string) => item,
            itemValueSelector: (item: string) => item,
            itemValueString: (item: string) => item,
            noResultText: "No results",
            onSearch: () => [],
        };
        render(<FileSelectionMenu {...props} advanced={{ autocomplete }} />);

        expect(screen.getByRole("combobox")).toBeInTheDocument();
        fireEvent.click(document.querySelector('input[value="EMPTY"]')!);
        expect(document.querySelector("#fileInput")).toBeInTheDocument();
        fireEvent.click(document.querySelector('input[value="NEW"]')!);
        expect(screen.getByTestId("project-resource-upload")).toBeInTheDocument();
    });
});
