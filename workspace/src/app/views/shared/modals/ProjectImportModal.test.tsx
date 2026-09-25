import React from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { createInstance } from "i18next";
import { I18nextProvider } from "react-i18next";
import { ProjectImportModal } from "./ProjectImportModal";
import {
    fetchProjectAccessControl,
    requestDeleteProjectImport,
    requestProjectImportDetails,
    requestProjectImportExecutionStatus,
    requestStartProjectImport,
} from "@ducks/workspace/requests";
import { requestProjectIdValidation } from "@ducks/common/requests";
import { IProjectImportDetails } from "@ducks/workspace/typings";
import translations from "../../../../locales/manual/en.json";

jest.mock("react-redux", () => ({ ...jest.requireActual("react-redux"), useDispatch: () => jest.fn() }));
jest.mock("../../../utils/getApiEndpoint", () => ({ workspaceApi: (path: string) => path }));
jest.mock("@ducks/router", () => ({ routerOp: { goToPage: jest.fn() } }));
jest.mock("../../../utils/routerUtils", () => ({ absoluteProjectPath: (projectId: string) => projectId }));
jest.mock("../../../hooks/useProjectAclManagementComponent", () => ({
    useProjectAclManagementComponent: () => ({ component: null }),
}));
jest.mock("@ducks/workspace/requests", () => ({
    fetchProjectAccessControl: jest.fn(),
    requestProjectImportDetails: jest.fn(),
    requestDeleteProjectImport: jest.fn(),
    requestProjectImportExecutionStatus: jest.fn(),
    requestStartProjectImport: jest.fn(),
}));
jest.mock("@ducks/common/requests", () => ({ requestProjectIdValidation: jest.fn() }));
jest.mock("../FileUploader/cases/UploadNewFile/UploadNewFile", () => ({
    UploadNewFile: ({ onUploadSuccess }) =>
        require("react").createElement(
            "div",
            null,
            require("react").createElement(
                "button",
                { onClick: () => onUploadSuccess({}, { body: { projectImportId: "upload-1" } }) },
                "Upload invalid",
            ),
            require("react").createElement(
                "button",
                { onClick: () => onUploadSuccess({}, { body: { projectImportId: "upload-2" } }) },
                "Upload valid",
            ),
        ),
}));

const i18n = createInstance();

beforeAll(async () => {
    await i18n.init({ lng: "en", resources: { en: { translation: translations } } });
});

beforeEach(() => {
    jest.mocked(requestProjectImportDetails)
        .mockResolvedValueOnce({
            data: {
                projectId: "invalid",
                label: "Invalid",
                projectAlreadyExists: false,
                noAccess: false,
                errorMessage: "Invalid archive",
            },
        } as Awaited<ReturnType<typeof requestProjectImportDetails>>)
        .mockResolvedValueOnce({
            data: { projectId: "valid", label: "Valid", projectAlreadyExists: false, noAccess: false },
        } as Awaited<ReturnType<typeof requestProjectImportDetails>>);
    jest.mocked(requestDeleteProjectImport).mockResolvedValue(
        {} as Awaited<ReturnType<typeof requestDeleteProjectImport>>,
    );
    const accessControl = { groups: [] };
    jest.mocked(fetchProjectAccessControl).mockResolvedValue({
        data: accessControl,
        axiosResponse: { data: accessControl, status: 200, statusText: "OK", headers: {}, config: {} },
    });
    jest.mocked(requestProjectIdValidation).mockResolvedValue(
        {} as Awaited<ReturnType<typeof requestProjectIdValidation>>,
    );
    jest.mocked(requestStartProjectImport).mockResolvedValue(
        {} as Awaited<ReturnType<typeof requestStartProjectImport>>,
    );
    jest.mocked(requestProjectImportExecutionStatus).mockResolvedValue({
        data: { projectId: "existing-project", importStarted: 0, importEnded: 1, success: true },
    } as Awaited<ReturnType<typeof requestProjectImportExecutionStatus>>);
});

afterEach(() => jest.resetAllMocks());

