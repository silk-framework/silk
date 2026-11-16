import { createMemoryHistory } from "history";
import { push, RouterState } from "connected-react-router";
import routerOps from "./operations";

const updateQuery = (
    search: string,
    query: Parameters<typeof routerOps.setQueryString>[0],
    options?: { preserveKeys?: readonly string[] },
): URL => {
    const history = createMemoryHistory({ initialEntries: [`/workbench?${search}`] });
    const router: RouterState = { location: { ...history.location, query: {} }, action: history.action };
    const dispatch = jest.fn((action: ReturnType<typeof push>) => action);
    routerOps.setQueryString(query, options)(dispatch, () => ({ router }));
    const target: unknown = dispatch.mock.calls[0]?.[0].payload.args?.[0];
    if (typeof target !== "string") {
        throw new Error("Expected navigation to a URL");
    }
    return new URL(target, "http://localhost");
};

it("replaces the query by default, including conversation highlight parameters", () => {
    const url = updateQuery("highlightConversation=c4&context=old&page=1", { page: 2 });
    expect(Array.from(url.searchParams.entries())).toEqual([["page", "2"]]);
});

it("preserves only caller-selected parameters and their decoded values", () => {
    const url = updateQuery(
        "context=chat%201%26two&tags[]=a&tags[]=b&stale=drop&page=1",
        { page: 2 },
        { preserveKeys: ["context", "tags"] },
    );
    expect(url.searchParams.get("context")).toBe("chat 1&two");
    expect(url.searchParams.get("tags")).toBe("a,b");
    expect(url.searchParams.get("page")).toBe("2");
    expect(url.searchParams.has("stale")).toBe(false);
});

it("lets explicit query values override preserved values", () => {
    const url = updateQuery("context=old", { context: "new" }, { preserveKeys: ["context"] });
    expect(url.searchParams.get("context")).toBe("new");
});

it("lets explicit empty values remove preserved parameters", () => {
    const url = updateQuery("context=old&tags=a,b", { context: "", tags: [] }, { preserveKeys: ["context", "tags"] });
    expect(url.search).toBe("");
});

it("does not restore a preserved parameter that has been removed from the current URL", () => {
    const url = updateQuery("page=1", { page: 2 }, { preserveKeys: ["context"] });
    expect(url.searchParams.has("context")).toBe(false);
});
