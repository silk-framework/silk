/**
 * Tests for the ProjectTaskTabView URL↔tab synchronization and the unified unsaved-changes
 * prompt (PromptModal) paths:
 *
 * - browser back/forward (popstate) to a sibling tab bookmark
 * - deep links with query parameters arriving while the current tab holds unsaved changes
 *   (these pass `history.block` silently and are prompted by the URL→tab sync effect instead)
 * - the URL restore after such a prompt is declined (must not re-prompt and must keep the
 *   unsaved-changes flag)
 * - query-param-only URL changes (including clearing the params), which must never prompt
 * - in-app tab clicks with and without unsaved changes
 * - closing the fullscreen-modal embedding with and without unsaved changes
 * - full-page navigations intercepted by `history.block` and routed through the same
 *   PromptModal via the custom `getUserConfirmation` (never `window.confirm`)
 *
 * The component is rendered with a `createBrowserHistory({ getUserConfirmation })`, mirroring
 * the production history setup in `src/app/store/configureStore.ts`.
 */
import React from "react";
import "@testing-library/jest-dom";
import mockAxios from "../../../../__mocks__/axios";
import { apiUrl, byTestId, mockedAxiosResponse, renderWrapper, setUseParams, workspacePath } from "../../../TestHelper";
import { act, fireEvent, RenderResult, waitFor } from "@testing-library/react";
import { createBrowserHistory, History } from "history";
import { ProjectTaskTabView } from "../../../../../src/app/views/shared/projectTaskTabView/ProjectTaskTabView";
import { getUserConfirmation } from "../../../../../src/app/views/shared/projectTaskTabView/unsavedChangesConfirmation";
import { IProjectTaskView, IViewActions, pluginRegistry } from "../../../../../src/app/views/plugins/PluginRegistry";

