import React from "react";
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { createTestI18n } from "../../../../test/createTestI18n";
import { I18nextProvider } from "react-i18next";
import { completeRequest, ControlledXMLHttpRequest } from "@eccenca/gui-elements/src/components/FileUpload/testHelpers";
import {
    requestDeleteProjectImport,
    requestProjectImportDetails,
    requestProjectImportExecutionStatus,
    requestStartProjectImport,
} from "@ducks/workspace/requests";
import { IProjectImportDetails } from "@ducks/workspace/typings";
import { routerOp } from "@ducks/router";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import translations from "../../../../locales/manual/en.json";
import { ProjectImportModal } from "./ProjectImportModal";

const mockDispatch = jest.fn();
jest.mock("react-redux", () => ({ ...jest.requireActual("react-redux"), useDispatch: () => mockDispatch }));
jest.mock("../../../utils/getApiEndpoint", () => ({ workspaceApi: (path: string) => `/api/workspace${path}` }));
jest.mock("@ducks/router", () => ({ routerOp: { goToPage: jest.fn((path) => ({ type: "navigate", path })) } }));
jest.mock("@ducks/workspace/requests", () => ({
    requestDeleteProjectImport: jest.fn(),
    requestProjectImportDetails: jest.fn(),
    requestProjectImportExecutionStatus: jest.fn(),
    requestStartProjectImport: jest.fn(),
}));
jest.mock("../../../hooks/useProjectAclManagementComponent", () => {
    const MockReact = require("react");
    return {
        useProjectAclManagementComponent: ({ onChange }) => ({
            component: MockReact.createElement(
                "button",
                { onClick: () => onChange({ groups: ["editors"] }) },
                "Set access groups",
            ),
        }),
    };
});

const getDetails = jest.mocked(requestProjectImportDetails);
const startImport = jest.mocked(requestStartProjectImport);
const getStatus = jest.mocked(requestProjectImportExecutionStatus);
const deleteImport = jest.mocked(requestDeleteProjectImport);
const response = <T,>(data: T): FetchResponse<T> => ({
    data,
    axiosResponse: { data, status: 200, statusText: "OK", headers: {}, config: {} },
});
const details: IProjectImportDetails = {
    projectId: "project",
    label: "Imported project",
    projectAlreadyExists: false,
    noAccess: false,
};
const deferred = <T,>() => {
    let resolve!: (value: T) => void;
    const promise = new Promise<T>((done) => {
        resolve = done;
    });
    return { promise, resolve };
};
const i18n = createTestI18n();
void i18n.init({
    lng: "en",
    resources: { en: { translation: translations } },
    initImmediate: false,
    interpolation: { escapeValue: false },
});
const renderModal = (props: Partial<React.ComponentProps<typeof ProjectImportModal>> = {}) => {
    const close = jest.fn(),
        back = jest.fn();
    return {
        close,
        back,
        ...render(
            <I18nextProvider i18n={i18n}>
                <ProjectImportModal close={close} back={back} {...props} />
            </I18nextProvider>,
        ),
    };
};
const select = (...names: string[]) =>
    fireEvent.change(document.querySelector('input[type="file"]')!, {
        target: { files: names.map((name) => new File(["archive"], name, { type: "application/zip" })) },
    });
const request = async (index = 0) => {
    await waitFor(() => expect(ControlledXMLHttpRequest.requests[index]?.sent).toBe(true));
    return ControlledXMLHttpRequest.requests[index];
};
const upload = async () => {
    select("project.zip");
    await completeRequest(await request(), 201, JSON.stringify({ projectImportId: "upload-id" }));
};

beforeEach(() => {
    jest.clearAllMocks();
    getDetails.mockResolvedValue(response(details));
    startImport.mockResolvedValue(response(undefined));
    deleteImport.mockResolvedValue(response(undefined));
    getStatus.mockResolvedValue(
        response({ projectId: "imported-project", importStarted: 1, importEnded: 2, success: true }),
    );
});

it("names the shared upload widget independently of the dialog", () => {
    renderModal();
    expect(screen.getByRole("group", { name: translations.ProjectImportModal.projectFile })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /browse/i })).toBeInTheDocument();
    expect(document.querySelector('input[type="file"]')).not.toHaveAttribute("multiple");
});

it("uploads multipart to the project endpoint, parses the id and analyses once without starting import", async () => {
    const append = jest.spyOn(FormData.prototype, "append");
    renderModal();
    await upload();
    const xhr = await request();
    expect(xhr.method.toUpperCase()).toBe("POST");
    expect(xhr.url).toMatch(/\/workspace\/projectImport$/);
    expect(append).toHaveBeenCalledWith(
        "file",
        expect.objectContaining({ size: 7, type: "application/zip" }),
        "project.zip",
    );
    expect(await screen.findByText("Imported project")).toBeInTheDocument();
    expect(getDetails).toHaveBeenCalledTimes(1);
    expect(getDetails).toHaveBeenCalledWith("upload-id");
    expect(startImport).not.toHaveBeenCalled();
});

