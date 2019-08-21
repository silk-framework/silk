import React from 'react';
import { shallow } from "enzyme";
import { expect } from 'chai';
import Enzyme from 'enzyme/build';
import Adapter from 'enzyme-adapter-react-15/build';
import DiscardChangesDialog
    from '../../../../src/HierarchicalMapping/elements/DiscardChangesDialog';

Enzyme.configure({ adapter: new Adapter() });

const props = {
    numberEditingElements: 1,
    handleDiscardConfirm: () => {},
    handleDiscardCancel: () => {},
};

describe("HierarchicalMapping", () => {
    describe("DiscardChangesDialog", () => {
        it("should show dialog", () => {
            const wrapper = shallow(
                <DiscardChangesDialog
                    {...props}
                />
            );
            expect(wrapper.find('ConfirmationDialog')).to.have.lengthOf(1);
        });
        it("should have title property", () => {
            const wrapper = shallow(
                <DiscardChangesDialog
                    {...props}
                />
            );
            expect(wrapper.find('ConfirmationDialog').props().title).to.not.equal(undefined);
        });
        it("should have working confirm button", () => {
            const handleDiscardConfirm = () => { return 'fake function confirm' } ;
            const wrapper = shallow(
                <DiscardChangesDialog
                    {...props}
                    handleDiscardConfirm={handleDiscardConfirm}
                />
            );
            expect(wrapper.find('ConfirmationDialog').props().confirmButton.props.onClick).to.equal(handleDiscardConfirm);
        });
        it("should have working cancel button", () => {
            const handleDiscardCancel = () => { return 'fake function cancel' } ;
            const wrapper = shallow(
                <DiscardChangesDialog
                    {...props}
                    handleDiscardCancel={handleDiscardCancel}
                />
            );
            expect(wrapper.find('ConfirmationDialog').props().cancelButton.props.onClick).to.equal(handleDiscardCancel);
        });
        it("should have text content", () => {
            const wrapper = shallow(
                <DiscardChangesDialog
                    {...props}
                />
            );
            expect(wrapper.find('p')).to.have.lengthOf(1);
        });
        it("should have different text content regarding to mapping type", () => {
            const wrapper1 = shallow(
                <DiscardChangesDialog
                    {...props}
                />
            );
            const wrapper2 = shallow(
                <DiscardChangesDialog
                    {...props}
                    numberEditingElements={2}
                />
            );
            expect(wrapper1.find('p')).to.not.equal(wrapper2.find('p'));
        });
        it("should have text content with specific amount of unchanged elements", () => {
            const wrapper = shallow(
                <DiscardChangesDialog
                    {...props}
                    numberEditingElements={34}
                />
            );
            expect(wrapper.find('p').text()).to.include(34);
        });
    });
});
