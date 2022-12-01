import { getLocalNameLabelFromPath } from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/containers/SuggestionNew/utils";

describe("getLocalNameLabelFromPath", () => {
    it("should filter out non-allowed characters in URIs", () => {
        expect(getLocalNameLabelFromPath("<http://path/localPath>")).toBe("localPath");
        expect(getLocalNameLabelFromPath("<http://path/localPath>>>")).toBe("localPath");
    });
    it("should get local name of the last path part", () => {
        expect(getLocalNameLabelFromPath("<http://path/noThis>/<http://path/andNotThis>/<http://path/butThis>")).toBe(
            "butThis"
        );
        expect(getLocalNameLabelFromPath("<http://server/parentPath/path#hashName")).toBe("hashName");
        expect(getLocalNameLabelFromPath("urn:middle:localPart")).toBe("localPart");
    });
});
