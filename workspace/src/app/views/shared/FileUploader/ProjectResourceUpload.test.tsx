import React from "react";
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import i18next from "i18next";
import { I18nextProvider } from "react-i18next";
import { SimpleDialog } from "@eccenca/gui-elements";
import { completeRequest, ControlledXMLHttpRequest } from "@eccenca/gui-elements/src/components/FileUpload/testHelpers";
import { requestIfResourceExists } from "@ducks/workspace/requests";
import translations from "../../../../locales/manual/en.json";

import ProjectResourceUpload, { ProjectResourceUploadHandle } from "./ProjectResourceUpload";

jest.mock("@ducks/workspace/requests", () => ({ requestIfResourceExists: jest.fn() }));
const resourceExists = requestIfResourceExists as jest.MockedFunction<typeof requestIfResourceExists>;
const i18n = i18next.createInstance();
void i18n.init({
    lng: "en",
    resources: { en: { translation: translations } },
    initImmediate: false,
    interpolation: { escapeValue: false },
});

const select = (...names: string[]) =>
    fireEvent.change(document.querySelector('input[type="file"]')!, {
        target: { files: names.map((name) => new File(["same"], name)) },
    });
const request = async (index: number) => {
    await waitFor(() => expect(ControlledXMLHttpRequest.requests[index]?.sent).toBe(true));
    return ControlledXMLHttpRequest.requests[index];
};
const renderUpload = (props: Partial<React.ComponentProps<typeof ProjectResourceUpload>> = {}) =>
    render(
        <I18nextProvider i18n={i18n}>
            <ProjectResourceUpload projectId="project with spaces" allowMultiple {...props} />
        </I18nextProvider>,
    );

beforeEach(() => {
    jest.clearAllMocks();
    resourceExists.mockResolvedValue(false);
});

