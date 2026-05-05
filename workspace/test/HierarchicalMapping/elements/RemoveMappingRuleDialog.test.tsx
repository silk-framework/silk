import React from "react";
import RemoveMappingRuleDialog from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/elements/RemoveMappingRuleDialog";
import { clickFoundElement, renderWrapper } from "../../integration/TestHelper";
import { cleanup } from "@testing-library/react";

const handleRemoveCancelMock = jest.fn();
const handleRemoveConfirmMock = jest.fn();
const props = {
    label: "",
    numberEditingElements: 2,
    handleCancelRemove: handleRemoveCancelMock,
    handleConfirmRemove: handleRemoveConfirmMock,
};

const getWrapper = () => renderWrapper(<RemoveMappingRuleDialog {...props} />);

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

        it("should handleDiscardConfirm called, when click on Discard button", async () => {
            // We have to use body, since the dialog runs in a portal directly located under body
            clickFoundElement(window.document.body, selectors.REMOVE_BUTTON);
            expect(handleRemoveConfirmMock).toHaveBeenCalled();
        });

        it("should handleDiscardCancel called, when click on Cancel button", () => {
            clickFoundElement(window.document.body, selectors.CANCEL_BUTTON);
            expect(handleRemoveCancelMock).toHaveBeenCalled();
        });

        afterEach(() => {
            wrapper.unmount();
            cleanup();
        });
    });
});
