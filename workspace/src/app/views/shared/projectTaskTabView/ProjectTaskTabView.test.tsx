import React from "react";
import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createBrowserHistory } from "history";
import { Router } from "react-router";
import { createInstance } from "i18next";
import { I18nextProvider } from "react-i18next";
import { ProjectTaskTabView } from "./ProjectTaskTabView";
import { IProjectTaskView, pluginRegistry } from "../../plugins/PluginRegistry";
import { requestItemLinks } from "@ducks/shared/requests";
import { IItemLink } from "@ducks/shared/typings";
import translations from "../../../../locales/manual/en.json";

jest.mock("react-redux", () => ({
    ...jest.requireActual("react-redux"),
    useSelector: () => undefined,
    useDispatch: () => jest.fn(),
}));
jest.mock("../../plugins/PluginRegistry", () => ({ pluginRegistry: { taskViews: jest.fn() } }));
jest.mock("@ducks/shared/requests", () => ({ requestItemLinks: jest.fn() }));

const i18n = createInstance();
const views: IProjectTaskView[] = [
    {
        id: "editor",
        label: "Workflow editor",
        render: (_projectId, _taskId, actions) => (
            <button onClick={() => actions?.unsavedChanges?.(true)}>Make changes</button>
        ),
    },
    { id: "report", label: "Workflow report", render: () => <div>Report content</div> },
];
const itemLink: IItemLink = { id: "preview", label: "Preview", path: "/preview" };
const externalLink: IItemLink = {
    id: "external",
    label: "External view",
    path: "/external",
    openInNewTab: true,
};

beforeAll(async () => {
    await i18n.init({
        lng: "en",
        resources: { en: { translation: translations } },
        interpolation: { escapeValue: false },
    });
});

beforeEach(() => {
    window.history.replaceState(null, "", "/projects/project/task/task/editor");
    jest.mocked(pluginRegistry.taskViews).mockReturnValue(views);
    const data = [itemLink, externalLink];
    jest.mocked(requestItemLinks).mockResolvedValue({
        data,
        axiosResponse: { data, status: 200, statusText: "OK", headers: {}, config: {} },
    });
});

afterEach(() => {
    window.onbeforeunload = null;
    jest.clearAllMocks();
});

const renderTabs = (props: Partial<React.ComponentProps<typeof ProjectTaskTabView>> = {}) => {
    const history = createBrowserHistory();
    const result = render(
        <I18nextProvider i18n={i18n}>
            <Router history={history}>
                <ProjectTaskTabView
                    taskViewConfig={{ projectId: "project", taskId: "task", pluginId: "workflow" }}
                    {...props}
                />
            </Router>
        </I18nextProvider>,
    );
    return { ...result, history };
};

it("exposes selected tabs and their labelled panels, keeping external links outside the tablist", async () => {
    renderTabs();
    const editor = await screen.findByRole("tab", { name: "Workflow editor", selected: true });
    const tablist = screen.getByRole("tablist");
    expect(within(tablist).getAllByRole("tab")).toHaveLength(3);
    expect(editor).toHaveAttribute("tabindex", "0");
    expect(editor).not.toHaveAttribute("aria-disabled", "true");
    const panel = screen.getByRole("tabpanel", { name: "Workflow editor" });
    expect(editor).toHaveAttribute("aria-controls", panel.id);
    expect(panel).toHaveAttribute("aria-labelledby", editor.id);
    for (const tab of within(tablist).getAllByRole("tab")) {
        expect(document.getElementById(tab.getAttribute("aria-controls")!)).not.toBeNull();
    }
    const external = screen.getByRole("link", { name: "External view" });
    expect(external).toHaveAttribute("href", "/external");
    expect(external).toHaveAttribute("target", "_blank");
    expect(external).toHaveAttribute("aria-describedby");
    expect(external).toHaveAccessibleDescription("The link will be opened in a new tab");
    expect(external.querySelector("svg")).toBeVisible();
    expect(external.querySelector("svg")).toHaveAttribute("aria-hidden", "true");
    expect(tablist).not.toContainElement(external);
});

it("keeps a single inline view and external links available without a tablist", async () => {
    jest.mocked(pluginRegistry.taskViews).mockReturnValue([views[0]]);
    renderTabs({ srcLinks: [externalLink], startWithLink: "editor" });

    expect(await screen.findByRole("link", { name: "External view" })).toHaveAttribute("target", "_blank");
    expect(screen.getByRole("button", { name: "Make changes" })).toBeVisible();
    expect(screen.queryByRole("tablist")).not.toBeInTheDocument();
    expect(screen.queryByRole("tab")).not.toBeInTheDocument();
    expect(screen.queryByRole("tabpanel")).not.toBeInTheDocument();
});

