import { convertToUri } from '../../../src/HierarchicalMapping/utils/convertToUri';

describe('convertToUri function', () => {
    it('should return Object with keys of passed `label, labelKey, valueKey`', () => {
        const res = convertToUri({
            label: 'a',
            labelKey: 'b',
            valueKey: 'c'
        });
        expect(Object.keys(res)).toEqual(['a', 'b', 'c'])
    });
    
    it('should return normalized uri with extra `Normalizing URI to` text, when `Create option (.*)` format is presented', () => {
        const res = convertToUri({
            label: 'Create option "<new uri>"',
            labelKey: 'b',
            valueKey: 'c'
        });
        expect(res).toEqual({
            "Create option \"<new uri>\"": "Normalizing URI to \"%3Cnew%20uri%3E\"",
            b: "Create option \"<new uri>\"",
            c: "Normalizing URI to \"%3Cnew%20uri%3E\""
        })
    });
    
    it('should return normalized uri, when `Create option (.*)` is NOT presented', () => {
        const res = convertToUri({
            label: '<some uri>',
            labelKey: 'b',
            valueKey: 'c'
        });
        expect(res).toEqual({
            "<some uri>": "%3Csome%20uri%3E",
            "b": "<some uri>",
            "c": "%3Csome%20uri%3E"
        })
    });
});
