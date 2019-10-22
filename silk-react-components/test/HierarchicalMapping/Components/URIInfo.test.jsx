import React from "react";
import { shallow } from 'enzyme';
import { NotAvailable } from '@eccenca/gui-elements';
import { URIInfo } from '../../../src/HierarchicalMapping/Components/URIInfo';

const getVocabInfoAsyncMock = jest.fn();

jest.doMock( '../../../src/HierarchicalMapping/store', () => getVocabInfoAsyncMock);
const props = {
    uri: '<superUri>',
    fallback: 'Fallback text',
    field: 'field'
};

const getWrapper = (renderer = shallow, args = props) => renderer(
    <URIInfo {...args} />
);


describe("URIInfo Component", () => {
    describe("on component mounted, ",() => {
        it("should render information text, when text is available from server", () => {
            const wrapper = getWrapper(shallow);
            wrapper.setState({
                info: 'Message'
            });
            
            expect(wrapper.find('span').text()).toEqual('Message')
        });
    
        it("should render fallback text, when text is NOT available from server", () => {
            const wrapper = getWrapper(shallow);
            expect(wrapper.find('span').text()).toEqual('Fallback text')
        });
    
        it("should render NotAvailable component, when fallback not available and `uri` in not string", () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                uri: {},
                fallback: undefined
            });
            expect(wrapper.find(NotAvailable)).toHaveLength(1);
        });
    
        it("should render text from `uri`, when `props.field` equal to 'label'", () => {
            const wrapper = getWrapper(shallow, {
                ...props,
                field: 'label',
                fallback: undefined
            });
            expect(wrapper.find('span').text()).toEqual('superUri')
        });
    });
});
