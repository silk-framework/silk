import React from "react";
import { shallow, mount } from 'enzyme';
import RemoveMappingRuleDialog from '../../../src/HierarchicalMapping/elements/RemoveMappingRuleDialog';

const handleRemoveCancelMock = jest.fn();
const handleRemoveConfirmMock = jest.fn();
const props = {
    numberEditingElements: 2,
    handleCancelRemove: handleRemoveCancelMock,
    handleConfirmRemove: handleRemoveConfirmMock
};

const getWrapper = (renderer = shallow) => renderer(
    <RemoveMappingRuleDialog {...props} />
);

const selectors = {
    REMOVE_BUTTON: "button.ecc-hm-delete-accept",
    CANCEL_BUTTON: "button.ecc-hm-delete-cancel",
};

describe("RemoveMappingRuleDialog Component", () => {
    describe("on user interaction, ",() => {
        
        let wrapper;
        beforeEach(() => {
            wrapper = getWrapper(mount);
        });
        
        it("should handleDiscardConfirm called, when click on Discard button", () => {
            wrapper.find(selectors.REMOVE_BUTTON).simulate('click');
            expect(handleRemoveConfirmMock).toHaveBeenCalled();
        });
    
        it("should handleDiscardCancel called, when click on Cancel button", () => {
            wrapper.find(selectors.CANCEL_BUTTON).simulate('click');
            expect(handleRemoveCancelMock).toHaveBeenCalled();
        });
        
        afterEach(() => {
            wrapper.unmount();
        })
    });
});
