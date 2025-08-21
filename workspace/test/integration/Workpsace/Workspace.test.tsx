import React from "react";
import "@testing-library/jest-dom";
import qs from "qs";
import { createMemoryHistory } from "history";
import mockAxios from "../../__mocks__/axios";
import { byTestId, findAllDOMElements, mockedAxiosResponse, renderWrapper, workspacePath } from "../TestHelper";
import { Workspace } from "../../../src/app/views/pages/Workspace/Workspace";
import { fireEvent, screen, waitFor } from "@testing-library/react";

describe("Search Items", () => {
    let hostPath = process.env.HOST;
    let history = null;
    const resultData = {
        description: "123123",
        id: "ID-",
        itemLinks: [
            {
                label: "Project details page",
                path: "/workbench/projects/eb233297-9d72-4b82-b77c-b2d1ee193c29_NewProject",
            },
        ],
        label: "New Project",
        type: "project",
    };

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
            }),
        );
    };

    const mockSearchItemsRequest = () => {
        let arr = [];
        for (let i = 0; i < 20; i++) {
            arr.push({
                ...resultData,
                id: resultData.id + i,
            });
        }

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
                    results: arr,
                    sortByProperties: [
                        {
                            id: "label",
                            label: "Label",
                        },
                    ],
                    total: 20,
                },
            }),
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
        return renderWrapper(<Workspace />, history);
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

    it("should facets presented correctly", async () => {
        const filteredQueryParams = {
            itemType: "dataset",
        };

        const wrapper = getWrapper(filteredQueryParams);

        mockItemTypesRequest();

        mockSearchItemsRequest();

        await waitFor(() => {
            const elements = findAllDOMElements(wrapper, byTestId(`facet-items`));
            expect(elements).toHaveLength(2);
        });
    });

    xit("should search bar send request for filtering", async () => {
        // THE QUERY STRING NOT UPDATED IN ROUTER-CONNECTED-ROUTER
        getWrapper();

        const input = screen.queryByTestId(`search-bar`) as Element;
        fireEvent.change(input, { target: { value: "test" } });
        fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

        await waitFor(() => {
            expect(mockAxios.lastReqGet().data).toEqual({
                facets: [],
                limit: 10,
                offset: 0,
                textQuery: "test",
            });
        });
    });

    xit("should pagination works as expected", async () => {
        getWrapper();

        mockItemTypesRequest();

        mockSearchItemsRequest();

        mockAxios.reset();

        const btn = await screen.findByLabelText("Next page");
        fireEvent.click(btn);

        await waitFor(() => {
            const reqInfo = mockAxios.getReqMatching({
                url: hostPath + "/api/workspace/searchItems",
            });

            expect(reqInfo.data).toEqual({
                itemType: "dataset",
                limit: 15,
                offset: 10,
                page: 2,
                facets: [
                    { facetId: "facetId1", type: "keyword", keywordIds: ["facet1Key1", "facet1Key2"] },
                    { facetId: "facetId2", type: "keyword", keywordIds: ["facet2Key"] },
                ],
            });
        });
    });
});
