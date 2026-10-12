import React from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";

import "@testing-library/jest-dom";

import { SidebarSearchField } from "./SidebarSearchField";

describe("SidebarSearchField", () => {
    it("links the focused search input to its visible suggestion listbox", async () => {
        render(
            <SidebarSearchField
                onQueryChange={jest.fn()}
                searchSuggestions={() => [{ value: "example", query: "", from: 0, length: 0 }]}
            />,
        );

        const input = screen.getByRole("searchbox");
        expect(input).not.toHaveAttribute("aria-controls");

        fireEvent.focus(input);
        const listbox = await screen.findByRole("listbox");
        expect(listbox.id).toBeTruthy();
        expect(input).toHaveAttribute("aria-controls", listbox.id);

        fireEvent.blur(input);
        await waitFor(() => expect(screen.queryByRole("listbox")).not.toBeInTheDocument());
        expect(input).not.toHaveAttribute("aria-controls");
    });
});
