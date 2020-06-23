import { trimValue } from '../../../src/HierarchicalMapping/utils/trimValue';
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
