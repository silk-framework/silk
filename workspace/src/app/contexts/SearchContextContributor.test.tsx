import React from "react";
import { render, screen } from "@testing-library/react";
import { useSelector } from "react-redux";

import { ApplicationContextRegistryProvider, useApplicationContextRegistry } from "./ApplicationContextRegistry";
import { SearchContextContributor } from "./SearchContextContributor";

jest.mock("react-redux", () => ({
    ...jest.requireActual("react-redux"),
    useSelector: jest.fn(),
}));

const RegistryState = () => {
    const { pageContributions } = useApplicationContextRegistry();
    return <output data-testid="registry-state">{JSON.stringify(pageContributions)}</output>;
};

describe("SearchContextContributor", () => {
    it("registers the current validated project search without rendering UI", () => {
        const selectorValues = [
            { itemType: "dataset", textQuery: " people " },
            [{ facetId: "tags", keywordIds: ["customer"] }],
            [{ id: "tags", label: "Tags", values: [{ id: "customer", label: "Customer" }] }],
        ];
        jest.mocked(useSelector).mockImplementation(() => selectorValues.shift());

        const view = render(
            <ApplicationContextRegistryProvider>
                <SearchContextContributor enabled projectId="movies" view="project" />
                <RegistryState />
            </ApplicationContextRegistryProvider>,
        );

        expect(view.container.querySelectorAll("output")).toHaveLength(1);
        expect(JSON.parse(screen.getByTestId("registry-state").textContent ?? "[]")).toEqual([
            {
                kind: "search",
                scope: { projectId: "movies", view: "project" },
                search: {
                    facets: [{ id: "tags", label: "Tags", values: [{ id: "customer", label: "Customer" }] }],
                    itemType: "dataset",
                    text: "people",
                },
            },
        ]);
    });
});
