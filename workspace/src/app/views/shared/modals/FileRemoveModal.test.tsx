import React from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { I18nextProvider } from "react-i18next";

import { projectFileResourceDependents, requestRemoveProjectResource } from "@ducks/workspace/requests";
import translations from "../../../../locales/manual/en.json";
import { createTestI18n } from "../../../../test/createTestI18n";
import { FileRemoveModal } from "./FileRemoveModal";

jest.mock("@ducks/workspace/requests", () => ({
    projectFileResourceDependents: jest.fn(),
    requestRemoveProjectResource: jest.fn(),
}));

const i18n = createTestI18n();
void i18n.init({ lng: "en", resources: { en: { translation: translations } }, interpolation: { escapeValue: false } });

beforeEach(() => {
    jest.clearAllMocks();
    jest.mocked(projectFileResourceDependents).mockResolvedValue({
        data: [],
        axiosResponse: { data: [], status: 200, statusText: "OK", headers: {}, config: {} },
    });
    jest.mocked(requestRemoveProjectResource).mockResolvedValue(undefined);
});

it.each([undefined, "folder/resource.ttl"])("deletes a plain project resource using its path: %s", async (fullPath) => {
    const onConfirm = jest.fn();
    render(
        <I18nextProvider i18n={i18n}>
            <FileRemoveModal
                projectId="project"
                file={{ id: "resource-id", name: "resource.ttl", fullPath }}
                onConfirm={onConfirm}
            />
        </I18nextProvider>,
    );
    await waitFor(() =>
        expect(projectFileResourceDependents).toHaveBeenCalledWith("project", fullPath ?? "resource.ttl"),
    );
    fireEvent.click(screen.getByRole("button", { name: /^Delete$/ }));
    await waitFor(() => expect(onConfirm).toHaveBeenCalledWith("resource-id"));
    expect(requestRemoveProjectResource).toHaveBeenCalledWith("project", fullPath ?? "resource.ttl");
});

it("closes without deleting the resource", async () => {
    const onConfirm = jest.fn();
    render(
        <I18nextProvider i18n={i18n}>
            <FileRemoveModal
                projectId="project"
                file={{ id: "resource-id", name: "resource.ttl" }}
                onConfirm={onConfirm}
            />
        </I18nextProvider>,
    );
    await waitFor(() => expect(projectFileResourceDependents).toHaveBeenCalled());
    fireEvent.click(screen.getByRole("button", { name: /^Cancel$/ }));
    expect(onConfirm).toHaveBeenCalledWith();
    expect(requestRemoveProjectResource).not.toHaveBeenCalled();
});
