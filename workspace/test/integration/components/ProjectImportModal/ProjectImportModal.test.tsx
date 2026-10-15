import React from "react";
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ProjectImportModal } from "../../../../src/app/views/shared/modals/ProjectImportModal";
import * as requests from "@ducks/workspace/requests";
import { requestProjectIdValidation } from "@ducks/common/requests";

jest.mock("../../../../src/app/utils/getApiEndpoint", () => ({ workspaceApi: (path: string) => path }));
jest.mock("react-redux", () => ({ useDispatch: () => jest.fn() }));
jest.mock("react-i18next", () => {
    const t = (key: string) => key;
    return { useTranslation: () => [t] };
});
jest.mock("@ducks/workspace/requests", () => ({
    fetchProjectAccessControl: jest.fn(),
    requestProjectImportDetails: jest.fn(),
    requestStartProjectImport: jest.fn(),
    requestProjectImportExecutionStatus: jest.fn(),
    requestDeleteProjectImport: jest.fn(),
}));
jest.mock("@ducks/common/requests", () => ({ requestProjectIdValidation: jest.fn() }));
jest.mock("@ducks/router", () => ({ routerOp: { goToPage: jest.fn() } }));
jest.mock("../../../../src/app/hooks/useProjectAclManagementComponent", () => {
    const React = jest.requireActual("react");
    return {
        useProjectAclManagementComponent: ({ onChange }) => ({
            component: React.createElement(
                "button",
                { onClick: () => onChange({ groups: ["editors"] }) },
                "Select ACL",
            ),
        }),
    };
});
jest.mock("../../../../src/app/views/shared/FileUploader/cases/UploadNewFile/UploadNewFile", () => {
    const React = jest.requireActual("react");
    return {
        UploadNewFile: ({ onUploadSuccess }) =>
            React.createElement(
                "button",
                { onClick: () => onUploadSuccess({}, { body: { projectImportId: "upload" } }) },
                "Upload",
            ),
    };
});
const details = { projectId: "original", label: "Project", marshallerId: "xml", projectAlreadyExists: false };
const start = jest.mocked(requests.requestStartProjectImport);
const validate = jest.mocked(requestProjectIdValidation);
async function open(overrides = {}) {
    jest.mocked(requests.requestProjectImportDetails).mockResolvedValue({
        data: { ...details, ...overrides },
    } as Awaited<ReturnType<typeof requests.requestProjectImportDetails>>);
    render(<ProjectImportModal close={jest.fn()} />);
    fireEvent.click(screen.getByText("Upload"));
    await screen.findByText("ProjectImportModal.importSummary");
}
beforeEach(() => {
    jest.clearAllMocks();
    validate.mockResolvedValue({} as Awaited<ReturnType<typeof requestProjectIdValidation>>);
    const accessControl = { groups: [] };
    jest.mocked(requests.fetchProjectAccessControl).mockResolvedValue({
        data: accessControl,
        axiosResponse: { data: accessControl, status: 200, statusText: "OK", headers: {}, config: {} },
    });
    jest.mocked(requests.requestProjectImportExecutionStatus).mockResolvedValue({
        data: { importEnded: 1, success: true, projectId: "target" },
    } as Awaited<ReturnType<typeof requests.requestProjectImportExecutionStatus>>);
});
it.each([false, true])("offers a custom ID with original collision %s", async (projectAlreadyExists) => {
    await open({ projectAlreadyExists });
    expect(screen.getByRole("radiogroup")).toHaveAttribute("data-test-id", "importDestination");
    for (const [label, testId] of [
        ["ProjectImportModal.destinationOriginal", "importDestinationOriginal"],
        ["ProjectImportModal.destinationGenerated", "importDestinationGenerated"],
        ["ProjectImportModal.destinationCustom", "importDestinationCustom"],
    ]) {
        expect(screen.getByLabelText(label)).toHaveAttribute("data-test-id", testId);
    }
    expect(document.getElementById("import-destination-label")).toHaveClass("eccgui-label--strong");
    fireEvent.click(screen.getByLabelText("ProjectImportModal.destinationCustom"));
    const input = screen.getByLabelText("CreateModal.CustomIdentifierInput.ProjectId");
    expect(input).toHaveAttribute("data-test-id", "customProjectIdInput");
    expect(input.closest('[data-test-id="customProjectIdField"]')).not.toBeNull();
    expect(screen.getByText("ProjectImportModal.importBtn").closest("button")).toBeDisabled();
    fireEvent.change(input, { target: { value: "custom" } });
    await waitFor(() => expect(validate).toHaveBeenCalledWith("custom"));
    await waitFor(() => expect(screen.getByText("ProjectImportModal.importBtn").closest("button")).toBeEnabled());
    fireEvent.click(screen.getByText("ProjectImportModal.importBtn"));
    await waitFor(() => expect(start).toHaveBeenCalledWith("upload", false, false, undefined, "custom"));
});
it("defaults to the original ID when available", async () => {
    await open();
    fireEvent.click(screen.getByText("ProjectImportModal.importBtn"));
    await waitFor(() => expect(start).toHaveBeenCalledWith("upload", false, false, undefined, undefined));
});
it("keeps the original ID on collision and requires replacement confirmation", async () => {
    await open({ projectAlreadyExists: true });
    expect(screen.getByLabelText("ProjectImportModal.destinationOriginal")).toBeChecked();
    expect(screen.getByText("ProjectImportModal.warningExistingProject")).toBeVisible();
    expect(start).not.toHaveBeenCalled();
    expect(screen.getByRole("button", { name: "ProjectImportModal.replaceImportBtn" })).toBeDisabled();
    fireEvent.click(screen.getByRole("checkbox"));
    fireEvent.click(screen.getByRole("button", { name: "ProjectImportModal.replaceImportBtn" }));
    await waitFor(() => expect(start).toHaveBeenCalledWith("upload", false, true, undefined, undefined));
});
it("does not allow replacement without access", async () => {
    await open({ projectAlreadyExists: true, noAccess: true });
    expect(screen.getByLabelText("ProjectImportModal.destinationOriginal")).toBeChecked();
    expect(screen.getByText("ProjectImportModal.warningExistingProjectForbidden")).toBeVisible();
    expect(screen.queryByRole("checkbox")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "ProjectImportModal.replaceImportBtn" })).toBeDisabled();
});
it("keeps a conflicting custom ID editable after submission", async () => {
    start.mockRejectedValueOnce({ httpStatus: 409 });
    validate
        .mockResolvedValueOnce({} as Awaited<ReturnType<typeof requestProjectIdValidation>>)
        .mockRejectedValueOnce({ httpStatus: 409 });
    await open();
    fireEvent.click(screen.getByLabelText("ProjectImportModal.destinationCustom"));
    fireEvent.change(screen.getByLabelText("CreateModal.CustomIdentifierInput.ProjectId"), {
        target: { value: "taken" },
    });
    await waitFor(() => expect(screen.getByText("ProjectImportModal.importBtn").closest("button")).toBeEnabled());
    fireEvent.click(screen.getByText("ProjectImportModal.importBtn"));
    await screen.findByText("ProjectImportModal.warningExistingProject");
    expect(screen.getByRole("button", { name: "ProjectImportModal.replaceImportBtn" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "ProjectImportModal.openExistingProject" })).toHaveAttribute(
        "href",
        expect.stringContaining("/taken"),
    );
    expect(screen.getByLabelText("CreateModal.CustomIdentifierInput.ProjectId")).toHaveValue("taken");
    fireEvent.change(screen.getByLabelText("CreateModal.CustomIdentifierInput.ProjectId"), {
        target: { value: "available" },
    });
    expect(screen.queryByRole("checkbox")).not.toBeInTheDocument();
    await waitFor(() => expect(screen.getByText("ProjectImportModal.importBtn").closest("button")).toBeEnabled());
    fireEvent.click(screen.getByText("ProjectImportModal.importBtn"));
    await waitFor(() => expect(start).toHaveBeenLastCalledWith("upload", false, false, undefined, "available"));
});

