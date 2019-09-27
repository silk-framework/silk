import {
    getRuleLabel} from '../../src/HierarchicalMapping/utils/getRuleLabel';
import { isClonableRule, isCopiableRule, isObjectMappingRule } from '../../src/HierarchicalMapping/utils/constants';
import { trimValue } from '../../src/HierarchicalMapping/utils/trimValue';

describe('helpers.js', () => {
    describe('should isCopiableRule function working', () => {
        it('when type equal to direct', () => {
            expect(isCopiableRule('direct')).toEqual(true);
        });
        it('when type equal to object', () => {
            expect(isCopiableRule('object')).toEqual(true);
        });
        it('when type equal to complex', () => {
            expect(isCopiableRule('complex')).toEqual(true);
        });
        it('when type equal to root', () => {
            expect(isCopiableRule('root')).toEqual(true);
        });
        it('when type is incorrect', () => {
            expect(isCopiableRule('dummy')).toEqual(false);
        })
    });
    
    describe('should isClonableRule function working', () => {
        it('when type equal to direct', () => {
            expect(isClonableRule('direct')).toEqual(true);
        });
        it('when type equal to object', () => {
            expect(isClonableRule('object')).toEqual(true);
        });
        it('when type equal to complex', () => {
            expect(isClonableRule('complex')).toEqual(true);
        });
        it('when type is incorrect', () => {
            expect(isClonableRule('dummy')).toEqual(false);
        })
    });
    
    describe('should isObjectMappingRule function working', () => {
        it('when type equal to direct', () => {
            expect(isObjectMappingRule('root')).toEqual(true);
        });
        it('when type equal to object', () => {
            expect(isObjectMappingRule('object')).toEqual(true);
        });
        it('when type is incorrect', () => {
            expect(isClonableRule('dummy')).toEqual(false);
        })
    });
    
    describe('trimValue function', () => {
        describe('should return trimmed value', () => {
            it('when pass the object within the `value` property', () => {
                expect(trimValue({
                    value: '  something'
                })).toEqual({value: 'something'});
            });
    
            it('when pass the object within the `label` property', () => {
                expect(trimValue({
                    label: '  something'
                })).toEqual({label: 'something'});
            });
    
            it('when pass the string', () => {
                expect(trimValue('  something')).toEqual('something');
            });
        });
    
        it('should return the same value on default case', () => {
            expect(trimValue({
                variable: '  something'
            })).toEqual({variable: '  something'});
        });
    });
    
    describe('getRuleLabel function', () => {
        describe('should return correct parsed object', () => {
            it('when `label` property is empty', () => {
                expect(getRuleLabel({
                    label: "",
                    uri: "<urn:ruleProperty:birthdate>"
                })).toEqual({
                    displayLabel: "Birthdate",
                    uri: "urn:ruleProperty:birthdate"
                });
            });
            it('when `label` property presented', () => {
                expect(getRuleLabel({
                    label: "LABEL",
                    uri: "<urn:ruleProperty:birthdate>"
                })).toEqual({
                    displayLabel: "LABEL",
                    uri: "urn:ruleProperty:birthdate"
                });
            });
        })
        
        
    });
});
