import React from "react";
import RemoveMappingRuleDialog
    from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/elements/RemoveMappingRuleDialog";
import {waitFor} from "@testing-library/react";
import {clickFoundElement, renderWrapper} from "../../integration/TestHelper";
import {logWrapperHtml} from "../utils/TestHelpers";

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
    REMOVE_BUTTON: ".ecc-hm-delete-accept",
    CANCEL_BUTTON: ".ecc-hm-delete-cancel",
};

describe("RemoveMappingRuleDialog Component", () => {
    describe("on user interaction, ", () => {
        let wrapper;
        beforeEach(() => {
            wrapper = getWrapper();
        });

        it("should handleDiscardConfirm called, when click on Discard button", async () => {
            logWrapperHtml(wrapper)
            await waitFor(() => {
                clickFoundElement(wrapper, selectors.REMOVE_BUTTON);
            })
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
