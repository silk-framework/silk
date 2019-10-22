import React from "react";
import { shallow } from 'enzyme';
import MessageHandler from '../../../src/HierarchicalMapping/Components/MessageHandler';
import { Alert, Error, Info, Success, Warning } from '@eccenca/gui-elements';


const getWrapper = (renderer = shallow) => renderer(
    <MessageHandler/>
);


describe("MessageHandler Component", () => {
    describe("on component mounted, ",() => {
        it("should render Alert component, when `errorType` is equal to `alert`", () => {
            const wrapper = getWrapper(shallow);
            wrapper.setState({
                errorMessages: [
                    {
                        errorType: 'alert',
                        message: 'lorem'
                    }
                ]
            });
            expect(wrapper.find(Alert)).toHaveLength(1);
        });
        
        it("should render Error component, when `errorType` is equal to `error`", () => {
            const wrapper = getWrapper(shallow);
            wrapper.setState({
                errorMessages: [
                    {
                        errorType: 'error',
                        message: 'lorem'
                    }
                ]
            });
            expect(wrapper.find(Error)).toHaveLength(1);
        });
    
        it("should render Info component, when `errorType` is equal to `info`", () => {
            const wrapper = getWrapper(shallow);
            wrapper.setState({
                errorMessages: [
                    {
                        errorType: 'info',
                        message: 'lorem'
                    }
                ]
            });
            expect(wrapper.find(Info)).toHaveLength(1);
        });
    
        it("should render Success component, when `errorType` is equal to `success`", () => {
            const wrapper = getWrapper(shallow);
            wrapper.setState({
                errorMessages: [
                    {
                        errorType: 'success',
                        message: 'lorem'
                    }
                ]
            });
            expect(wrapper.find(Success)).toHaveLength(1);
        });
    
        it("should render Warning component, when `errorType` is equal to `warning`", () => {
            const wrapper = getWrapper(shallow);
            wrapper.setState({
                errorMessages: [
                    {
                        errorType: 'warning',
                        message: 'lorem'
                    }
                ]
            });
            expect(wrapper.find(Warning)).toHaveLength(1);
        });
        
    });
});
