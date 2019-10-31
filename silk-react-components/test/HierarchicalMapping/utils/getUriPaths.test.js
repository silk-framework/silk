import getPathsRecursive from '../../../src/HierarchicalMapping/utils/getUriPaths';

describe('getPathsRecursive function', () => {
    it('should return empty array, when operators is empty', () => {
        expect(getPathsRecursive()).toEqual([]);
    });
    
    it('should return array with path, when operators have `path` property', () => {
        expect(getPathsRecursive({
            path: 'path'
        })).toEqual([
            'path'
        ]);
    });
    
    it('should return array of functions, when operators have `function` and `inputs` property', () => {
        const fn = () => {};
        expect(getPathsRecursive({
            function: fn,
            inputs: [
                { path: '/' },
                { path: '/test' }
            ]
        })).toEqual([
            '/',
            '/test'
        ]);
    });
    
    it('should return array of functions recursively, when operators have `function` and `inputs` property', () => {
        const fn = () => {};
        expect(getPathsRecursive({
            function: fn,
            inputs: [
                {
                    path: '/',
                    function: fn,
                    inputs: [{
                        path: '/nested'
                    }]
                },
                { path: '/test' }
            ]
        })).toEqual([
            '/',
            '/nested',
            '/test'
        ]);
    });
});
