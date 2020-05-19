import React from "react";
import "@testing-library/jest-dom";
import { Provider } from "react-redux";
import { configureStore } from "@reduxjs/toolkit";
import rootReducer from "../../../src/app/store/reducers";
import { ConnectedRouter } from "connected-react-router";
import { createBrowserHistory } from "history";
import mockAxios from "../../__mocks__/axios";
import { mount } from "enzyme";
import { Workspace } from "../../../src/app/views/pages/Workspace/Workspace";
import { AppLayout } from "../../../src/app/views/layout/AppLayout/AppLayout";

const createStore = (history = createBrowserHistory()) =>
    configureStore({
        reducer: rootReducer(history),
    });

const getWrapper = (props = {}, h) => {
    let history = h;
    if (!history) {
        history = createBrowserHistory();
        history.location.pathname = "/dataintegration/workspaceNew/projects/cmem";
    }

    const store = createStore(history);

    return mount(
        <Provider store={store}>
            <ConnectedRouter history={history}>
                <AppLayout>
                    <Workspace {...props} />
                </AppLayout>
            </ConnectedRouter>
        </Provider>
    );
};

jest.mock("react-router", () => ({
    ...jest.requireActual("react-router"),
    useParams: () => ({
        projectId: "cmem",
    }),
    useRouteMatch: () => ({ url: "/dataintegration/workspaceNew/projects/cmem" }),
}));

describe("Project page", () => {
    let hostPath = process.env.HOST;
    afterEach(() => {
        mockAxios.reset();
    });

    it("should get common data types or for specific project", async () => {
        let history = createBrowserHistory();
        history.location.pathname = "/dataintegration/workspaceNew/projects/cmem";

        getWrapper({}, history);

        const reqInfo = mockAxios.getReqMatching({
            url: hostPath + "/api/workspace/searchConfig/types?projectId=cmem",
        });

        expect(reqInfo).toBeTruthy();
    });

    it("should request meta data", async () => {
        let history = createBrowserHistory();
        history.location.pathname = "/dataintegration/workspaceNew/projects/cmem";

        getWrapper({}, history);

        const reqInfo = mockAxios.getReqMatching({
            url: hostPath + "/api/workspace/projects/cmem/metaData",
        });

        expect(reqInfo).toBeTruthy();
    });

    xit("should send the right projectId to backend", async () => {
        let history = createBrowserHistory();
        history.location.pathname = "/dataintegration/workspaceNew/projects/cmem";

        getWrapper({}, history);

        const reqInfo = mockAxios.getReqMatching({
            url: hostPath + "/api/workspace/searchItems",
        });

        expect(reqInfo.data).toEqual({
            limit: 10,
            offset: 0,
            project: "cmem",
            textQuery: "",
            facets: [],
        });
    });
});
