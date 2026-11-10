import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import i18next from "i18next";
import { I18nextProvider } from "react-i18next";
import translations from "../../../../locales/manual/en.json";
import { GlobalTableContext } from "../../../GlobalContextsWrapper";
import { defaultGlobalTableSettings } from "../../../hooks/useStoreGlobalTableSettings";
import { IAppliedSorterState } from "@ducks/workspace/typings";
import SortButton from "./SortButton";

const i18n = i18next.createInstance();
const sorters = [
    { id: "name", label: "Name" },
    { id: "date", label: "Date" },
];

beforeAll(async () => {
    await i18n.init({
        lng: "en",
        resources: { en: { translation: translations } },
        interpolation: { escapeValue: false },
    });
});

it.each<IAppliedSorterState>([
    { sortBy: "name", sortOrder: "ASC" },
    { sortBy: "name", sortOrder: "DESC" },
    { sortBy: "name", sortOrder: "" },
    { sortBy: "", sortOrder: "" },
])("describes the current sort and the action performed by each item: %j", async (activeSort) => {
    const update = jest.fn();
    const currentDirection = activeSort.sortOrder === "DESC" ? "descending" : "ascending";
    render(
        <I18nextProvider i18n={i18n}>
            <GlobalTableContext.Provider
                value={{ globalTableSettings: defaultGlobalTableSettings, updateGlobalTableSettings: update }}
            >
                <SortButton sortersList={sorters} activeSort={activeSort} />
            </GlobalTableContext.Provider>
        </I18nextProvider>,
    );
    const trigger = screen.getByRole("button");
    expect(trigger).toHaveAccessibleName(
        activeSort.sortBy ? `Sort options. Currently sorted by Name, ${currentDirection}.` : "Sort options",
    );
    fireEvent.click(trigger);
    const name = await screen.findByRole("menuitem", { name: "Name" });
    const nextDirection = activeSort.sortBy === "name" && activeSort.sortOrder === "ASC" ? "descending" : "ascending";
    expect(name).toHaveAccessibleDescription(
        activeSort.sortBy === "name"
            ? `Current sort: ${currentDirection}. Activate to sort ${nextDirection}.`
            : "Activate to sort by Name, ascending.",
    );
    expect(screen.getByRole("menuitem", { name: "Date" })).toHaveAccessibleDescription(
        `Activate to sort by Date, ${currentDirection}.`,
    );
    fireEvent.click(name);
    expect(update).toHaveBeenLastCalledWith({
        sortBy: "name",
        sortOrder: nextDirection === "descending" ? "DESC" : "ASC",
    });
    fireEvent.click(trigger);
    fireEvent.click(await screen.findByRole("menuitem", { name: "Date" }));
    expect(update).toHaveBeenLastCalledWith({ sortBy: "date", sortOrder: activeSort.sortOrder || "ASC" });
});
