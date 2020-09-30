import { getRuleLabel } from '../../../src/HierarchicalMapping/utils/getRuleLabel';

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
