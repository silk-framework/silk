import React from "react";
import { History } from "history";
import { mount } from "enzyme";
import { Provider } from "react-redux";
import { AppLayout } from "../../src/app/views/layout/AppLayout/AppLayout";
import { Workspace } from "../../src/app/views/pages/Workspace/Workspace";
import { configureStore } from "@reduxjs/toolkit";
import rootReducer from "../../src/app/store/reducers";
import { ConnectedRouter } from "connected-react-router";
import { AxiosMockType } from "jest-mock-axios/dist/lib/mock-axios-types";

/** Creates the Redux store. */
export const createStore = (history: History<{}>) =>
    configureStore({
        reducer: rootReducer(history),
    });

/** Returns a wrapper for the application. */
export const testWrapper = (props: any, history: History<{}>) => {
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

export const logRequests = (mockAxios: AxiosMockType) => {
    mockAxios.queue().forEach((request) => {
        console.log(request);
    });
};