it.each(["not json", "{}", "null", '{"projectImportId":42}', '{"projectImportId":""}'])(
    "rejects invalid upload response %s and permits a retry",
    async (body) => {
        renderModal();
        select("project.zip");
        await completeRequest(await request(), 201, body);
        expect(screen.getByRole("alert")).toHaveTextContent(translations.ProjectImportModal.responseInvalid);
        expect(getDetails).not.toHaveBeenCalled();
        fireEvent.click(screen.getByRole("button", { name: /^retry$/i }));
        await completeRequest(await request(1), 201, '{"projectImportId":"retry-id"}');
        await screen.findByText("Imported project");
        expect(getDetails).toHaveBeenCalledWith("retry-id");
    },
);

it("enforces size and single-file restrictions before sending an upload", () => {
    const { rerender } = renderModal({ maxFileUploadSizeBytes: 2 });
    select("large.zip");
    expect(screen.getByRole("alert")).toHaveTextContent("maximum file size of 2 bytes");
    rerender(
        <I18nextProvider i18n={i18n}>
            <ProjectImportModal close={jest.fn()} />
        </I18nextProvider>,
    );
    select("one.zip", "two.zip");
    expect(screen.getByRole("alert")).toHaveTextContent("at most 1 incomplete files");
    expect(ControlledXMLHttpRequest.requests).toHaveLength(0);
    expect(getDetails).not.toHaveBeenCalled();
});

it("shows transport errors inline and retries through the shared uploader", async () => {
    renderModal();
    select("project.zip");
    // Exhaust the transport's automatic retries before asserting the terminal error and manual retry.
    for (let attempt = 0; attempt < 4; attempt++) await completeRequest(await request(attempt), 400, "Bad archive");
    expect(screen.getByRole("alert")).toHaveTextContent("project.zip");
    expect(getDetails).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: /^retry$/i }));
    await completeRequest(await request(4), 201, '{"projectImportId":"upload-id"}');
    await screen.findByText("Imported project");
});

it("retries analysis without uploading the archive again", async () => {
    getDetails.mockRejectedValueOnce(new Error("Analysis unavailable"));
    renderModal();
    await upload();
    await screen.findByText(/Analysis unavailable/);
    fireEvent.click(screen.getByRole("button", { name: /^retry$/i }));
    await screen.findByText("Imported project");
    expect(getDetails).toHaveBeenCalledTimes(2);
    expect(ControlledXMLHttpRequest.requests).toHaveLength(1);
});

it("shows archive analysis errors without offering import", async () => {
    getDetails.mockResolvedValue(response({ ...details, errorMessage: "Invalid project archive" }));
    renderModal();
    await upload();
    await screen.findByText(/Invalid project archive/);
    expect(screen.queryByRole("button", { name: /^Import project$/ })).not.toBeInTheDocument();
    expect(startImport).not.toHaveBeenCalled();
});

it.each(["new", "fresh-id", "replace"])("preserves %s import decisions, ACL and navigation", async (mode) => {
    getDetails.mockResolvedValue(response({ ...details, projectAlreadyExists: mode !== "new" }));
    const { close } = renderModal();
    await upload();
    fireEvent.click(await screen.findByRole("button", { name: "Set access groups" }));
    if (mode === "replace") fireEvent.click(screen.getByRole("checkbox", { name: "Replace existing project" }));
    const label =
        mode === "replace"
            ? "Replace existing project"
            : mode === "fresh-id"
              ? "Import as new project"
              : "Import project";
    fireEvent.click(screen.getByRole("button", { name: label }));
    await waitFor(() => expect(close).toHaveBeenCalledTimes(1));
    expect(startImport).toHaveBeenCalledWith(
        "upload-id",
        mode === "fresh-id",
        mode === "replace",
        mode === "replace" ? undefined : ["editors"],
    );
    expect(routerOp.goToPage).toHaveBeenCalledWith(expect.stringContaining("/projects/imported-project"));
    expect(mockDispatch).toHaveBeenCalledTimes(1);
});

it.each(["Cancel", "Back"])("%s aborts the active upload and ignores its late response", async (action) => {
    const callbacks = renderModal();
    select("project.zip");
    const xhr = await request();
    fireEvent.click(screen.getByRole("button", { name: action }));
    await waitFor(() => expect(action === "Cancel" ? callbacks.close : callbacks.back).toHaveBeenCalledTimes(1));
    expect(xhr.aborted).toBe(true);
    await completeRequest(xhr, 201, '{"projectImportId":"late-id"}');
    expect(getDetails).not.toHaveBeenCalled();
    expect(deleteImport).not.toHaveBeenCalled();
});

