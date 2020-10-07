import React from "react";
import mockAxios from "../../__mocks__/axios";
import {
    apiUrl,
    checkRequestMade,
    legacyApiUrl,
    testWrapper,
    withMount,
    workspacePath,
    findAll,
    byTestId,
} from "../TestHelper";
import { createBrowserHistory } from "history";
import Project from "../../../src/app/views/pages/Project";
import qs from "qs";

describe("Project page", () => {
    const testProjectId = "Hierarchical_Person_Example";
    let hostPath = process.env.HOST;
    let projectPageWrapper: ReactWrapper<any, any> = null;
    beforeEach(() => {
        const history = createBrowserHistory();
        history.location.pathname = workspacePath("/projects/" + testProjectId);

        projectPageWrapper = withMount(testWrapper(<Project />, history));

        return projectPageWrapper;
    });

    afterEach(() => {
        mockAxios.reset();
    });

    it("should get common data types or for specific project", async () => {
        checkRequestMade(apiUrl("/workspace/searchConfig/types?projectId=" + testProjectId));
    });

    it("should request meta data", async () => {
        checkRequestMade(apiUrl("/workspace/projects/" + testProjectId + "/metaData"));
    });

    it("should get available resources for file widget", () => {
        checkRequestMade(legacyApiUrl("/workspace/projects/" + testProjectId + "/resources"));
    });

    it("should get prefixes for configuration widget", () => {
        checkRequestMade(apiUrl("/workspace/projects/" + testProjectId + "/prefixes"));
    });

    it("should filter items, by given criteria from URL search params", async () => {
        const filteredQueryParams = qs.stringify(
            {
                textQuery: "some text",
                itemType: "dataset",
                limit: 15,
                page: 2,
                f_ids: ["facetId1", "facetId2"],
                f_keys: ["facet1Key1|facet1Key2", "facet2Key"],
                types: ["keyword", "keyword"],
            },
            { arrayFormat: "comma" }
        );

        let history = createBrowserHistory();
        history.location.pathname = workspacePath("/projects/" + testProjectId);
        history.location.search = filteredQueryParams;

        withMount(testWrapper(<Project />, history));

        const expectedSearchResponse = {
            textQuery: "some text",
            itemType: "dataset",
            limit: 15,
            offset: 10,
            project: testProjectId,
            facets: [
                { facetId: "facetId1", type: "keyword", keywordIds: ["facet1Key1", "facet1Key2"] },
                { facetId: "facetId2", type: "keyword", keywordIds: ["facet2Key"] },
            ],
        };

        checkRequestMade(apiUrl("/workspace/searchItems"), "POST", expectedSearchResponse);
    });

    // TODO
    xit("file search bar is not shown when there are no files", () => {
        const filesearchinput = findAll(projectPageWrapper, byTestId(`file-search-bar`));
        expect(filesearchinput).toHaveLength(0);
    });

    // TODO
    xit("file search bar never disappears when no results are shown", async (done) => {
        const filesearchinput = screen.queryByTestId(`file-search-bar`);
        fireEvent.change(filesearchinput, { target: { value: "this-is-not-a-known-file-teststring" } });
        fireEvent.keyDown(filesearchinput, { key: "Enter", code: "Enter" });
        await waitFor(() => {
            const filesearchinputTest = screen.findAllByTestId(`file-search-bar`);
            expect(filesearchinputTest).toHaveLength(1);
            done();
        });
    });
});
