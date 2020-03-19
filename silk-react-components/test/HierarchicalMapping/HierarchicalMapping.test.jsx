import HierarchicalMapping from '../../src/HierarchicalMapping/HierarchicalMapping';
import {URI} from "ecc-utils";



describe("Hierarchical Mapping Component", () => {
    describe("Rule routing",() => {
        it("should route correctly", () => {
            const updatedUrl = (currentUrl, newRuleId) => {
                return HierarchicalMapping.updateMappingEditorUrl(new URI(currentUrl), newRuleId);
            };
            expect(updatedUrl("/transform/project1/transform2/editor/", "test"))
                .toEqual('/transform/project1/transform2/editor/rule/test');
            expect(updatedUrl("/transform/project1/transform2/editor", "test"))
                .toEqual('/transform/project1/transform2/editor/rule/test');
            expect(updatedUrl("/transform/project1/transform2/editor/rule/old", "test"))
                .toEqual('/transform/project1/transform2/editor/rule/test');
            expect(updatedUrl("/transform/project1/transform2/editor/rule/editor", "test"))
                .toEqual('/transform/project1/transform2/editor/rule/test');
            expect(updatedUrl("/transform/project1/transform2/editor/rule/editor/", "test"))
                .toEqual('/transform/project1/transform2/editor/rule/test');
            expect(updatedUrl("/transform/project1/transform2/editor/x/y/z", "test"))
                .toEqual('/transform/project1/transform2/editor/rule/test');
            expect(updatedUrl("/transform/project1/transform2/editor/illegal", "test"))
                .toEqual('/transform/project1/transform2/editor/rule/test');
            expect(updatedUrl("/transform/project1/transform2/editor/rule/transform", "test"))
                .toEqual('/transform/project1/transform2/editor/rule/test');
        });
    });
});