const showImportOptions = async (details: Partial<IProjectImportDetails> = {}, selectCustom = true) => {
    jest.mocked(requestProjectImportDetails)
        .mockReset()
        .mockResolvedValue({
            data: {
                projectId: "archive-project",
                label: "Archive project",
                projectAlreadyExists: false,
                noAccess: false,
                ...details,
            },
        } as Awaited<ReturnType<typeof requestProjectImportDetails>>);
    render(
        <I18nextProvider i18n={i18n}>
            <ProjectImportModal close={jest.fn()} />
        </I18nextProvider>,
    );
    fireEvent.click(screen.getByRole("button", { name: "Upload invalid" }));
    await screen.findByText("Archive project");
    if (selectCustom) {
        fireEvent.click(screen.getByRole("radio", { name: "Use a custom project ID" }));
    }
};

it("allows another upload after the first file cannot be imported", async () => {
    render(
        <I18nextProvider i18n={i18n}>
            <ProjectImportModal close={jest.fn()} />
        </I18nextProvider>,
    );

    fireEvent.click(screen.getByRole("button", { name: "Upload invalid" }));
    expect(await screen.findByText(/Invalid archive/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Upload valid" }));

    await waitFor(() => expect(requestProjectImportDetails).toHaveBeenCalledWith("upload-2"));
    expect(await screen.findByText("Valid")).toBeInTheDocument();
    expect(requestDeleteProjectImport).toHaveBeenCalledWith("upload-1");
});

it("offers confirmed replacement when a custom project ID already exists", async () => {
    jest.mocked(requestProjectIdValidation).mockRejectedValue({ httpStatus: 409 });
    await showImportOptions();

    fireEvent.change(screen.getByLabelText("Project identifier"), { target: { value: "existing-project" } });
    await waitFor(() => expect(fetchProjectAccessControl).toHaveBeenCalledWith("existing-project"));
    await screen.findByRole("checkbox", { name: "Replace existing project" });
    expect(screen.getByRole("button", { name: "Open existing project page" })).toHaveAttribute(
        "href",
        "existing-project",
    );
    const replaceButton = screen.getByRole("button", { name: "Replace existing project" });
    expect(replaceButton).toBeDisabled();

    fireEvent.click(screen.getByRole("checkbox", { name: "Replace existing project" }));
    expect(replaceButton).toBeEnabled();
    fireEvent.click(replaceButton);
    await waitFor(() =>
        expect(requestStartProjectImport).toHaveBeenCalledWith("upload-1", false, true, undefined, "existing-project"),
    );
});

it("does not offer replacement of an inaccessible custom project", async () => {
    jest.mocked(requestProjectIdValidation).mockRejectedValue({ httpStatus: 409 });
    jest.mocked(fetchProjectAccessControl).mockRejectedValue({ httpStatus: 403 });
    await showImportOptions();

    fireEvent.change(screen.getByLabelText("Project identifier"), { target: { value: "private-project" } });
    await waitFor(() => expect(fetchProjectAccessControl).toHaveBeenCalledWith("private-project"));
    expect(await screen.findByText(/do not have permission to replace it/)).toBeInTheDocument();
    expect(screen.queryByRole("checkbox", { name: "Replace existing project" })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Replace existing project" })).toBeDisabled();
});

it("uses archive access when the custom ID matches the archive project ID", async () => {
    jest.mocked(requestProjectIdValidation).mockRejectedValue({ httpStatus: 409 });
    await showImportOptions({ projectAlreadyExists: true, noAccess: true });

    fireEvent.change(screen.getByLabelText("Project identifier"), { target: { value: "archive-project" } });
    expect(await screen.findByText(/do not have permission to replace it/)).toBeInTheDocument();
    expect(fetchProjectAccessControl).not.toHaveBeenCalled();
    expect(screen.queryByRole("checkbox", { name: "Replace existing project" })).not.toBeInTheDocument();
});

it("clears replacement approval when the custom project ID changes", async () => {
    jest.mocked(requestProjectIdValidation).mockImplementation((projectId) =>
        projectId === "existing-project"
            ? Promise.reject({ httpStatus: 409 })
            : Promise.resolve({} as Awaited<ReturnType<typeof requestProjectIdValidation>>),
    );
    await showImportOptions();

    const input = screen.getByLabelText("Project identifier");
    fireEvent.change(input, { target: { value: "existing-project" } });
    const checkbox = await screen.findByRole("checkbox", { name: "Replace existing project" });
    fireEvent.click(checkbox);
    fireEvent.change(input, { target: { value: "new-project" } });

    expect(screen.queryByRole("checkbox", { name: "Replace existing project" })).not.toBeInTheDocument();
    const importButton = screen.getByRole("button", { name: "Import project" });
    await waitFor(() => expect(importButton).toBeEnabled());
    fireEvent.click(importButton);
    await waitFor(() =>
        expect(requestStartProjectImport).toHaveBeenCalledWith("upload-1", false, false, undefined, "new-project"),
    );
});

it("keeps the original project ID replacement flow", async () => {
    await showImportOptions({ projectAlreadyExists: true }, false);

    const replaceButton = screen.getByRole("button", { name: "Replace existing project" });
    expect(replaceButton).toBeDisabled();
    fireEvent.click(screen.getByRole("checkbox", { name: "Replace existing project" }));
    fireEvent.click(replaceButton);

    await waitFor(() =>
        expect(requestStartProjectImport).toHaveBeenCalledWith("upload-1", false, true, undefined, undefined),
    );
    expect(fetchProjectAccessControl).not.toHaveBeenCalled();
});

it("offers replacement if a custom ID becomes occupied before import starts", async () => {
    jest.mocked(requestStartProjectImport).mockRejectedValueOnce({ httpStatus: 409 });
    jest.mocked(requestProjectIdValidation)
        .mockResolvedValueOnce({} as Awaited<ReturnType<typeof requestProjectIdValidation>>)
        .mockRejectedValueOnce({ httpStatus: 409 });
    await showImportOptions();

    fireEvent.change(screen.getByLabelText("Project identifier"), { target: { value: "late-collision" } });
    const importButton = screen.getByRole("button", { name: "Import project" });
    await waitFor(() => expect(importButton).toBeEnabled());
    fireEvent.click(importButton);

    const checkbox = await screen.findByRole("checkbox", { name: "Replace existing project" });
    const replaceButton = screen.getByRole("button", { name: "Replace existing project" });
    expect(replaceButton).toBeDisabled();
    fireEvent.click(checkbox);
    fireEvent.click(replaceButton);

    await waitFor(() =>
        expect(requestStartProjectImport).toHaveBeenLastCalledWith(
            "upload-1",
            false,
            true,
            undefined,
            "late-collision",
        ),
    );
});

it("does not offer replacement for an unrelated import conflict", async () => {
    jest.mocked(requestStartProjectImport).mockRejectedValueOnce({ httpStatus: 409 });
    await showImportOptions();

    fireEvent.change(screen.getByLabelText("Project identifier"), { target: { value: "available-project" } });
    const importButton = screen.getByRole("button", { name: "Import project" });
    await waitFor(() => expect(importButton).toBeEnabled());
    fireEvent.click(importButton);

    expect(await screen.findByText("Project could not be imported.")).toBeInTheDocument();
    expect(screen.queryByRole("checkbox", { name: "Replace existing project" })).not.toBeInTheDocument();
});

it("blocks replacement when custom ID access cannot be checked", async () => {
    jest.mocked(requestProjectIdValidation).mockRejectedValue({ httpStatus: 409 });
    jest.mocked(fetchProjectAccessControl).mockRejectedValue({ httpStatus: 503 });
    await showImportOptions();

    fireEvent.change(screen.getByLabelText("Project identifier"), { target: { value: "existing-project" } });
    expect(
        await screen.findByText("The project ID could not be validated. Edit the ID to try again."),
    ).toBeInTheDocument();
    expect(screen.queryByRole("checkbox", { name: "Replace existing project" })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Import project" })).toBeDisabled();
});
