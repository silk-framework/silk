import React from "react";
import { shallow } from 'enzyme';
import { SourcePath } from '../../../src/HierarchicalMapping/components/SourcePath';
import { NotAvailable } from '@eccenca/gui-elements';


const getWrapper = (renderer = shallow, args = {}) => renderer(
    <SourcePath {...args} />
);


describe("SourcePath Component", () => {
    describe("on component mounted, ",() => {
        it("should render NotAvailable component, when `sourcePath` is NOT presented in `rule` prop", () => {
            const wrapper = getWrapper(shallow, {
                rule: {}
            });
            expect(wrapper.find(NotAvailable)).toHaveLength(1);
        });
    
        it("should render NotAvailable component, when `sourcePath` is presented in `rule` prop", () => {
            const wrapper = getWrapper(shallow, {
                rule: {
                    sourcePath: 'text'
                }
            });
            expect(wrapper.find('span').text()).toEqual('text')
        });
    });
});
