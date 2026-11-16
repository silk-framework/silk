import React from "react";
import { fireEvent, render } from "@testing-library/react";
import { IAppliedFacetState, IFacetState } from "@ducks/workspace/typings";
import FacetsList from "./FacetsList";

let mockFacets: IFacetState[] = [];
let mockApplied: IAppliedFacetState[] = [];
jest.mock("@ducks/workspace", () => ({
    workspaceSel: { facetsSelector: () => mockFacets, appliedFacetsSelector: () => mockApplied },
    workspaceOp: { toggleFacetOp: jest.fn(), changePageOp: jest.fn() },
}));
jest.mock("react-redux", () => ({ useSelector: (select: () => unknown) => select(), useDispatch: () => jest.fn() }));
jest.mock("react-router", () => ({ useLocation: () => ({ search: "" }) }));
jest.mock("react-i18next", () => ({
    useTranslation: () => [
        (key: string, values?: { smth?: string }) => (key === "common.action.ShowSmth" ? values?.smth : key),
        { exists: () => false },
    ],
}));

beforeEach(() => {
    mockFacets = [
        {
            id: "conversation",
            label: "Conversation",
            description: "",
            type: "keyword",
            values: Array.from({ length: 9 }, (_, i) => ({ id: `c${i}`, label: `C${i}`, count: 1 })),
        },
    ];
    mockApplied = [{ facetId: "conversation", type: "keyword", keywordIds: ["c1"] }];
});
const values = (container: HTMLElement): string[] =>
    Array.from(container.querySelectorAll<HTMLInputElement>('input[type="checkbox"]'), (input) => input.value);

it("pins a preferred value ahead of checked values and retains it when expanded or collapsed", () => {
    const view = render(<FacetsList prioritizedFacetValues={{ conversation: "c8" }} />);
    expect(values(view.container)).toEqual(["c8", "c1", "c0", "c2", "c3", "c4", "c5"]);
    fireEvent.click(view.getByRole("button", { name: "common.words.more" }));
    expect(values(view.container)).toHaveLength(9);
    expect(values(view.container).slice(0, 2)).toEqual(["c8", "c1"]);
    fireEvent.click(view.getByRole("button", { name: "common.words.less" }));
    expect(values(view.container)).toHaveLength(7);
    expect(values(view.container)[0]).toBe("c8");
});

it("reacts to a changed preference and restores checked-first order when cleared", () => {
    const view = render(<FacetsList prioritizedFacetValues={{ conversation: "c8" }} />);
    view.rerender(<FacetsList prioritizedFacetValues={{ conversation: "c7" }} />);
    expect(values(view.container)[0]).toBe("c7");
    view.rerender(<FacetsList />);
    expect(values(view.container)).toEqual(["c1", "c0", "c2", "c3", "c4", "c5", "c6"]);
});
