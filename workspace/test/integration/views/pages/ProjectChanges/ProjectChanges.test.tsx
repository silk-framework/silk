import React from "react";
import "@testing-library/jest-dom";
import { RenderResult, waitFor } from "@testing-library/react";
import mockAxios from "../../../../__mocks__/axios";
import {
    apiUrl,
    byTestId,
    checkRequestMade,
    clickFoundElement,
    findElement,
    mockedAxiosResponse,
    renderWrapper,
} from "../../../TestHelper";
import ChangeList from "../../../../../src/app/views/pages/ProjectChanges/ChangeList";
import { IChangeEntry } from "../../../../../src/app/views/pages/ProjectChanges/changesRequests";

describe("Project changes", () => {
    afterEach(() => {
        mockAxios.reset();
    });

    const PROJECT_ID = "cmem";
    const changesUrl = apiUrl(`/workspace/projects/${PROJECT_ID}/changes`);
    const revertUrl = (seq: number) => apiUrl(`/workspace/projects/${PROJECT_ID}/changes/${seq}/revert`);

    const changes: IChangeEntry[] = [
        {
            seq: 3,
            timestamp: "2026-08-26T09:51:02.417Z",
            user: "urn:user:alice",
            origin: "mcp:claude-code",
            type: "AddMapping",
            description: "Added mapping rule 'name' under 'root' in transform 'persons'",
            revertible: true,
        },
        {
            seq: 2,
            timestamp: "2026-08-26T09:50:12.345Z",
            user: "urn:user:alice",
            type: "ReplaceTask",
            description: "Updated task 'persons'",
            revertible: true,
            revertedBy: 3,
        },
        {
            seq: 1,
            timestamp: "2026-08-26T09:49:58.001Z",
            user: "urn:user:alice",
            type: "WorkflowExecuted",
            description: "Executed workflow 'workflow'",
            revertible: false,
        },
    ];

    const loadChangeList = async (): Promise<RenderResult> => {
        const wrapper = renderWrapper(<ChangeList projectId={PROJECT_ID} />);
        mockAxios.mockResponseFor({ url: changesUrl }, mockedAxiosResponse({ data: { changes } }));
        await waitFor(() => {
            expect(wrapper.container.querySelectorAll("tbody tr")).toHaveLength(changes.length);
        });
        return wrapper;
    };

    it("should list all changes with their descriptions", async () => {
        const wrapper = await loadChangeList();
        changes.forEach((change) => {
            expect(wrapper.container.textContent).toContain(change.description);
        });
        expect(wrapper.container.textContent).toContain("mcp:claude-code");
    });

    it("should only offer to revert changes that are revertible and not reverted already", async () => {
        const wrapper = await loadChangeList();
        expect(findElement(wrapper, byTestId("change-revert-btn-3"))).not.toBeDisabled();
        expect(findElement(wrapper, byTestId("change-revert-btn-2"))).toBeDisabled();
        expect(findElement(wrapper, byTestId("change-revert-btn-1"))).toBeDisabled();
    });

    it("should revert a change after confirmation and reload the list", async () => {
        const wrapper = await loadChangeList();
        clickFoundElement(wrapper, byTestId("change-revert-btn-3"));
        await waitFor(() => {
            expect(findElement(document.body, byTestId("remove-item-button"))).toBeInTheDocument();
        });
        clickFoundElement(document.body, byTestId("remove-item-button"));
        await waitFor(() => {
            checkRequestMade(revertUrl(3), "POST");
        });
        mockAxios.mockResponseFor(
            { url: revertUrl(3) },
            mockedAxiosResponse({
                data: {
                    seq: 4,
                    timestamp: "2026-08-26T09:52:00.000Z",
                    type: "RemoveMapping",
                    revertible: true,
                    reverts: 3,
                },
            }),
        );
        // The list is reloaded after the revert
        await waitFor(() => {
            expect(mockAxios.queue().length).toBeGreaterThan(0);
        });
        mockAxios.mockResponseFor({ url: changesUrl }, mockedAxiosResponse({ data: { changes } }));
        await waitFor(() => {
            expect(document.body.querySelector(byTestId("remove-item-button"))).not.toBeInTheDocument();
        });
    });
});