describe("ProjectTaskTabView", () => {
    const projectId = "testProject";
    const taskId = "testTask";
    // Unique plugin ID so the registered test views cannot collide with core task view plugins.
    const testPluginId = "projectTaskTabViewTestPlugin";
    const basePath = workspacePath(`/projects/${projectId}/task/${taskId}`);
    const itemLinksUrl = apiUrl(`/workspace/projects/${projectId}/tasks/${taskId}/links`);

    /** viewActions of the currently rendered task view. Captured so tests can toggle the
     * unsaved-changes flag exactly like an embedded editor (e.g. the mapping editor) does. */
    let capturedViewActions: IViewActions | undefined;

    const testView = (id: string, label: string): IProjectTaskView => ({
        id,
        label,
        render: (renderProjectId, renderTaskId, viewActions) => {
            capturedViewActions = viewActions;
            return <div data-test-id={`view-content-${id}`}>{label}</div>;
        },
    });

    beforeAll(() => {
        setUseParams(projectId, taskId);
        pluginRegistry.registerTaskView(testPluginId, testView("viewA", "View A"));
        pluginRegistry.registerTaskView(testPluginId, testView("viewB", "View B"));
    });

    let wrapper: RenderResult | null = null;
    let confirmSpy: jest.SpyInstance;

    beforeEach(() => {
        capturedViewActions = undefined;
        // The unified prompt paths must NEVER fall back to the native confirm.
        confirmSpy = jest.spyOn(window, "confirm").mockReturnValue(true);
    });

    afterEach(() => {
        wrapper?.unmount();
        wrapper = null;
        mockAxios.reset();
        confirmSpy.mockRestore();
        window.onbeforeunload = null;
        window.history.replaceState({}, "", "/");
    });

    /** Renders the tab view at `${basePath}/viewA` and waits until view A is shown. */
    const renderTabView = async (extraProps: Partial<React.ComponentProps<typeof ProjectTaskTabView>> = {}) => {
        // The component derives the selected tab from window.location (getBookmark), so the real
        // URL must match the history location.
        window.history.replaceState({}, "", `${basePath}/viewA`);
        const history: History = createBrowserHistory({ getUserConfirmation });
        wrapper = renderWrapper(
            <ProjectTaskTabView
                taskViewConfig={{ pluginId: testPluginId, projectId: projectId, taskId: taskId }}
                {...extraProps}
            />,
            history,
        );
        await waitFor(() => expect(mockAxios.getReqByUrl(itemLinksUrl)).toBeTruthy());
        mockAxios.mockResponseFor(itemLinksUrl, mockedAxiosResponse({ data: [] }));
        await waitFor(() => expect(viewContent("viewA")).toBeInTheDocument());
        return history;
    };

    /** The rendered content of a task view (only the selected view renders). */
    const viewContent = (viewId: string): Element | null => document.querySelector(byTestId(`view-content-${viewId}`));

    /** The tab anchor in the TabBar. */
    const tabAnchor = (viewId: string): HTMLElement => {
        const anchor = document.querySelector(byTestId(`taskView-${viewId}`));
        expect(anchor).not.toBeNull();
        return anchor as HTMLElement;
    };

    /** The unsaved-changes PromptModal (portal-rendered, thus queried on the document). */
    const promptModal = (): Element | null => document.querySelector(byTestId("project-tab-prompt-modal"));

    const expectPromptModal = async () => {
        await waitFor(() => expect(promptModal()).toBeInTheDocument());
    };

    const expectNoPromptModal = async () => {
        await waitFor(() => expect(promptModal()).toBeNull());
    };

    /** Clicks the proceed / cancel button of the open PromptModal. Async act so the Promise
     * continuations behind the modal decision (requestConfirmation) run inside act. */
    const answerPrompt = async (buttonId: "prompt-proceed" | "prompt-cancel") => {
        const button = document.querySelector(`#${buttonId}`);
        expect(button).not.toBeNull();
        await act(async () => {
            fireEvent.click(button as HTMLElement);
        });
    };

    /** Marks the currently rendered view as having unsaved changes (or not). */
    const setUnsavedChanges = (status: boolean) => {
        expect(capturedViewActions?.unsavedChanges).toBeTruthy();
        act(() => {
            capturedViewActions!.unsavedChanges!(status);
        });
    };

    /** Simulates a browser back/forward (POP) navigation to the given URL: the browser has
     * already moved to the new entry when the `popstate` event fires. */
    const simulateBrowserPop = (url: string) => {
        act(() => {
            window.history.pushState({ key: "poptest", state: undefined }, "", url);
            window.dispatchEvent(new PopStateEvent("popstate", { state: { key: "poptest", state: undefined } }));
        });
    };

    it("should apply the tab of a sibling tab bookmark on browser back/forward when there are no unsaved changes", async () => {
        const history = await renderTabView();
        expect(tabAnchor("viewA").getAttribute("aria-selected")).toBe("true");
        // "Forward" to the sibling tab's bookmark URL
        simulateBrowserPop(`${basePath}/viewB`);
        await waitFor(() => expect(viewContent("viewB")).toBeInTheDocument());
        expect(viewContent("viewA")).toBeNull();
        expect(tabAnchor("viewB").getAttribute("aria-selected")).toBe("true");
        expect(history.location.pathname).toBe(`${basePath}/viewB`);
        // "Back" to the un-bookmarked base URL: returns to the default (first) tab
        simulateBrowserPop(basePath);
        await waitFor(() => expect(viewContent("viewA")).toBeInTheDocument());
        expect(tabAnchor("viewA").getAttribute("aria-selected")).toBe("true");
        // Clean navigation never prompts
        expect(promptModal()).toBeNull();
        expect(confirmSpy).not.toHaveBeenCalled();
    });

    it("should switch tabs on click without prompting when there are no unsaved changes", async () => {
        const history = await renderTabView();
        fireEvent.click(tabAnchor("viewB"));
        await waitFor(() => expect(viewContent("viewB")).toBeInTheDocument());
        expect(tabAnchor("viewB").getAttribute("aria-selected")).toBe("true");
        // The tab click rewrites the URL to the tab's bookmark
        expect(history.location.pathname).toBe(`${basePath}/viewB`);
        expect(promptModal()).toBeNull();
        expect(confirmSpy).not.toHaveBeenCalled();
    });

    it("should prompt on tab click with unsaved changes and keep tab, URL and dirty flag on decline", async () => {
        const history = await renderTabView();
        setUnsavedChanges(true);
        fireEvent.click(tabAnchor("viewB"));
        await expectPromptModal();
        await answerPrompt("prompt-cancel");
        await expectNoPromptModal();
        // Still on view A, URL untouched
        expect(viewContent("viewA")).toBeInTheDocument();
        expect(viewContent("viewB")).toBeNull();
        expect(history.location.pathname).toBe(`${basePath}/viewA`);
        // The unsaved-changes flag must survive the decline: the next tab click prompts again.
        fireEvent.click(tabAnchor("viewB"));
        await expectPromptModal();
        expect(confirmSpy).not.toHaveBeenCalled();
    });

    it("should prompt on tab click with unsaved changes and switch tab and clear the dirty flag on accept", async () => {
        const history = await renderTabView();
        setUnsavedChanges(true);
        fireEvent.click(tabAnchor("viewB"));
        await expectPromptModal();
        await answerPrompt("prompt-proceed");
        await waitFor(() => expect(viewContent("viewB")).toBeInTheDocument());
        await expectNoPromptModal();
        expect(history.location.pathname).toBe(`${basePath}/viewB`);
        // The dirty flag was cleared by the accepted switch: navigating back must not prompt.
        fireEvent.click(tabAnchor("viewA"));
        await waitFor(() => expect(viewContent("viewA")).toBeInTheDocument());
        expect(promptModal()).toBeNull();
        expect(confirmSpy).not.toHaveBeenCalled();
    });

    it("should prompt via the PromptModal for a deep link with query params when dirty and apply the tab on accept", async () => {
        const history = await renderTabView();
        setUnsavedChanges(true);
        // Deep links with a search string pass history.block silently (block only prompts for
        // search-less navigations), so the URL has already changed when the prompt opens.
        simulateBrowserPop(`${basePath}/viewB?ruleId=rule1`);
        await expectPromptModal();
        expect(history.location.pathname).toBe(`${basePath}/viewB`);
        expect(viewContent("viewA")).toBeInTheDocument(); // tab not applied yet
        await answerPrompt("prompt-proceed");
        await waitFor(() => expect(viewContent("viewB")).toBeInTheDocument());
        // The deep link URL including its query params is kept
        expect(history.location.pathname).toBe(`${basePath}/viewB`);
        expect(history.location.search).toBe("?ruleId=rule1");
        expect(confirmSpy).not.toHaveBeenCalled();
    });

    it("should restore the URL and keep the unsaved-changes flag when a dirty deep-link navigation is declined", async () => {
        const history = await renderTabView();
        setUnsavedChanges(true);
        simulateBrowserPop(`${basePath}/viewB?ruleId=rule1`);
        await expectPromptModal();
        await answerPrompt("prompt-cancel");
        // The URL is restored to the (kept) selected tab's bookmark, without the search string.
        await waitFor(() => expect(history.location.pathname).toBe(`${basePath}/viewA`));
        expect(history.location.search).toBe("");
        expect(viewContent("viewA")).toBeInTheDocument();
        expect(viewContent("viewB")).toBeNull();
        // The synchronous URL restore must pass history.block without opening a second prompt
        // (restoringDeclinedUrlRef) and without falling back to window.confirm.
        await expectNoPromptModal();
        expect(confirmSpy).not.toHaveBeenCalled();
        // The unsaved-changes flag is untouched: an in-app tab click still prompts.
        fireEvent.click(tabAnchor("viewB"));
        await expectPromptModal();
        expect(confirmSpy).not.toHaveBeenCalled();
    });

    it("should never prompt for query-param-only URL changes, including clearing the params", async () => {
        const history = await renderTabView();
        setUnsavedChanges(true);
        // Adding query params on the same pathname (e.g. selecting a rule in the editor)
        act(() => {
            history.push(`${basePath}/viewA?paramA=1`);
        });
        expect(history.location.search).toBe("?paramA=1");
        // Replacing them
        act(() => {
            history.replace(`${basePath}/viewA?paramA=2`);
        });
        expect(history.location.search).toBe("?paramA=2");
        // CLEARING them (empty search used to trip the history.block prompt)
        act(() => {
            history.push(`${basePath}/viewA`);
        });
        expect(history.location.search).toBe("");
        expect(history.location.pathname).toBe(`${basePath}/viewA`);
        expect(viewContent("viewA")).toBeInTheDocument();
        expect(promptModal()).toBeNull();
        expect(confirmSpy).not.toHaveBeenCalled();
    });

    it("should route a dirty full-page navigation through the PromptModal and cancel it on decline", async () => {
        const history = await renderTabView();
        const projectPagePath = workspacePath(`/projects/${projectId}`);
        setUnsavedChanges(true);
        // Search-less pathname change: intercepted by history.block, confirmation routed through
        // the custom getUserConfirmation into the PromptModal.
        act(() => {
            history.push(projectPagePath);
        });
        await expectPromptModal();
        // While the prompt is open the navigation has not happened
        expect(history.location.pathname).toBe(`${basePath}/viewA`);
        await answerPrompt("prompt-cancel");
        await expectNoPromptModal();
        // Navigation cancelled, view unchanged
        expect(history.location.pathname).toBe(`${basePath}/viewA`);
        expect(viewContent("viewA")).toBeInTheDocument();
        expect(confirmSpy).not.toHaveBeenCalled();
    });

    it("should proceed with a dirty full-page navigation when the prompt is accepted", async () => {
        const history = await renderTabView();
        const projectPagePath = workspacePath(`/projects/${projectId}`);
        setUnsavedChanges(true);
        act(() => {
            history.push(projectPagePath);
        });
        await expectPromptModal();
        await answerPrompt("prompt-proceed");
        await waitFor(() => expect(history.location.pathname).toBe(projectPagePath));
        await expectNoPromptModal();
        expect(confirmSpy).not.toHaveBeenCalled();
    });

    it("should prompt when closing the modal embedding with unsaved changes and only close on accept", async () => {
        const handlerRemoveModal = jest.fn();
        await renderTabView({ handlerRemoveModal });
        setUnsavedChanges(true);
        const closeButton = () => document.querySelector(byTestId("close-project-tab-view")) as HTMLElement;
        fireEvent.click(closeButton());
        await expectPromptModal();
        await answerPrompt("prompt-cancel");
        await expectNoPromptModal();
        expect(handlerRemoveModal).not.toHaveBeenCalled();
        // Second attempt, this time accepting
        fireEvent.click(closeButton());
        await expectPromptModal();
        await answerPrompt("prompt-proceed");
        await waitFor(() => expect(handlerRemoveModal).toHaveBeenCalledTimes(1));
        expect(confirmSpy).not.toHaveBeenCalled();
    });

    it("should close the modal embedding without prompting when there are no unsaved changes", async () => {
        const handlerRemoveModal = jest.fn();
        await renderTabView({ handlerRemoveModal });
        fireEvent.click(document.querySelector(byTestId("close-project-tab-view")) as HTMLElement);
        await waitFor(() => expect(handlerRemoveModal).toHaveBeenCalledTimes(1));
        expect(promptModal()).toBeNull();
        expect(confirmSpy).not.toHaveBeenCalled();
    });
});
