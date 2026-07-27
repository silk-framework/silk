import React from "react";
import "@testing-library/jest-dom";
import { createMemoryHistory } from "history";
import { screen, waitFor, RenderResult } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import mockAxios from "../../../../__mocks__/axios";
import { byTestId, renderWrapper } from "../../../TestHelper";
import { CreateSplitButton } from "../../../../../src/app/views/layout/Header/CreateSplitButton";
import { artefactTypes } from "../../../../../src/app/views/layout/Header/artefactTypes";
import { commonOp } from "@ducks/common";

// Capitalised label as the menu renders it (labels come from the registry's `labelKey`, which for the
// first-class types resolves to the type name; the button applies `uppercaseFirstChar`).
const expectedLabel = (dtype: string) => dtype.charAt(0).toUpperCase() + dtype.slice(1);

describe("CreateSplitButton", () => {
    let wrapper: RenderResult;
    let setDTypeSpy: jest.SpyInstance;

    const render = () => {
        const history = createMemoryHistory();
        wrapper = renderWrapper(<CreateSplitButton />, history, {});
        return wrapper;
    };

    beforeEach(() => {
        // Spy on the action creator the button dispatches; call through so the reducer still runs.
        setDTypeSpy = jest.spyOn(commonOp, "setSelectedArtefactDType");
    });

    afterEach(() => {
        wrapper?.unmount();
        setDTypeSpy.mockRestore();
        mockAxios.reset();
    });

    it("should dispatch setSelectedArtefactDType('all') when the primary Create button is clicked", async () => {
        render();
        const user = userEvent.setup();

        await user.click(wrapper.baseElement.querySelector(byTestId("create-item-btn"))!);

        expect(setDTypeSpy).toHaveBeenCalledTimes(1);
        expect(setDTypeSpy).toHaveBeenCalledWith("all");
    });

    it("should render one caret-menu entry per registry type, labelled from the registry, in order", async () => {
        render();
        const user = userEvent.setup();

        await user.click(screen.getByRole("button", { name: /Create …$/ }));

        const items = await screen.findAllByRole("menuitem");
        expect(items).toHaveLength(artefactTypes.length);
        expect(items.map((item) => item.textContent)).toEqual(artefactTypes.map((type) => expectedLabel(type.dtype)));
    });

    it("should dispatch the per-type dtype when the matching caret-menu entry is clicked", async () => {
        for (const type of artefactTypes) {
            render();
            const user = userEvent.setup();

            await user.click(screen.getByRole("button", { name: /Create …$/ }));
            const item = await screen.findByRole("menuitem", { name: expectedLabel(type.dtype) });
            await user.click(item);

            await waitFor(() => {
                expect(setDTypeSpy).toHaveBeenCalledWith(type.dtype);
            });

            wrapper.unmount();
            setDTypeSpy.mockClear();
        }
    });
});
