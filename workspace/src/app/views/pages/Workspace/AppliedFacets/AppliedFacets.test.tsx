import React from "react";
import { fireEvent, within } from "@testing-library/react";
import { createBrowserHistory } from "history";
import AppliedFacets from ".";
import { renderWrapper } from "../../../../../../test/integration/TestHelper";

it("removes one active value, keeps other filters and resets pagination", () => {
    const history = createBrowserHistory();
    const view = renderWrapper(<AppliedFacets />, history, {
        workspace: {
            filters: {
                facets: [
                    {
                        id: "custom",
                        label: "Custom facet",
                        description: "",
                        type: "keyword",
                        values: [
                            { id: "a", label: "Alpha", count: 2 },
                            { id: "b", label: "Beta", count: 1 },
                        ],
                    },
                ],
                appliedFacets: [{ facetId: "custom", type: "keyword", keywordIds: ["a", "b"] }],
                appliedFilters: { textQuery: "movies" },
                pagination: { current: 3 },
            },
        },
    });
    const tag = view.getByText("Alpha").closest<HTMLElement>(".eccgui-tag__item");
    expect(tag).not.toBeNull();
    if (!tag) {
        throw new Error("Expected the active filter tag");
    }
    fireEvent.click(within(tag).getByRole("button", { name: "Remove tag" }));

    expect(view.queryByText("Alpha")).not.toBeInTheDocument();
    expect(view.getByText("Beta")).toBeInTheDocument();
    const query = new URLSearchParams(history.location.search);
    expect(query.get("f_keys")).toBe("b");
    expect(query.get("textQuery")).toBe("movies");
    expect(query.get("page")).toBe("1");
});

it("keeps selected filters removable when facet metadata or values are missing", () => {
    const history = createBrowserHistory();
    const view = renderWrapper(<AppliedFacets />, history, {
        workspace: {
            filters: {
                facets: [{ id: "changeKind", label: "Kind", description: "", type: "keyword", values: [] }],
                appliedFacets: [
                    { facetId: "changeState", type: "keyword", keywordIds: ["unreviewed"] },
                    { facetId: "changeKind", type: "keyword", keywordIds: ["added"] },
                ],
            },
        },
    });
    expect(view.getByText("State")).toBeInTheDocument();
    expect(view.getByText("Unreviewed")).toBeInTheDocument();
    expect(view.getByText("Kind")).toBeInTheDocument();
    expect(view.getByText("Added")).toBeInTheDocument();
    fireEvent.click(view.getAllByRole("button", { name: "Remove tag" })[0]);
    expect(view.queryByText("Unreviewed")).not.toBeInTheDocument();
    expect(new URLSearchParams(history.location.search).get("f_ids")).toBe("changeKind");
});