it.each(["Cancel", "Back"])(
    "%s cleans up the archive and ignores late analysis even if deletion fails",
    async (action) => {
        const pending = deferred<Awaited<ReturnType<typeof requestProjectImportDetails>>>();
        getDetails.mockReturnValueOnce(pending.promise);
        deleteImport.mockRejectedValueOnce(new Error("Cleanup unavailable"));
        const callbacks = renderModal();
        await upload();
        fireEvent.click(screen.getByRole("button", { name: action }));
        await waitFor(() => expect(action === "Cancel" ? callbacks.close : callbacks.back).toHaveBeenCalledTimes(1));
        expect(deleteImport).toHaveBeenCalledWith("upload-id");
        await act(async () => pending.resolve(response(details)));
        expect(screen.queryByText("Imported project")).not.toBeInTheDocument();
        expect(mockDispatch).not.toHaveBeenCalled();
    },
);

it("aborts active upload on unmount", async () => {
    const { unmount } = renderModal();
    select("project.zip");
    const xhr = await request();
    unmount();
    await act(async () => {});
    expect(xhr.aborted).toBe(true);
    await completeRequest(xhr, 201, '{"projectImportId":"late-id"}');
    expect(getDetails).not.toHaveBeenCalled();
});

it("keeps import disabled while Cancel waits for archive cleanup", async () => {
    const pending = deferred<Awaited<ReturnType<typeof requestDeleteProjectImport>>>();
    deleteImport.mockReturnValueOnce(pending.promise);
    const { close } = renderModal();
    await upload();
    await screen.findByRole("button", { name: "Import project" });
    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
    expect(screen.getByRole("button", { name: "Import project" })).toBeDisabled();
    expect(close).not.toHaveBeenCalled();
    await act(async () => pending.resolve(response(undefined)));
    expect(close).toHaveBeenCalledTimes(1);
});

it("does not navigate or poll after cancelling a pending import start", async () => {
    const pending = deferred<Awaited<ReturnType<typeof requestStartProjectImport>>>();
    startImport.mockReturnValueOnce(pending.promise);
    const { close } = renderModal();
    await upload();
    fireEvent.click(await screen.findByRole("button", { name: "Import project" }));
    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
    await waitFor(() => expect(close).toHaveBeenCalledTimes(1));
    await act(async () => pending.resolve(response(undefined)));
    expect(getStatus).not.toHaveBeenCalled();
    expect(mockDispatch).not.toHaveBeenCalled();
});

it("retries failed import execution with the same options without reuploading", async () => {
    getStatus.mockResolvedValueOnce(
        response({
            projectId: "project",
            importStarted: 1,
            importEnded: 2,
            success: false,
            failureMessage: "Import failed",
        }),
    );
    const { close } = renderModal();
    await upload();
    fireEvent.click(await screen.findByRole("button", { name: "Import project" }));
    await screen.findByText(/Import failed/);
    fireEvent.click(screen.getByRole("button", { name: /^retry$/i }));
    await waitFor(() => expect(close).toHaveBeenCalledTimes(1));
    expect(startImport).toHaveBeenCalledTimes(2);
    expect(startImport).toHaveBeenLastCalledWith("upload-id", false, false, undefined);
    expect(ControlledXMLHttpRequest.requests).toHaveLength(1);
});

it("uploads and analyses once after StrictMode effect replay", async () => {
    render(
        <React.StrictMode>
            <I18nextProvider i18n={i18n}>
                <ProjectImportModal close={jest.fn()} />
            </I18nextProvider>
        </React.StrictMode>,
    );
    await upload();
    await screen.findByText("Imported project");
    expect(getDetails).toHaveBeenCalledTimes(1);
    expect(ControlledXMLHttpRequest.requests).toHaveLength(1);
});

it.each(["Cancel", "unmount"])("%s stops polling during retry backoff", async (action) => {
    getStatus.mockRejectedValueOnce(new Error("Status unavailable"));
    const { close, unmount } = renderModal();
    await upload();
    const start = await screen.findByRole("button", { name: "Import project" });
    jest.useFakeTimers();
    try {
        await act(async () => {
            fireEvent.click(start);
        });
        expect(getStatus).toHaveBeenCalledTimes(1);
        await act(async () => {
            if (action === "Cancel") fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
            else unmount();
        });
        await act(async () => {
            jest.advanceTimersByTime(120_000);
        });
        expect(getStatus).toHaveBeenCalledTimes(1);
        expect(mockDispatch).not.toHaveBeenCalled();
        expect(close).toHaveBeenCalledTimes(action === "Cancel" ? 1 : 0);
    } finally {
        unmount();
        jest.useRealTimers();
    }
});