it("supports arrow-key focus, Enter/Space activation and tabbing into the panel", async () => {
    const user = userEvent.setup();
    const { history } = renderTabs();
    const editor = await screen.findByRole("tab", { name: "Workflow editor", selected: true });
    editor.focus();
    await act(() => user.keyboard("{ArrowRight}"));
    const report = screen.getByRole("tab", { name: "Workflow report" });
    expect(report).toHaveFocus();
    expect(editor).toHaveAttribute("aria-selected", "true");
    await act(() => user.keyboard("{Enter}"));
    expect(report).toHaveAttribute("aria-selected", "true");
    expect(editor).toHaveAttribute("aria-selected", "false");
    expect(history.location.pathname).toBe("/projects/project/task/task/report");
    expect(screen.getByRole("tabpanel", { name: "Workflow report" })).toHaveTextContent("Report content");
    await act(() => user.keyboard("{ArrowLeft} "));
    expect(editor).toHaveAttribute("aria-selected", "true");
    await act(() => user.keyboard("{ArrowLeft}"));
    expect(screen.getByRole("tab", { name: "Preview" })).toHaveFocus();
    await act(() => user.keyboard("{ArrowRight}"));
    expect(editor).toHaveFocus();
    await act(() => user.tab());
    expect(screen.getByRole("link", { name: "External view" })).toHaveFocus();
    await act(() => user.tab());
    await act(() => user.tab());
    expect(screen.getByRole("tabpanel", { name: "Workflow editor" })).toHaveFocus();
});

it("preserves the selected tab until an unsaved-change switch is confirmed", async () => {
    const user = userEvent.setup();
    renderTabs();
    const editor = await screen.findByRole("tab", { name: "Workflow editor", selected: true });
    await act(() => user.click(screen.getByRole("button", { name: "Make changes" })));
    await act(() => user.click(screen.getByRole("tab", { name: "Workflow report" })));
    expect(editor).toHaveAttribute("aria-selected", "true");
    await act(() => user.click(screen.getByRole("button", { name: "Cancel" })));
    expect(editor).toHaveAttribute("aria-selected", "true");
    await act(() => user.click(screen.getByRole("tab", { name: "Workflow report" })));
    await act(() => user.click(screen.getByRole("button", { name: "Proceed" })));
    await waitFor(() =>
        expect(screen.getByRole("tab", { name: "Workflow report" })).toHaveAttribute("aria-selected", "true"),
    );
});

it("does not render an unused external-link description, and keeps the iframe mounted", async () => {
    jest.mocked(pluginRegistry.taskViews).mockReturnValue([]);
    const secondLink = { ...itemLink, id: "second", label: "Second preview", path: "/second" };
    renderTabs({ srcLinks: [itemLink, secondLink], startWithLink: itemLink });
    const preview = await screen.findByRole("tab", { name: "Preview", selected: true });
    expect(document.querySelector('[id$="-external-link-description"]')).not.toBeInTheDocument();
    const iframe = screen.getByTitle("Preview");
    fireEvent.load(iframe);
    expect(preview).toHaveAttribute("aria-controls", screen.getByRole("tabpanel", { name: "Preview" }).id);
    fireEvent.click(screen.getByRole("tab", { name: "Second preview" }));
    expect(screen.getByTitle("Second preview")).toBe(iframe);
    expect(iframe).toHaveAttribute("src", "/second?inlineView=true");
    expect(screen.getByRole("tabpanel", { name: "Second preview" })).toContainElement(iframe);
});

it("uses unique tab and panel IDs across inline and modal instances", async () => {
    renderTabs();
    renderTabs({ handlerRemoveModal: jest.fn() });
    await waitFor(() =>
        expect(screen.getAllByRole("tab", { name: "Workflow editor", selected: true })).toHaveLength(2),
    );
    const tabs = screen.getAllByRole("tab");
    expect(new Set(tabs.map((tab) => tab.id)).size).toBe(tabs.length);
    for (const tab of tabs) {
        expect(document.getElementById(tab.getAttribute("aria-controls")!)).toHaveAttribute("aria-labelledby", tab.id);
    }
    const externalLinks = screen.getAllByRole("link", { name: "External view" });
    expect(new Set(externalLinks.map((link) => link.getAttribute("aria-describedby"))).size).toBe(2);
    for (const link of externalLinks) {
        expect(link).toHaveAccessibleDescription("The link will be opened in a new tab");
    }
});
