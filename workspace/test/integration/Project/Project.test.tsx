import React from "react";
import mockAxios from "../../__mocks__/axios";
import { apiUrl, checkRequestMade, legacyApiUrl, testWrapper, workspacePath } from "../TestHelper";
import { createBrowserHistory } from "history";
import Project from "../../../src/app/views/pages/Project";
import qs from "qs";

describe("Project page", () => {
    let hostPath = process.env.HOST;
    beforeEach(() => {
        const history = createBrowserHistory();
        history.location.pathname = workspacePath("/projects/cmem");

        return testWrapper(<Project />, history);
    });

    afterEach(() => {
        mockAxios.reset();
    });

    it("should get common data types or for specific project", async () => {
        checkRequestMade(apiUrl("/workspace/searchConfig/types?projectId=cmem"));
    });

    it("should request meta data", async () => {
        checkRequestMade(apiUrl("/workspace/projects/cmem/metaData"));
    });

    it("should get available resources for file widget", () => {
        checkRequestMade(legacyApiUrl("/workspace/projects/cmem/resources"));
    });

    it("should get prefixes for configuration widget", () => {
        checkRequestMade(apiUrl("/workspace/projects/cmem/prefixes"));
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
        history.location.pathname = workspacePath("/projects/cmem");
        history.location.search = filteredQueryParams;

        testWrapper(<Project />, history);

        const expectedSearchResponse = {
            textQuery: "some text",
            itemType: "dataset",
            limit: 15,
            offset: 10,
            project: "cmem",
            facets: [
                { facetId: "facetId1", type: "keyword", keywordIds: ["facet1Key1", "facet1Key2"] },
                { facetId: "facetId2", type: "keyword", keywordIds: ["facet2Key"] },
            ],
        };

        checkRequestMade(apiUrl("/workspace/searchItems"), "POST", expectedSearchResponse);
    });
});
