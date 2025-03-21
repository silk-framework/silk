import React from "react";
import { mount, ReactWrapper } from "enzyme";
import SuggestionContainer from "../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/containers/SuggestionNew/SuggestionContainer";
import { waitFor } from "@testing-library/react";
import {
    byTestId,
    clickElement,
    clickWrapperElement,
    findAll,
    findSingleElement,
    logWrapperHtmlOnError,
    rangeArray,
} from "../../utils/TestHelpers";
import { testWrapper, withMount } from "../../../integration/TestHelper";
import Task from "../../../../src/app/views/pages/Task";
import { createBrowserHistory } from "history";

/** Helper functions to generate test data. */
const prefixToSourcePath = (prefix: string) => prefix;
const prefixToTargetProperty = (prefix: string, nr: number) => `urn:targetProp:${prefix}${nr}`;
const prefixToTargetLabel = (prefix: string, nr: number) => `${prefix} target label ${nr}`;
const prefixToTargetDescription = (prefix: string) => `${prefix} target description`;
const prefixToExampleValues = (prefix: string) => [`${prefix} example value`];
interface ICandidateTemplate {
    // The prefix string of the candidate that is used to generate all values.
    prefix: string;
    // The number of candidates
    nrCandidates: number;
}
/** Generates matching candidates. */
const generateCandidates = (candidateTemplates: ICandidateTemplate[], matchFromDataset: boolean) => {
    return candidateTemplates.map((candidate) => {
        const { prefix, nrCandidates } = candidate;
        return {
            uri: matchFromDataset ? prefixToSourcePath(prefix) : prefixToTargetProperty(prefix, 1),
            candidates: rangeArray(matchFromDataset ? nrCandidates : 1).map((nr) => ({
                uri: matchFromDataset ? prefixToTargetProperty(prefix, nr) : prefixToSourcePath(prefix),
                label: matchFromDataset ? prefixToTargetLabel(prefix, nr) : undefined,
                confidence: 0,
            })),
            description: matchFromDataset ? undefined : prefixToTargetDescription(prefix),
            label: matchFromDataset ? undefined : prefixToTargetLabel(prefix, 1),
        };
    });
};
/** Matching results. */
const candidate = (suffix: string) => `candidate${suffix}`;
const candidate1 = candidate("1");
const candidate2 = candidate("2");
const candidateNonMatches = candidate("nonMatched");
const mockSuggestions = (matchFromDataset: boolean) =>
    generateCandidates(
        [
            { prefix: candidate1, nrCandidates: 1 },
            { prefix: candidate2, nrCandidates: 2 },
            { prefix: candidateNonMatches, nrCandidates: 0 },
        ],
        matchFromDataset
    );
const mockExampleValues = () => {
    const examplesValues = {};
    examplesValues[prefixToSourcePath(candidate1)] = prefixToExampleValues(candidate1);
    examplesValues[prefixToSourcePath(candidate2)] = prefixToExampleValues(candidate2);
    examplesValues[prefixToSourcePath(candidateNonMatches)] = prefixToExampleValues(candidateNonMatches);
    return examplesValues;
};

const mockPrefixes = {
    rdf: "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    foaf: "http://xmlns.com/foaf/0.1/",
    customPrefix: "http://customPrefix.cc/",
};

const props = {
    ruleId: "root",
    targetClassUris: [],
    onAskDiscardChanges: jest.fn(),
    onClose: jest.fn(),
    selectedVocabs: [],
    setSelectedVocabs: jest.fn(),
};

const getWrapper = (args = props): ReactWrapper<any, any> => {
    let history = createBrowserHistory();
    return withMount(
        testWrapper(<SuggestionContainer {...args} />, history, {
            common: { initialSettings: { dmBaseUrl: "http://docker.local" } },
        })
    );
};

jest.mock("../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/store", () => {
    const getSuggestionsAsyncMock = (data: any) => {
        return {
            subscribe: (subscribeFn: (any) => void) => {
                subscribeFn({
                    suggestions: mockSuggestions(data.matchFromDataset),
                    warnings: [],
                });
            },
        };
    };
    const fetchExampleValuesMock = (subscribeFn: (any) => void) => subscribeFn(mockExampleValues());
    const fetchPrefixes = (subscribeFn: (any) => void) => subscribeFn(mockPrefixes);
    const apiDetails = () => ({ baseUrl: "" });
    return {
        __esModule: true,
        getSuggestionsAsync: jest.fn().mockImplementation(getSuggestionsAsyncMock),
        schemaExampleValuesAsync: jest
            .fn()
            .mockImplementation(jest.fn().mockReturnValue({ subscribe: fetchExampleValuesMock })),
        prefixesAsync: jest.fn().mockImplementation(jest.fn().mockReturnValue({ subscribe: fetchPrefixes })),
        generateRuleAsync: jest.fn().mockImplementation(jest.fn().mockReturnValue({ subscribe: jest.fn() })),
        getApiDetails: jest.fn().mockImplementation(apiDetails),
        useApiDetails: jest.fn().mockImplementation(apiDetails),
    };
});

jest.mock("../../../../src/app/views/pages/MappingEditor/api/silkRestApi", () => {
    const mockVocabularyInfos = () => ({
        then: (thenFn) => {
            thenFn({ data: { vocabularies: ["dummyVocab"] } });
            return { catch: () => {} };
        },
    });
    return {
        ...jest.requireActual("../../../../src/app/views/pages/MappingEditor/api/silkRestApi"),
        retrieveTransformVocabularyInfos: jest.fn().mockImplementation(mockVocabularyInfos),
        initFrontendInfo: jest.fn().mockImplementation(mockVocabularyInfos), // No valid value, but doesn't matter.
    };
});

const {
    getSuggestionsAsync,
    schemaExampleValuesAsync,
    prefixesAsync,
    generateRuleAsync,
} = require("../../../../src/app/views/pages/MappingEditor/HierarchicalMapping/store");

describe("Suggestion Container Component", () => {
    afterEach(() => {
        jest.resetModules();
    });

    it("should request suggestions, examples and prefixes on mount", async () => {
        getWrapper();
        expect(getSuggestionsAsync).toBeCalledWith(
            {
                targetClassUris: props.targetClassUris,
                ruleId: props.ruleId,
                matchFromDataset: true,
                nrCandidates: 20,
            },
            true
        );
        expect(schemaExampleValuesAsync).toBeCalled();
        expect(prefixesAsync).toBeCalled();
    });

    it("should load suggestion with reverted `matchFromDataset` value on swap action", async () => {
        const wrapper = getWrapper();
        const btn = await waitFor(() => findSingleElement(wrapper, byTestId("SWAP_BUTTON")));
        clickWrapperElement(btn);
        expect(getSuggestionsAsync).toBeCalledWith(
            {
                targetClassUris: props.targetClassUris,
                ruleId: props.ruleId,
                matchFromDataset: false,
                nrCandidates: 20,
            },
            true
        );
    });

    //FIXME: This test does not work, presumably because the add button is disabled, if no mappings are selected
    it.skip("should add action works without selected items", async () => {
        const wrapper = getWrapper();
        await waitFor(
            () => {
                expect(findAll(wrapper, "table tbody tr")).toHaveLength(3);
            },
            { onTimeout: logWrapperHtmlOnError(wrapper) }
        );
        clickElement(wrapper, byTestId("add_button"));
        expect(generateRuleAsync).toBeCalledWith([], props.ruleId, undefined);
    });
});
