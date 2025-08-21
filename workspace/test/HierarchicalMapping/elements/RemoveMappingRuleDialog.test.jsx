import React from "react";
import RemoveMappingRuleDialog from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/elements/RemoveMappingRuleDialog";
import { fireEvent, render } from "@testing-library/react";
import { findElement } from "../../integration/TestHelper";

const handleRemoveCancelMock = jest.fn();
const handleRemoveConfirmMock = jest.fn();
const props = {
    numberEditingElements: 2,
    handleCancelRemove: handleRemoveCancelMock,
    handleConfirmRemove: handleRemoveConfirmMock,
};

const getWrapper = (renderer = render) => renderer(<RemoveMappingRuleDialog {...props} />);

const selectors = {
    REMOVE_BUTTON: "button.ecc-hm-delete-accept",
    CANCEL_BUTTON: "button.ecc-hm-delete-cancel",
};

describe("RemoveMappingRuleDialog Component", () => {
    describe("on user interaction, ", () => {
        let wrapper;
        beforeEach(() => {
            wrapper = getWrapper(render);
        });

        it("should handleDiscardConfirm called, when click on Discard button", () => {
            fireEvent.click(findElement(wrapper, selectors.REMOVE_BUTTON));
            expect(handleRemoveConfirmMock).toHaveBeenCalled();
        });

        it("should handleDiscardCancel called, when click on Cancel button", () => {
            fireEvent.click(findElement(wrapper, selectors.CANCEL_BUTTON));
            expect(handleRemoveCancelMock).toHaveBeenCalled();
        });

        afterEach(() => {
            wrapper.unmount();
        });
    });
});
