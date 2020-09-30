import { isClonableRule, isCopiableRule, isRootOrObjectRule } from '../../../src/HierarchicalMapping/utils/constants';

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
        expect(isRootOrObjectRule('root')).toEqual(true);
    });
    it('when type equal to object', () => {
        expect(isRootOrObjectRule('object')).toEqual(true);
    });
    it('when type is incorrect', () => {
        expect(isClonableRule('dummy')).toEqual(false);
    })
});
