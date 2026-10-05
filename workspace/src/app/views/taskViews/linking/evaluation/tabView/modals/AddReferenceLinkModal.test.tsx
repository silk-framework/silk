import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createInstance } from "i18next";
import { I18nextProvider } from "react-i18next";

import "@testing-library/jest-dom";

import translations from "../../../../../../../locales/manual/en.json";

import { AddReferenceLinkModal } from "./AddReferenceLinkModal";

const i18n = createInstance();

beforeAll(async () => {
    await i18n.init({ lng: "en", resources: { en: { translation: translations } } });
});

it("changes the reference link type with the keyboard", async () => {
    const user = userEvent.setup();
    render(
        <I18nextProvider i18n={i18n}>
            <AddReferenceLinkModal projectId="project" linkingTaskId="task" onClose={jest.fn()} />
        </I18nextProvider>,
    );

    const combobox = screen.getByRole("combobox", { name: "Type" });
    combobox.focus();
    await user.keyboard("{Enter}{ArrowDown}");
    expect(document.getElementById(combobox.getAttribute("aria-activedescendant")!)).toEqual(
        screen.getByRole("option", { name: "Declined" }),
    );
    await user.keyboard("{Enter}");
    await waitFor(() =>
        expect(document.querySelector('[data-test-id="reference-links-types-select"]')).toHaveTextContent("Declined"),
    );
});
