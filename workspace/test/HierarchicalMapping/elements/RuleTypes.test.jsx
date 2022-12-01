import React from "react";
import { mount, shallow } from 'enzyme';
import { NotAvailable } from 'gui-elements-deprecated';
import { ThingName } from '../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/ThingName';
import RuleTypes from '../../../src/app/views/pages/MappingEditor/HierarchicalMapping/elements/RuleTypes';

const getWrapper = (renderer = shallow, props = {}) => renderer(
    <RuleTypes {...props} />
);

describe("RuleTypes Component", () => {
    describe("on components mounted ", () => {
        describe('when `rule.type` equal to `object` ', () => {
            it('and `rule.rules.typeRules` presented, should return array of <ThingName />', () => {
                const wrapper = getWrapper(shallow, {
                    rule: {
                        type: 'object',
                        rules: {
                            typeRules: [{
                                typeUri: 'simple_uri',
                            }, {
                                typeUri: 'simple_uri1',
                            }]
                        }
                    }
                });
                expect(wrapper.find(ThingName)).toHaveLength(2);
            });

            it('and `rule.rules.typeRules` Empty, should return <NotAvailable />', () => {
                const wrapper = getWrapper(shallow, {
                    rule: {
                        type: 'object',
                        rules: {
                            typeRules: []
                        }
                    }
                });
                expect(wrapper.find(NotAvailable)).toHaveLength(1);
            });
        });

        describe('should return text, when `rule.type` equal to `direct` or `complex` ', () => {
            it('and `rule.mappingTarget.valueType.nodeType` presented', () => {
                const wrapper = getWrapper(shallow, {
                    rule: {
                        type: 'direct',
                        mappingTarget: {
                            valueType: {
                                nodeType: 'dummy'
                            }
                        }
                    }
                });
                expect(wrapper.find('span').text()).toEqual('dummy');
            });

            xit('and `rule.mappingTarget.valueType.nodeType` NOT presented', () => {
                const wrapper = getWrapper(shallow, {
                    rule: {
                        type: 'direct',
                        mappingTarget: {
                            valueType: {
                            }
                        }
                    }
                });
                expect(wrapper.find(NotAvailable)).toHaveLength(1);
            });

            it('and language prefix presented', () => {
                const wrapper = getWrapper(shallow, {
                    rule: {
                        type: 'complex',
                        mappingTarget: {
                            valueType: {
                                nodeType: 'dummy',
                                lang: 'prefix'
                            }
                        }
                    }
                });
                expect(wrapper.find('span').text()).toEqual('dummy (prefix)');
            });
        });

        it('should return proper types for root mapping', () => {
            const wrapperNoType = getWrapper(mount, {
                rule: {
                    type: 'root',
                }
            });
            expect(wrapperNoType.find('span').contains(<NotAvailable />)).toBeTruthy()
            expect(wrapperNoType.html()).toContain("not available")
            const wrapperWithType = getWrapper(mount, {
                rule: {
                    type: 'root',
                    rules: {
                        typeRules: [{
                            "typeUri" : "<https://domain.com/expectedType>"
                        }]
                    }
                }
            });
            expect(wrapperWithType.html()).toContain(">expectedType<")
        });
    });
});
