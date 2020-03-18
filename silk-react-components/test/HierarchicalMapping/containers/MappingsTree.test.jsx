import MappingsTree from '../../../src/HierarchicalMapping/containers/MappingsTree';

describe("Mappings Tree Component", () => {
    it("should calculate expanded parent nodes based on current rule", () => {
        const objectMapping = (id, propertyMappings = [], type = "object") => {
            return {
                id: id,
                rules: {
                    propertyRules: propertyMappings
                },
                type: type
            };
        };
        const currentId = "current";
        const objectMapping1 = objectMapping("object1", [objectMapping("object1b", [objectMapping(currentId)])]);
        const objectMapping2 = objectMapping("object2", [objectMapping("object2b", [objectMapping("notCurrent")])]);
        const tree = objectMapping("root", [objectMapping1, objectMapping2], "root");
        expect(MappingsTree.extractParentIds(tree, currentId)).toEqual(["root", "object1", "object1b"])
    });
});
