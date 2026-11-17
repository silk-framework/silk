import React from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom";

import TypesList from "./TypesList";

describe("TypesList", () => {
    it("exposes Select entries as listbox options", async () => {
        const onChange = jest.fn();
        render(<TypesList selected="value" onChange={onChange} />);

        fireEvent.click(screen.getByRole("button"));

        const listbox = await screen.findByRole("listbox");
        expect(listbox.querySelectorAll('[role="option"]')).toHaveLength(2);
        expect(listbox.querySelectorAll('[role="menuitem"]')).toHaveLength(0);

        fireEvent.click(screen.getByRole("option", { name: "object" }).querySelector("a")!);
        await waitFor(() => expect(onChange.mock.calls[0][0]).toBe("object"));
    });
});
