import React from "react";
import { shallow, mount } from 'enzyme';
import DiscardChangesDialog from '../../../src/HierarchicalMapping/elements/DiscardChangesDialog';

const handleDiscardCancelMock = jest.fn();
const handleDiscardConfirmMock = jest.fn();
const props = {
    numberEditingElements: 2,
    handleDiscardCancel: handleDiscardCancelMock,
    handleDiscardConfirm: handleDiscardConfirmMock
};

const getWrapper = (renderer = shallow) => renderer(
    <DiscardChangesDialog {...props} />
);


const selectors = {
    DISCARD_BUTTON: "button.ecc-hm-accept-discard",
    CANCEL_BUTTON: "button.ecc-hm-cancel-discard",
};

describe("DiscardChangesDialog Component", () => {
    
    describe("on user interaction, ",() => {
        
        let wrapper;
        beforeEach(() => {
            wrapper = getWrapper(mount);
        });
        
        it("should handleDiscardConfirm called, when click on Discard button", () => {
            wrapper.find(selectors.DISCARD_BUTTON).simulate('click');
            expect(handleDiscardConfirmMock).toHaveBeenCalled();
        });
    
        it("should handleDiscardCancel called, when click on Cancel button", () => {
            wrapper.find(selectors.CANCEL_BUTTON).simulate('click');
            expect(handleDiscardCancelMock).toHaveBeenCalled();
        });
        
        afterEach(() => {
            wrapper.unmount();
        })
    });
});
