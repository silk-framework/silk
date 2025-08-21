import React from "react";
import DiscardChangesDialog from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/elements/DiscardChangesDialog";
import { fireEvent, render } from "@testing-library/react";
import { findElement } from "../../integration/TestHelper";

const handleDiscardCancelMock = jest.fn();
const handleDiscardConfirmMock = jest.fn();
const props = {
    numberEditingElements: 2,
    handleDiscardCancel: handleDiscardCancelMock,
    handleDiscardConfirm: handleDiscardConfirmMock,
};

const getWrapper = (renderer = shallow) => renderer(<DiscardChangesDialog {...props} />);

const selectors = {
    DISCARD_BUTTON: "button.ecc-hm-accept-discard",
    CANCEL_BUTTON: "button.ecc-hm-cancel-discard",
};

describe("DiscardChangesDialog Component", () => {
    describe("on user interaction, ", () => {
        let wrapper;
        beforeEach(() => {
            wrapper = getWrapper(render);
        });

        it("should handleDiscardConfirm called, when click on Discard button", () => {
            fireEvent.click(findElement(wrapper, selectors.DISCARD_BUTTON));
            expect(handleDiscardConfirmMock).toHaveBeenCalled();
        });

        it("should handleDiscardCancel called, when click on Cancel button", () => {
            fireEvent.click(findElement(wrapper, selectors.CANCEL_BUTTON));
            expect(handleDiscardCancelMock).toHaveBeenCalled();
        });

        afterEach(() => {
            wrapper.unmount();
        });
    });
});
