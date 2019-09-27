import React from "react";
import { shallow } from 'enzyme';
import RuleTitle from '../../../src/HierarchicalMapping/elements/RuleTitle';
import { NotAvailable } from '@eccenca/gui-elements';
import { ThingName } from '../../../src/HierarchicalMapping/Components/ThingName';

const getWrapper = (renderer = shallow, props = {}) => renderer(
    <RuleTitle {...props} />
);

describe("RuleTitle Component", () => {
    describe("on components mounted ", () => {
        it('should return label, when `metadata.label` presented', () => {
            const wrapper = getWrapper(shallow, {
                rule: {
                    metadata: {
                        label: 'Something'
                    }
                }
            });
            expect(wrapper.find('span').text()).toEqual('Something');
        });
        
        describe('should return <ThingName />', () => {
            let wrapper;
            
            const rule = {
                type: 'object',
                mappingTarget: {
                    uri: 'ANOTHER_URI'
                }
            };
            
            beforeEach(() => {
                wrapper = getWrapper(shallow, {
                    rule
                });
            });
            
            it('when `rule.type` equal to `root` and uri presented', () => {
                const wrapper = getWrapper(shallow, {
                    rule: {
                        type: 'root',
                        rules: {
                            typeRules: [{
                                typeUri: 'simple_uri'
                            }]
                        }
                    }
                });
                expect(wrapper.find(ThingName)).toHaveLength(1);
            });
            
            it('when `rule.type` equal to `object` and uri presented', () => {
                expect(wrapper.find(ThingName)).toHaveLength(1);
            });
            
            it('when `rule.type` equal to `direct` and uri presented', () => {
                rule.type = 'direct';
                wrapper.setProps({rule});
                expect(wrapper.find(ThingName)).toHaveLength(1);
            });
            
            it('when `rule.type` equal to `complex` and uri presented', () => {
                rule.type = 'complex';
                wrapper.setProps({rule});
                expect(wrapper.find(ThingName)).toHaveLength(1);
            });
            
            afterEach(() => {
                wrapper.unmount();
            });
        });
        
        describe('should return <NotAvailable />', () => {
            it('when `type` property missing in rule arg', () => {
                const wrapper = getWrapper(shallow, {
                    rule: {}
                });
                expect(wrapper.find(NotAvailable)).toHaveLength(1);
            });
            
            it('when `rule.type` equal to `root` and uri NOT presented', () => {
                const wrapper = getWrapper(shallow, {
                    rule: {
                        type: 'root',
                        rules: {
                            typeRules: [{
                                typeUri: ''
                            }]
                        }
                    }
                });
                expect(wrapper.find(NotAvailable)).toHaveLength(1);
            });
            
            describe('when uri not presented ', () => {
                let wrapper;
                
                const rule = {
                    type: 'object',
                    mappingTarget: {
                        uri: ''
                    }
                };
                
                beforeEach(() => {
                    wrapper = getWrapper(shallow, {
                        rule
                    });
                });
                
                it('and `rule.type` equal to `object` ', () => {
                    expect(wrapper.find(NotAvailable)).toHaveLength(1);
                });
                
                it('and `rule.type` equal to `direct`', () => {
                    rule.type = 'direct';
                    wrapper.setProps({rule});
                    expect(wrapper.find(NotAvailable)).toHaveLength(1);
                });
                
                it('and `rule.type` equal to `complex` ', () => {
                    rule.type = 'complex';
                    wrapper.setProps({rule});
                    expect(wrapper.find(NotAvailable)).toHaveLength(1);
                });
                
                afterEach(() => {
                    wrapper.unmount();
                });
            });
        });
    });
});
