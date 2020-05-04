import React from "react";
import "@testing-library/jest-dom";
import { Workspace } from "../../../src/app/views/pages/Workspace/Workspace";
import { Provider } from "react-redux";
import { configureStore } from "@reduxjs/toolkit";
import rootReducer from "../../../src/app/store/reducers";
import { ConnectedRouter } from "connected-react-router";
import qs from "qs";
import { createBrowserHistory } from "history";
import { waitFor } from "@testing-library/dom";
import { render } from "@testing-library/react";
import mockAxios from "../../__mocks__/axios";

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
    {
        arrayFormat: "comma",
    }
);

let history = createBrowserHistory();

history.location.pathname = "/dataintegration/workspaceNew";
history.location.search = filteredQueryParams;

const store = configureStore({
    reducer: rootReducer(history),
});

const getWrapper = (props = {}) => {
    return render(
        <Provider store={store}>
            <ConnectedRouter history={history}>
                <Workspace {...props} />
            </ConnectedRouter>
        </Provider>
    );
};

describe("when query params provided", () => {
    let wrapper = beforeEach(() => {
        wrapper = getWrapper();
    });

    afterEach(() => {
        mockAxios.reset();
    });

    it("should setup the filters from address bar", async () => {
        // mockAxios.mockResponseFor({
        //     url: 'undefinedundefined/workspace/searchItems'
        // }, {
        //     data: {
        //         results: [{
        //             label: 'FOR_TEST',
        //             itemLinks: []
        //         }],
        //         facets: [],
        //         sortByProperties: [],
        //         total: 1
        //     },
        //     status: 200,
        //     statusText: 'OK',
        //     headers: {},
        //     config: {},
        // });

        const reqInfo = mockAxios.getReqMatching("/searchItems");
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

        // await waitFor(() => {
        //     expect(wrapper.getByText('FOR_TEST')).toHaveLength(1);
        // });
    });
});
