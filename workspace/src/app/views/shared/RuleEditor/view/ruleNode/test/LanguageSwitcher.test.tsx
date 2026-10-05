import React from "react";
import "@testing-library/jest-dom";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createInstance } from "i18next";
import { I18nextProvider } from "react-i18next";
import translations from "../../../../../../../locales/manual/en.json";
import { LanguageSwitcher, LanguageSwitcherContext } from "../PathInputOperator";

const i18n = createInstance();

beforeAll(async () => {
    await i18n.init({ lng: "en", resources: { en: { translation: translations } } });
});

it("names the selected language filter and its search input and connects its explanation", async () => {
    const user = userEvent.setup();
    render(
        <I18nextProvider i18n={i18n}>
            <LanguageSwitcherContext.Provider value={{ showLanguageFilterButton: true }}>
                <LanguageSwitcher initialLanguage="en" onLanguageChange={jest.fn()} />
            </LanguageSwitcherContext.Provider>
        </I18nextProvider>,
    );

    const selectButton = screen.getByRole("button", { name: "Filter by language, English selected" });
    await user.tab();
    expect(selectButton).toHaveFocus();
    expect(selectButton).toHaveAccessibleDescription(
        "Set a language filter. Only values for the chosen language will be fetched.",
    );
    fireEvent.click(selectButton);
    const searchInput = screen.getByRole("combobox", { name: "Search languages" });
    await waitFor(() => expect(searchInput).toHaveFocus());
    expect(searchInput).toHaveAttribute("aria-controls", screen.getByRole("listbox").id);
    expect(document.getElementById(searchInput.getAttribute("aria-activedescendant")!)).toEqual(
        screen.getByRole("option", { name: "en" }),
    );
    await user.keyboard("{ArrowDown}");
    expect(document.getElementById(searchInput.getAttribute("aria-activedescendant")!)).toEqual(
        screen.getByRole("option", { name: "de" }),
    );
    expect(document.querySelector('[data-test-id="language-filter-remove"]')).toHaveAttribute("role", "option");
    fireEvent.change(searchInput, { target: { value: "es-MX" } });
    await user.keyboard("{ArrowDown}");
    expect(document.getElementById(searchInput.getAttribute("aria-activedescendant")!)).toEqual(
        screen.getByRole("option", { name: "es-MX" }),
    );
    fireEvent.change(searchInput, { target: { value: "en" } });
    expect(document.getElementById(searchInput.getAttribute("aria-activedescendant")!)).toEqual(
        screen.getByRole("option", { name: "en" }),
    );
    fireEvent.change(searchInput, { target: { value: "de" } });
    await user.keyboard("{Enter}");
    expect(selectButton).toHaveAccessibleName("Filter by language, German selected");
    await waitFor(() => expect(selectButton).toHaveFocus());
    await user.click(selectButton);
    await user.clear(screen.getByRole("combobox", { name: "Search languages" }));
    await user.click(screen.getByRole("option", { name: "No language filter" }));
    expect(screen.getByRole("button", { name: "Filter by language, no filter." })).toHaveFocus();
});

it("announces the language filter when tabbing to it without a selected language", async () => {
    const user = userEvent.setup();
    render(
        <I18nextProvider i18n={i18n}>
            <LanguageSwitcherContext.Provider value={{ showLanguageFilterButton: true }}>
                <LanguageSwitcher initialLanguage={undefined} onLanguageChange={jest.fn()} />
            </LanguageSwitcherContext.Provider>
        </I18nextProvider>,
    );

    const selectButton = screen.getByRole("button", { name: "Filter by language, no filter." });
    await user.tab();
    expect(selectButton).toHaveFocus();
    expect(selectButton).toHaveAccessibleDescription(
        "Set a language filter. Only values for the chosen language will be fetched.",
    );
});
