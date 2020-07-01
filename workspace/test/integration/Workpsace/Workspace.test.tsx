import React from "react";
import qs from "qs";
import { createBrowserHistory, createMemoryHistory } from "history";
import mockAxios from "../../__mocks__/axios";
import { mockedAxiosResponse, testWrapper, withRender, workspacePath } from "../TestHelper";
import { Workspace } from "../../../src/app/views/pages/Workspace/Workspace";
import { waitFor, fireEvent, screen } from "@testing-library/react";

describe("Search Items", () => {
    let hostPath = process.env.HOST;
    let history = null;

    const mockItemTypesRequest = () => {
        mockAxios.mockResponseFor(
            {
                url: hostPath + "/api/workspace/searchConfig/types",
            },
            mockedAxiosResponse({
                data: {
                    label: "Datatypes",
                    values: [{ id: "dataset", label: "dataset" }],
                },
            })
        );
    };

    const mockSearchItemsRequest = () => {
        mockAxios.mockResponseFor(
            {
                url: hostPath + "/api/workspace/searchItems",
            },
            mockedAxiosResponse({
                data: {
                    facets: [
                        {
                            id: "facetId",
                            label: "FACET_LABEL",
                            type: "keyword",
                            values: [
                                { id: "test1", label: "test1" },
                                { id: "test2", label: "test2" },
                            ],
                        },
                    ],
                    results: [],
                    sortByProperties: [],
                },
            })
        );
    };

    const getWrapper = (searchParams?: Object) => {
        history = createMemoryHistory();
        const rootPath = workspacePath("");
        history.push(rootPath);
        if (searchParams) {
            const qsStr = qs.stringify(searchParams, { arrayFormat: "comma" });
            history.push(`${rootPath}?${qsStr}`);
        }
        return withRender(testWrapper(<Workspace />, history));
    };

    afterEach(() => {
        mockAxios.reset();
    });

    it("should filter items, by given criteria from URL search params", async () => {
        const filteredQueryParams = {
            textQuery: "some text",
            itemType: "dataset",
            limit: 15,
            page: 2,
            f_ids: ["facetId1", "facetId2"],
            f_keys: ["facet1Key1|facet1Key2", "facet2Key"],
            types: ["keyword", "keyword"],
        };

        getWrapper(filteredQueryParams);

        const reqInfo = mockAxios.getReqMatching({
            url: hostPath + "/api/workspace/searchItems",
        });
        expect(reqInfo.data).toEqual({
            textQuery: "some text",
            itemType: "dataset",
            limit: 15,
            offset: 10,
            facets: [
                { facetId: "facetId1", type: "keyword", keywordIds: ["facet1Key1", "facet1Key2"] },
                { facetId: "facetId2", type: "keyword", keywordIds: ["facet2Key"] },
            ],
        });
    });

    it("should item types requested", () => {
        getWrapper();

        const reqInfo = mockAxios.getReqMatching({
            url: hostPath + "/api/workspace/searchConfig/types",
        });
        expect(reqInfo).toBeTruthy();
    });

    it("should facets presented correctly", async (done) => {
        const filteredQueryParams = {
            itemType: "dataset",
        };

        getWrapper(filteredQueryParams);

        mockItemTypesRequest();

        mockSearchItemsRequest();

        await waitFor(() => {
            const elements = screen.queryAllByTestId(`facet-items`);
            expect(elements).toHaveLength(2);
            done();
        });
    });

    xit("should search bar send request for filtering", async (done) => {
        const { rerender } = getWrapper();

        const input = screen.queryByTestId(`search-bar`);
        fireEvent.change(input, { target: { value: "test" } });
        fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

        withRender(testWrapper(<Workspace />, history), rerender);

        await waitFor(() => {
            expect(mockAxios.lastReqGet().data).toEqual({
                facets: [],
                limit: 10,
                offset: 0,
                textQuery: "test",
            });

            done();
        });
    });
});
