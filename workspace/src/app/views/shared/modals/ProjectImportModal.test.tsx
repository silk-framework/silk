import React from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { createInstance } from "i18next";
import { I18nextProvider } from "react-i18next";
import { ProjectImportModal } from "./ProjectImportModal";
import { requestDeleteProjectImport, requestProjectImportDetails } from "@ducks/workspace/requests";
import translations from "../../../../locales/manual/en.json";

jest.mock("react-redux", () => ({ ...jest.requireActual("react-redux"), useDispatch: () => jest.fn() }));
jest.mock("../../../utils/getApiEndpoint", () => ({ workspaceApi: (path: string) => path }));
jest.mock("@ducks/router", () => ({ routerOp: { goToPage: jest.fn() } }));
jest.mock("../../../utils/routerUtils", () => ({ absoluteProjectPath: (projectId: string) => projectId }));
jest.mock("../../../hooks/useProjectAclManagementComponent", () => ({
    useProjectAclManagementComponent: () => ({ component: null }),
}));
jest.mock("@ducks/workspace/requests", () => ({
    requestProjectImportDetails: jest.fn(),
    requestDeleteProjectImport: jest.fn(),
}));
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
});

afterEach(() => jest.clearAllMocks());

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
