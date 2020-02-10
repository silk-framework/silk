import getUriOperatorsRecursive from '../../../src/HierarchicalMapping/utils/getUriOperators';

describe('getUriOperators function', () => {
    it('should return empty array, when operators is empty', () => {
        expect(getUriOperatorsRecursive({})).toEqual([]);
    });
    
    it('should return array with function, when operators have function property', () => {
        const fn = jest.fn();
        expect(getUriOperatorsRecursive({
            function: fn
        })).toEqual([
            fn
        ]);
    });
    
    it('should return array of function, when operators have function property', () => {
        const fn = jest.fn();
        expect(getUriOperatorsRecursive({
            function: fn
        })).toEqual([
            fn
        ]);
    });
    it('should return array of functions, when operators have `inputs` property', () => {
        const fn = jest.fn();
        const input1Fn = jest.fn();
        const input2Fn = jest.fn();
        expect(getUriOperatorsRecursive({
            function: fn,
            inputs: [
                { function: input1Fn },
                { function: input2Fn }
            ]
        })).toEqual([
            input1Fn,
            input2Fn,
            fn
        ]);
    });
    
    it('should return array of functions recursively, when operators have `inputs` property', () => {
        const fn = jest.fn();
        const input1Fn = jest.fn();
        const input2Fn = jest.fn();
        const nestedFn = jest.fn();
        expect(getUriOperatorsRecursive({
            function: fn,
            inputs: [
                {
                    function: input1Fn,
                    inputs: [
                        { function: nestedFn }
                    ]
                },
                { function: input2Fn }
            ]
        })).toEqual([
            nestedFn,
            input1Fn,
            input2Fn,
            fn
        ]);
    });
});
