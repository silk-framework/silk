import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";

import {
    ApplicationContextRegistryProvider,
    PageContextContribution,
    createSearchContext,
    useApplicationContextRegistry,
    usePageContextContribution,
} from "./ApplicationContextRegistry";

const SearchContributor = ({ contribution }: { contribution?: PageContextContribution }) => {
    usePageContextContribution(contribution);
    return null;
};

const RegistryState = () => {
    const registry = useApplicationContextRegistry();
    return (
        <output data-testid="registry-state">
            {JSON.stringify({
                foregroundViews: registry.foregroundViews,
                pageContributions: registry.pageContributions,
            })}
        </output>
    );
};

const state = () => JSON.parse(screen.getByTestId("registry-state").textContent ?? "{}");

describe("ApplicationContextRegistry", () => {
    it("does not rerender contribution owners when registry state changes", () => {
        let contributorRenderCount = 0;
        const contribution: PageContextContribution = {
            kind: "search",
            scope: { view: "workspace" },
            search: { text: "stable" },
        };
        const RenderCounterContributor = () => {
            contributorRenderCount += 1;
            usePageContextContribution(contribution);
            return null;
        };
        const RegisterAnotherContribution = () => {
            const registry = useApplicationContextRegistry();
            return (
                <button
                    onClick={() =>
                        registry.registerPageContribution({
                            kind: "search",
                            scope: { view: "workspace" },
                            search: { text: "another" },
                        })
                    }
                >
                    register another
                </button>
            );
        };

        render(
            <ApplicationContextRegistryProvider>
                <RenderCounterContributor />
                <RegisterAnotherContribution />
            </ApplicationContextRegistryProvider>,
        );
        expect(contributorRenderCount).toBe(1);

        fireEvent.click(screen.getByText("register another"));
        expect(contributorRenderCount).toBe(1);
    });

    it("registers, updates and removes a page contribution with its component lifecycle", () => {
        const first: PageContextContribution = {
            kind: "search",
            scope: { view: "workspace" },
            search: { text: "first" },
        };
        const second: PageContextContribution = {
            kind: "search",
            scope: { view: "workspace" },
            search: { text: "second" },
        };
        const view = render(
            <ApplicationContextRegistryProvider>
                <SearchContributor contribution={first} />
                <RegistryState />
            </ApplicationContextRegistryProvider>,
        );

        expect(state().pageContributions).toEqual([first]);

        view.rerender(
            <ApplicationContextRegistryProvider>
                <SearchContributor contribution={second} />
                <RegistryState />
            </ApplicationContextRegistryProvider>,
        );
        expect(state().pageContributions).toEqual([second]);

        view.rerender(
            <ApplicationContextRegistryProvider>
                <RegistryState />
            </ApplicationContextRegistryProvider>,
        );
        expect(state().pageContributions).toEqual([]);
    });

    it("does not let an old registration remove a newer contribution", () => {
        const Handles = () => {
            const registry = useApplicationContextRegistry();
            const oldHandle = React.useRef<ReturnType<typeof registry.registerPageContribution>>();
            return (
                <>
                    <button
                        onClick={() => {
                            oldHandle.current = registry.registerPageContribution({
                                kind: "search",
                                scope: { view: "workspace" },
                                search: { text: "old" },
                            });
                        }}
                    >
                        register old
                    </button>
                    <button
                        onClick={() =>
                            registry.registerPageContribution({
                                kind: "search",
                                scope: { view: "workspace" },
                                search: { text: "new" },
                            })
                        }
                    >
                        register new
                    </button>
                    <button onClick={() => oldHandle.current?.unregister()}>remove old</button>
                </>
            );
        };
        render(
            <ApplicationContextRegistryProvider>
                <Handles />
                <RegistryState />
            </ApplicationContextRegistryProvider>,
        );

        fireEvent.click(screen.getByText("register old"));
        fireEvent.click(screen.getByText("register new"));
        fireEvent.click(screen.getByText("remove old"));

        expect(state().pageContributions).toEqual([
            { kind: "search", scope: { view: "workspace" }, search: { text: "new" } },
        ]);
    });

    it("updates a foreground registration without changing stack precedence", () => {
        let updateFirst: ((label: string) => void) | undefined;
        const Handles = () => {
            const registry = useApplicationContextRegistry();
            const first = React.useRef<ReturnType<typeof registry.registerForegroundView>>();
            React.useEffect(() => {
                first.current = registry.registerForegroundView({
                    kind: "taskView",
                    projectId: "movies",
                    taskId: "first",
                    taskLabel: "First",
                    taskType: "workflow",
                });
                registry.registerForegroundView({
                    kind: "taskView",
                    projectId: "movies",
                    taskId: "second",
                    taskLabel: "Second",
                    taskType: "dataset",
                });
                updateFirst = (taskLabel) => first.current?.update({
                    kind: "taskView",
                    projectId: "movies",
                    taskId: "first",
                    taskLabel,
                    taskType: "workflow",
                });
            }, [registry.registerForegroundView]);
            return null;
        };
        render(
            <ApplicationContextRegistryProvider>
                <Handles />
                <RegistryState />
            </ApplicationContextRegistryProvider>,
        );

        expect(state().foregroundViews.map(({ taskId }) => taskId)).toEqual(["first", "second"]);
        act(() => updateFirst?.("Updated first"));
        expect(state().foregroundViews.map(({ taskId }) => taskId)).toEqual(["first", "second"]);
        expect(state().foregroundViews[0].taskLabel).toBe("Updated first");
    });
});

describe("createSearchContext", () => {
    it("keeps only selected facets known by the current search response", () => {
        expect(
            createSearchContext(
                { itemType: "dataset", textQuery: " people " },
                [
                    { facetId: "tags", keywordIds: ["customer", "unknown"] },
                    { facetId: "unknownFacet", keywordIds: ["value"] },
                ],
                [
                    {
                        id: "tags",
                        label: "Tags",
                        values: [
                            { id: "customer", label: "Customer" },
                            { id: "supplier", label: "Supplier" },
                        ],
                    },
                ],
            ),
        ).toEqual({
            facets: [{ id: "tags", label: "Tags", values: [{ id: "customer", label: "Customer" }] }],
            itemType: "dataset",
            text: "people",
        });
    });
});
