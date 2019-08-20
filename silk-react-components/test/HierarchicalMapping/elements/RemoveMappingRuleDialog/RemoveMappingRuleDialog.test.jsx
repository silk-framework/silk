import React from 'react';
import { shallow } from "enzyme";
import { expect } from 'chai';
import Enzyme from 'enzyme/build';
import Adapter from 'enzyme-adapter-react-15/build';
import RemoveMappingRuleDialog
    from '../../../../src/HierarchicalMapping/elements/RemoveMappingRuleDialog/RemoveMappingRuleDialog';

Enzyme.configure({ adapter: new Adapter() });

const props = {
    mappingType: 'test',
    handleConfirmRemove: () => {},
    handleCancelRemove: () => {},
};

describe("HierarchicalMapping", () => {
    describe("RemoveMappingRuleDialog", () => {
        it("should show dialog", () => {
            const wrapper = shallow(
                <RemoveMappingRuleDialog
                    {...props}
                />
            );
            expect(wrapper.find('ConfirmationDialog')).to.have.lengthOf(1);
        });
        it("should have title property", () => {
            const wrapper = shallow(
                <RemoveMappingRuleDialog
                    {...props}
                />
            );
            expect(wrapper.find('ConfirmationDialog').props().title).to.not.equal(undefined);
        });
        it("should have working confirm button", () => {
            const handleConfirmRemove = () => { return 'fake function confirm' } ;
            const wrapper = shallow(
                <RemoveMappingRuleDialog
                    {...props}
                    handleConfirmRemove={handleConfirmRemove}
                />
            );
            expect(wrapper.find('ConfirmationDialog').props().confirmButton.props.onClick).to.equal(handleConfirmRemove);
        });
        it("should have working cancel button", () => {
            const handleCancelRemove = () => { return 'fake function cancel' } ;
            const wrapper = shallow(
                <RemoveMappingRuleDialog
                    {...props}
                    handleCancelRemove={handleCancelRemove}
                />
            );
            expect(wrapper.find('ConfirmationDialog').props().cancelButton.props.onClick).to.equal(handleCancelRemove);
        });
        it("should have text content", () => {
            const wrapper = shallow(
                <RemoveMappingRuleDialog
                    {...props}
                />
            );
            expect(wrapper.find('p')).to.have.lengthOf(1);
        });
        it("should have different text content regarding to mapping type", () => {
            const wrapper1 = shallow(
                <RemoveMappingRuleDialog
                    {...props}
                />
            );
            const wrapper2 = shallow(
                <RemoveMappingRuleDialog
                    {...props}
                    mappingType={'object'}
                />
            );
            expect(wrapper1.find('p')).to.not.equal(wrapper2.find('p'));
        });
    });
});
