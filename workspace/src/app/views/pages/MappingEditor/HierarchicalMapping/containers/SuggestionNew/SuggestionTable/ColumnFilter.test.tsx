import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import ColumnFilter from "./ColumnFilter";

jest.mock("../SuggestionContainer", () => ({
    SuggestionListContext: require("react").createContext({ portalContainer: undefined }),
}));

it("exposes the checked filter and updates it when filters are toggled or replaced", async () => {
    const filters = [
        { action: "first", label: "First", selectable: "always" as const },
        { action: "second", label: "Second", selectable: "always" as const },
    ];
    const Filter = () => {
        const [selectedFilter, setSelectedFilter] = React.useState("");
        return (
            <ColumnFilter
                filters={filters}
                selectedFilter={selectedFilter}
                fromDataset
                onApplyFilter={(value) => setSelectedFilter((previous) => (previous === value ? "" : value))}
            />
        );
    };
    render(<Filter />);
    const open = () => fireEvent.click(screen.getByRole("button"));
    open();
    expect(await screen.findByRole("menuitemcheckbox", { name: "First" })).toHaveAttribute("aria-checked", "false");
    fireEvent.click(screen.getByRole("menuitemcheckbox", { name: "First" }));
    open();
    expect(await screen.findByRole("menuitemcheckbox", { name: "First" })).toHaveAttribute("aria-checked", "true");
    fireEvent.click(screen.getByRole("menuitemcheckbox", { name: "Second" }));
    open();
    expect(await screen.findByRole("menuitemcheckbox", { name: "First" })).toHaveAttribute("aria-checked", "false");
    expect(screen.getByRole("menuitemcheckbox", { name: "Second" })).toHaveAttribute("aria-checked", "true");
    fireEvent.click(screen.getByRole("menuitemcheckbox", { name: "Second" }));
    open();
    expect(await screen.findByRole("menuitemcheckbox", { name: "Second" })).toHaveAttribute("aria-checked", "false");
});
