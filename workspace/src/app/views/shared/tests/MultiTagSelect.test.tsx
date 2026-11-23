import React from "react";
import { act, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";

import { MultiTagSelect } from "../MultiTagSelect";

jest.mock("react-i18next", () => ({ useTranslation: () => [(key: string) => key] }));
jest.mock("../../../hooks/useErrorHandler", () => ({
    __esModule: true,
    default: () => ({ registerError: jest.fn() }),
}));
jest.mock("../Metadata/MetadataUtils", () => ({
    __esModule: true,
    default: {
        queryTags: jest.fn(async () => ({
            data: {
                tags: [
                    { uri: "alpha-1", label: "alpha 1" },
                    { uri: "alpha-2", label: "alpha 2" },
                    { uri: "beta", label: "beta" },
                ],
            },
        })),
    },
}));

describe("MultiTagSelect", () => {
    it.each([false, true])(
        "keeps the filter active while toggling existing tags (controlled: %s)",
        async (controlled) => {
            const TagSelector = () => {
                const [tags, setTags] = React.useState([
                    { uri: "alpha-1", label: "alpha 1" },
                    { uri: "alpha-2", label: "alpha 2" },
                    { uri: "beta", label: "beta" },
                ]);
                return (
                    <MultiTagSelect
                        projectId="project"
                        {...(controlled ? { selectedTags: tags } : { initialTags: tags })}
                        handleTagSelectionChange={
                            controlled ? ({ selectedItems }) => setTags(selectedItems) : jest.fn()
                        }
                    />
                );
            };
            render(<TagSelector />);
            const input = screen.getByRole("combobox");
            await userEvent.click(input);
            await userEvent.type(input, "alpha");
            const option = await screen.findByRole("option", { name: "alpha 1" });
            act(() => option.querySelector("a")!.focus());
            await userEvent.keyboard("{Enter}");

            expect(input).toHaveValue("alpha");
            expect(option).toBeInTheDocument();
            expect(option).toHaveAttribute("aria-selected", "false");
            expect(screen.queryByRole("option", { name: "beta" })).not.toBeInTheDocument();
            expect(input).toHaveFocus();
            act(() => option.querySelector("a")!.focus());
            await userEvent.keyboard("{Enter}");
            expect(option).toHaveAttribute("aria-selected", "true");
            expect(input).toHaveValue("alpha");
            expect(screen.getByRole("listbox")).toBeInTheDocument();
        },
    );
});
