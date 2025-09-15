import React from "react";
import RemoveMappingRuleDialog from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/elements/RemoveMappingRuleDialog";
import { render } from "@testing-library/react";
import { clickFoundElement, findElement } from "../../integration/TestHelper";

const handleRemoveCancelMock = jest.fn();
const handleRemoveConfirmMock = jest.fn();
const props = {
    numberEditingElements: 2,
    handleCancelRemove: handleRemoveCancelMock,
    handleConfirmRemove: handleRemoveConfirmMock,
};

const getWrapper = () => render(<RemoveMappingRuleDialog {...props} />);

const selectors = {
    REMOVE_BUTTON: "button.ecc-hm-delete-accept",
    CANCEL_BUTTON: "button.ecc-hm-delete-cancel",
};

describe("RemoveMappingRuleDialog Component", () => {
    describe("on user interaction, ", () => {
        let wrapper;
        beforeEach(() => {
            wrapper = getWrapper();
        });

        it("should handleDiscardConfirm called, when click on Discard button", () => {
            clickFoundElement(wrapper, selectors.REMOVE_BUTTON);
            expect(handleRemoveConfirmMock).toHaveBeenCalled();
        });

        it("should handleDiscardCancel called, when click on Cancel button", () => {
            clickFoundElement(wrapper, selectors.CANCEL_BUTTON);
            expect(handleRemoveCancelMock).toHaveBeenCalled();
        });

        afterEach(() => {
            wrapper.unmount();
        });
    });
});
