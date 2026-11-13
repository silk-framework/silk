import React from "react";
import "@testing-library/jest-dom";
import { act, fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createInstance } from "i18next";
import { I18nextProvider } from "react-i18next";
import translations from "../../../../../../../../locales/manual/en.json";
import PrefixList from "../PrefixList";
import { SuggestionListContext } from "../SuggestionContainer";
import TargetList from "./TargetList";
import TypesList from "./TypesList";

jest.mock("../SuggestionContainer", () => ({
    SuggestionListContext: require("react").createContext({
        isFromDataset: true,
        search: "",
        portalContainer: undefined,
    }),
}));

const i18n = createInstance();

beforeAll(async () => {
    await i18n.init({ lng: "en", resources: { en: { translation: translations } } });
});

const renderWithTranslations = (element: React.ReactElement) =>
    render(<I18nextProvider i18n={i18n}>{element}</I18nextProvider>);

it("announces the selected target property for its source and names its search input", async () => {
    const user = userEvent.setup();
    const target = { uri: "urn:target", label: "Target A", type: "value" as const, confidence: 1, _selected: true };
    renderWithTranslations(<TargetList targets={[target]} onChange={jest.fn()} elementLabel="Source A" />);

    const selectButton = screen.getByRole("button", { name: "Target property for Source A, Target A selected" });
    await act(async () => user.tab());
    expect(selectButton).toHaveFocus();
    await act(async () => fireEvent.click(selectButton));
    expect(screen.getByRole("combobox", { name: "Search target properties" })).toBeInTheDocument();
});

it("announces the selected target URI when it has no label", () => {
    const target = { uri: "urn:target", type: "value" as const, confidence: 1, _selected: true };
    renderWithTranslations(<TargetList targets={[target]} onChange={jest.fn()} elementLabel="Source A" />);

    expect(
        screen.getByRole("button", { name: "Target property for Source A, urn:target selected" }),
    ).toBeInTheDocument();
});

it("names the source path selector when the suggestion table is reversed", async () => {
    const targets = [
        { uri: "urn:source-a", label: "Source A", type: "value" as const, confidence: 1, _selected: true },
        { uri: "urn:source-b", label: "Source B", type: "value" as const, confidence: 0.5, _selected: false },
    ];
    renderWithTranslations(
        <SuggestionListContext.Provider
            value={{
                portalContainer: undefined,
                exampleValues: {},
                search: "",
                isFromDataset: false,
                vocabulariesAvailable: true,
            }}
        >
            <TargetList targets={targets} onChange={jest.fn()} elementLabel="Target A" />
        </SuggestionListContext.Provider>,
    );

    const selectButton = screen.getByRole("button", { name: "Source path for Target A, Source A selected" });
    await act(async () => fireEvent.click(selectButton));
    expect(screen.getByRole("combobox", { name: "Search source paths" })).toBeInTheDocument();
});

it("announces the selected mapping type for its source", () => {
    renderWithTranslations(<TypesList selected="value" onChange={jest.fn()} elementLabel="Source A" />);

    expect(screen.queryByRole("button", { name: "Mapping type for Source A, value selected" })).not.toBeInTheDocument();
    expect(screen.getByRole("combobox", { name: "Mapping type for Source A, value selected" })).toBeInTheDocument();
});

it("navigates and selects a mapping type with the keyboard", async () => {
    const user = userEvent.setup();
    const onChange = jest.fn();
    const ControlledTypesList = () => {
        const [selected, setSelected] = React.useState<"value" | "object">("value");
        return (
            <TypesList
                selected={selected}
                onChange={(type) => {
                    onChange(type);
                    setSelected(type);
                }}
                elementLabel="Source A"
            />
        );
    };
    renderWithTranslations(<ControlledTypesList />);

    await act(async () => user.tab());
    const combobox = screen.getByRole("combobox", { name: "Mapping type for Source A, value selected" });
    expect(combobox).toHaveFocus();
    await act(async () => user.keyboard("{Enter}"));
    await act(async () => user.keyboard("{ArrowDown}"));
    expect(document.getElementById(combobox.getAttribute("aria-activedescendant")!)).toEqual(
        screen.getByRole("option", { name: "object" }),
    );
    await act(async () => user.keyboard("{Enter}"));
    expect(onChange).toHaveBeenCalledWith("object");
    expect(combobox).toHaveAccessibleName("Mapping type for Source A, object selected");
    expect(combobox).toHaveFocus();
});

it("names the prefix selector and its search input", async () => {
    renderWithTranslations(
        <PrefixList prefixes={[{ key: "ex", uri: "urn:example" }]} selectedPrefix="urn:example" onChange={jest.fn()} />,
    );

    const selectButton = screen.getByRole("button", { name: "Use known prefix" });
    expect(selectButton).toBeInTheDocument();
    await act(async () => fireEvent.click(selectButton));
    expect(screen.getByRole("combobox", { name: "Search prefixes" })).toBeInTheDocument();
});
