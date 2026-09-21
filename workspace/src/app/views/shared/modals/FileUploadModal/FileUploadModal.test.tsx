import React from "react";
import { act, fireEvent, render } from "@testing-library/react";

import "@testing-library/jest-dom";

import { FileUploadModal } from "./FileUploadModal";

let mockUploaderProps: Record<string, any>;
const mockUploaderInstance = {
    cancelAll: jest.fn(),
    reset: jest.fn(),
    upload: jest.fn(),
};

jest.mock("../../FileUploader", () => {
    const MockReact = require("react");
    return function MockFileUploader(props) {
        mockUploaderProps = props;
        MockReact.useEffect(() => props.getInstance(mockUploaderInstance), [props.getInstance]);
        return MockReact.createElement("div", { "data-testid": "file-uploader" });
    };
});

jest.mock("react-redux", () => ({
    useSelector: (selector) => selector({}),
}));

jest.mock("@ducks/common", () => ({
    commonSel: {
        currentProjectIdSelector: () => "project-id",
        initialSettingsSelector: () => ({ maxFileUploadSize: 1_000 }),
    },
}));

jest.mock("react-i18next", () => ({
    useTranslation: () => [(key, fallback) => (typeof fallback === "string" ? fallback : key)],
}));

beforeEach(() => {
    jest.clearAllMocks();
    mockUploaderProps = {};
});

describe("FileUploadModal", () => {
    it("prevents closing for the complete active upload lifecycle", () => {
        const onDiscard = jest.fn();
        render(<FileUploadModal isOpen onDiscard={onDiscard} />);
        const closeButton = document.querySelector(
            '[data-test-id="file-upload-dialog-close-btn"]',
        ) as HTMLButtonElement;

        expect(closeButton).toBeEnabled();
        act(() => mockUploaderProps.onUploadStateChange(true));
        expect(closeButton).toBeDisabled();
        fireEvent.click(closeButton);
        expect(onDiscard).not.toHaveBeenCalled();

        act(() => mockUploaderProps.onUploadStateChange(false));
        expect(closeButton).toBeEnabled();
        fireEvent.click(closeButton);
        expect(mockUploaderInstance.reset).toHaveBeenCalledTimes(1);
        expect(onDiscard).toHaveBeenCalledTimes(1);
    });
});
