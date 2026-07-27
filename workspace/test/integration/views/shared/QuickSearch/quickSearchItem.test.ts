import { IRecentlyViewedItem, ISearchResultsServer } from "../../../../../src/app/store/ducks/workspace/typings";
import {
    QuickSearchItem,
    filterQuickSearchItemsLocally,
    fromRecentlyViewedItem,
    fromSearchResult,
    groupQuickSearchItems,
} from "../../../../../src/app/views/shared/QuickSearch/quickSearchItem";

/** Minimal, fully typed quick search item with only the fields a test cares about overridden. */
const mkItem = (itemType: string, key: string, overrides: Partial<QuickSearchItem> = {}): QuickSearchItem => ({
    key,
    label: key,
    itemType,
    path: "",
    searchableText: key.toLowerCase(),
    hiddenSearchTokens: [],
    ...overrides,
});

/** N items of the same type, keyed 0..N-1, so a group's cap can be observed. */
const mkItems = (itemType: string, count: number): QuickSearchItem[] =>
    Array.from({ length: count }, (_, i) => mkItem(itemType, `${itemType}-${i}`));

describe("quickSearchItem grouping", () => {
    it("orders the type groups by the canonical item type order, not by input order", () => {
        // Deliberately shuffled input.
        const items = [
            mkItem("task", "t1"),
            mkItem("project", "p1"),
            mkItem("dataset", "d1"),
            mkItem("workflow", "w1"),
            mkItem("transform", "tr1"),
            mkItem("linking", "l1"),
        ];

        const groups = groupQuickSearchItems(items, 5);

        expect(groups.map((g) => g.itemType)).toEqual([
            "project",
            "workflow",
            "transform",
            "linking",
            "dataset",
            "task",
        ]);
    });

    it("only emits groups for the types that are actually present", () => {
        const items = [mkItem("workflow", "w1"), mkItem("dataset", "d1")];

        const groups = groupQuickSearchItems(items, 5);

        expect(groups.map((g) => g.itemType)).toEqual(["workflow", "dataset"]);
    });

    it("caps every group at the requested number of items", () => {
        const items = [...mkItems("workflow", 8), ...mkItems("dataset", 3)];

        const groups = groupQuickSearchItems(items, 5);

        const workflowGroup = groups.find((g) => g.itemType === "workflow")!;
        const datasetGroup = groups.find((g) => g.itemType === "dataset")!;
        expect(workflowGroup.items).toHaveLength(5);
        // The first five in encounter order are kept.
        expect(workflowGroup.items.map((i) => i.key)).toEqual([
            "workflow-0",
            "workflow-1",
            "workflow-2",
            "workflow-3",
            "workflow-4",
        ]);
        // A smaller group is not padded.
        expect(datasetGroup.items).toHaveLength(3);
    });

    it("appends unknown types after the known ones, preserving their first-seen order", () => {
        const items = [
            mkItem("zeta" as string, "z1"),
            mkItem("workflow", "w1"),
            mkItem("alpha" as string, "a1"),
            mkItem("project", "p1"),
        ];

        const groups = groupQuickSearchItems(items, 5);

        // Known types first (in canonical order), then unknown types in the order they appeared.
        expect(groups.map((g) => g.itemType)).toEqual(["project", "workflow", "zeta", "alpha"]);
    });

    it("returns an empty grouping for an empty input", () => {
        expect(groupQuickSearchItems([], 5)).toEqual([]);
    });
});

describe("quickSearchItem local filter", () => {
    const items = [
        mkItem("workflow", "w1", { searchableText: "customer ingest workflow", hiddenSearchTokens: [] }),
        mkItem("dataset", "d1", { searchableText: "orders dataset", hiddenSearchTokens: ["sku"] }),
    ];

    it("requires every search word to match somewhere in the haystack (AND semantics)", () => {
        expect(filterQuickSearchItemsLocally(items, "customer workflow").map((i) => i.key)).toEqual(["w1"]);
        // "customer" is not part of the dataset haystack, so a two-word query cannot match it.
        expect(filterQuickSearchItemsLocally(items, "customer orders")).toHaveLength(0);
    });

    it("also matches against the hidden search tokens", () => {
        expect(filterQuickSearchItemsLocally(items, "sku").map((i) => i.key)).toEqual(["d1"]);
    });

    it("returns all items when the query has no search words", () => {
        expect(filterQuickSearchItemsLocally(items, "   ")).toBe(items);
    });
});

describe("quickSearchItem normalization", () => {
    it("normalizes a recently viewed task item, lower-casing the type and building a stable key", () => {
        const recent: IRecentlyViewedItem = {
            projectId: "proj",
            projectLabel: "Project Label",
            itemType: "Workflow",
            taskId: "task1",
            taskLabel: "My Workflow",
            pluginLabel: "Workflow plugin",
            itemLinks: [{ label: "open", path: "/workbench/projects/proj/workflow/task1" }],
            searchTags: ["etl"],
        };

        const item = fromRecentlyViewedItem(recent);

        expect(item.key).toBe("recent:proj:task1");
        expect(item.label).toBe("My Workflow");
        expect(item.itemType).toBe("workflow");
        expect(item.path).toBe("/workbench/projects/proj/workflow/task1");
        // The haystack is lower-cased and folds in label, project context, plugin, type and tags.
        expect(item.searchableText).toContain("my workflow");
        expect(item.searchableText).toContain("project label");
        expect(item.searchableText).toContain("etl");
    });

    it("normalizes a workspace search result and falls back to id when a label is missing", () => {
        const result: ISearchResultsServer = {
            id: "abc",
            label: "",
            type: "Dataset",
            projectId: "proj",
            itemLinks: [{ label: "open", path: "/workbench/projects/proj/dataset/abc" }],
        };

        const item = fromSearchResult(result);

        expect(item.key).toBe("search:proj:abc");
        expect(item.label).toBe("abc");
        expect(item.itemType).toBe("dataset");
        expect(item.path).toBe("/workbench/projects/proj/dataset/abc");
    });

    it("yields an empty path when a search result has no item links", () => {
        const result: ISearchResultsServer = { id: "abc", label: "No links", type: "task" };
        expect(fromSearchResult(result).path).toBe("");
    });
});