describe("ProjectResourceUpload with the real FileUpload", () => {
    it("checks resources, sends PUT to encoded paths and preserves the success announcement", async () => {
        const success = jest.fn(),
            uploaded = jest.fn(),
            allSuccessful = jest.fn();
        renderUpload({
            onUploadSuccess: success,
            listenToUploadedFiles: uploaded,
            allFilesSuccessfullyUploadedHandler: allSuccessful,
        });
        select("folder/first file.ttl", "second.ttl");
        const first = await request(0);
        expect(resourceExists).toHaveBeenCalledWith("project with spaces", "folder/first file.ttl");
        expect(first.method.toUpperCase()).toBe("PUT");
        expect(first.url).toContain("?path=folder%2Ffirst%20file.ttl");
        await completeRequest(first, 200, "");
        expect(screen.getByRole("status")).toHaveTextContent("folder/first file.ttl was successfully uploaded");
        await completeRequest(await request(1), 200, "");
        expect(success).toHaveBeenCalledTimes(2);
        expect(uploaded).toHaveBeenLastCalledWith(
            expect.arrayContaining([
                expect.objectContaining({ name: "folder/first file.ttl" }),
                expect.objectContaining({ name: "second.ttl" }),
            ]),
        );
        expect(allSuccessful).toHaveBeenLastCalledWith(true);
        expect(screen.getByText("2 of 2 files completed")).toBeInTheDocument();
    });

    it("uploads new files while replacements await confirmation, without a consumer queue", async () => {
        resourceExists.mockImplementation(async (_project, name) => name === "existing.ttl");
        const allSuccessful = jest.fn();
        renderUpload({ allFilesSuccessfullyUploadedHandler: allSuccessful });
        select("existing.ttl", "new.ttl");
        const first = await request(0);
        expect(first.url).toContain("path=new.ttl");
        await screen.findByText(/existing.ttl.*already exists/);
        await completeRequest(first, 200, "");
        expect(allSuccessful).toHaveBeenLastCalledWith(false);
        fireEvent.click(screen.getByRole("button", { name: /^replace$/i }));
        const second = await request(1);
        expect(second.url).toContain("path=existing.ttl");
        await completeRequest(second, 200, "");
        expect(allSuccessful).toHaveBeenLastCalledWith(true);
        expect(resourceExists).toHaveBeenCalledTimes(2);
    });

    it("declines replacements without uploading and separates multiple prompts", async () => {
        resourceExists.mockResolvedValue(true);
        renderUpload();
        select("first.ttl", "second.ttl");
        await waitFor(() => expect(screen.getAllByRole("button", { name: /cancel replacement/i })).toHaveLength(2));
        expect(screen.getAllByTestId("replacement-notification-spacing")).toHaveLength(1);
        for (const button of screen.getAllByRole("button", { name: /cancel replacement/i })) fireEvent.click(button);
        await waitFor(() => expect(screen.queryByText("first.ttl")).not.toBeInTheDocument());
        expect(ControlledXMLHttpRequest.requests).toHaveLength(0);
    });

    it("retries resource-check failures through the shared component", async () => {
        resourceExists.mockRejectedValueOnce(new Error("Unavailable")).mockResolvedValue(false);
        renderUpload();
        select("check.ttl");
        expect(await screen.findByRole("alert")).toHaveTextContent(
            "The selected files could not be checked: Unavailable",
        );
        fireEvent.click(screen.getByRole("button", { name: /^retry$/i }));
        await completeRequest(await request(0), 200, "");
        expect(resourceExists).toHaveBeenCalledTimes(2);
    });

    it("cancels a pending check and closes pending replacement prompts on reset", async () => {
        let resolveCheck!: (exists: boolean) => void;
        resourceExists.mockImplementationOnce(
            () =>
                new Promise((resolve) => {
                    resolveCheck = resolve;
                }),
        );
        const ref = React.createRef<ProjectResourceUploadHandle>();
        renderUpload({ ref });
        select("late.ttl");
        await waitFor(() => expect(resourceExists).toHaveBeenCalled());
        act(() => ref.current!.reset());
        await act(async () => resolveCheck(false));
        expect(ControlledXMLHttpRequest.requests).toHaveLength(0);
        resourceExists.mockResolvedValue(true);
        select("existing.ttl");
        await screen.findByRole("button", { name: /^replace$/i });
        act(() => ref.current!.cancelAll());
        expect(screen.queryByRole("button", { name: /^replace$/i })).not.toBeInTheDocument();
    });

    it("retains cancelled progress, removes a row and continues only remaining files inside a dialog", async () => {
        const activity = jest.fn();
        render(
            <I18nextProvider i18n={i18n}>
                <SimpleDialog isOpen title="Project resources">
                    <ProjectResourceUpload projectId="project" allowMultiple onUploadStateChange={activity} />
                </SimpleDialog>
            </I18nextProvider>,
        );
        select("a.ttl", "b.ttl", "c.ttl");
        await completeRequest(await request(0), 200, "");
        await request(1);
        fireEvent.click(screen.getByRole("button", { name: "Stop uploads" }));
        expect(activity).toHaveBeenLastCalledWith(false);
        expect(screen.getByRole("progressbar", { name: "Overall upload progress" })).toHaveAttribute(
            "aria-valuenow",
            "33",
        );
        fireEvent.click(screen.getByRole("button", { name: "Remove", description: "b.ttl" }));
        expect(screen.getByRole("progressbar", { name: "Overall upload progress" })).toHaveAttribute(
            "aria-valuenow",
            "50",
        );
        fireEvent.click(screen.getByRole("button", { name: "Continue uploads" }));
        const remaining = await request(2);
        expect(remaining.url).toContain("path=c.ttl");
        await completeRequest(remaining, 200, "");
        expect(ControlledXMLHttpRequest.requests.map((xhr) => xhr.method.toUpperCase())).toEqual(["PUT", "PUT", "PUT"]);
        expect(resourceExists).toHaveBeenCalledTimes(3);
    });

    it("preserves single-file limits and size restrictions before resource checks", async () => {
        renderUpload({ allowMultiple: false, maxFileUploadSizeBytes: 2 });
        select("too-large.ttl");
        expect(screen.getByRole("alert")).toBeInTheDocument();
        expect(resourceExists).not.toHaveBeenCalled();
        expect(ControlledXMLHttpRequest.requests).toHaveLength(0);
    });
});