it("can generate a new ID even when the original is available", async () => {
    await open();
    fireEvent.click(screen.getByLabelText("ProjectImportModal.destinationGenerated"));
    fireEvent.click(screen.getByText("ProjectImportModal.importBtn"));
    await waitFor(() => expect(start).toHaveBeenCalledWith("upload", true, false, undefined, undefined));
});
it.each([
    [400, "CreateModal.CustomIdentifierInput.validations.invalid"],
    [500, "ProjectImportModal.validationFailed"],
])("blocks import when validation returns %s", async (httpStatus, message) => {
    validate.mockRejectedValueOnce({ httpStatus });
    await open();
    fireEvent.click(screen.getByLabelText("ProjectImportModal.destinationCustom"));
    fireEvent.change(screen.getByLabelText("CreateModal.CustomIdentifierInput.ProjectId"), {
        target: { value: "invalid" },
    });
    await screen.findByText(message);
    expect(screen.getByText("ProjectImportModal.importBtn").closest("button")).toBeDisabled();
});
it("requires replacement confirmation when custom ID validation returns 409", async () => {
    validate.mockRejectedValueOnce({ httpStatus: 409 });
    await open();
    fireEvent.click(screen.getByLabelText("ProjectImportModal.destinationCustom"));
    fireEvent.change(screen.getByLabelText("CreateModal.CustomIdentifierInput.ProjectId"), {
        target: { value: "taken" },
    });

    await screen.findByText("ProjectImportModal.warningExistingProject");
    const replaceButton = screen.getByRole("button", { name: "ProjectImportModal.replaceImportBtn" });
    expect(replaceButton).toBeDisabled();
    fireEvent.click(screen.getByRole("checkbox"));
    fireEvent.click(replaceButton);
    await waitFor(() => expect(start).toHaveBeenCalledWith("upload", false, true, undefined, "taken"));
});
it("ignores validation responses for a previous ID", async () => {
    let rejectPrevious: (reason: unknown) => void = () => {};
    validate.mockImplementationOnce(
        () =>
            new Promise((_, reject) => {
                rejectPrevious = reject;
            }),
    );
    await open();
    fireEvent.click(screen.getByLabelText("ProjectImportModal.destinationCustom"));
    const input = screen.getByLabelText("CreateModal.CustomIdentifierInput.ProjectId");
    fireEvent.change(input, { target: { value: "previous" } });
    await waitFor(() => expect(validate).toHaveBeenCalledWith("previous"));
    expect(screen.getByText("ProjectImportModal.importBtn").closest("button")).toBeDisabled();
    fireEvent.change(input, { target: { value: "current" } });
    await waitFor(() => expect(screen.getByText("ProjectImportModal.importBtn").closest("button")).toBeEnabled());
    await act(async () => rejectPrevious({ httpStatus: 409 }));
    expect(screen.queryByText("ProjectImportModal.warningExistingProject")).not.toBeInTheDocument();
    fireEvent.click(screen.getByText("ProjectImportModal.importBtn"));
    await waitFor(() => expect(start).toHaveBeenCalledWith("upload", false, false, undefined, "current"));
});

it("shows ACL controls only for new projects and clears replacement approval when switching", async () => {
    await open({ projectAlreadyExists: true });
    fireEvent.click(screen.getByLabelText("ProjectImportModal.destinationGenerated"));
    fireEvent.click(screen.getByText("Select ACL"));
    fireEvent.click(screen.getByLabelText("ProjectImportModal.destinationOriginal"));
    expect(screen.queryByText("Select ACL")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("checkbox"));
    fireEvent.click(screen.getByLabelText("ProjectImportModal.destinationGenerated"));
    fireEvent.click(screen.getByLabelText("ProjectImportModal.destinationOriginal"));
    expect(screen.getByRole("checkbox")).not.toBeChecked();
    expect(screen.getByRole("button", { name: "ProjectImportModal.replaceImportBtn" })).toBeDisabled();
    fireEvent.click(screen.getByLabelText("ProjectImportModal.destinationGenerated"));
    fireEvent.click(screen.getByText("ProjectImportModal.importBtn"));
    await waitFor(() => expect(start).toHaveBeenCalledWith("upload", true, false, ["editors"], undefined));
});
