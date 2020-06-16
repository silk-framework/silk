import React from "react";
import { History } from "history";
import { mount, ReactWrapper } from "enzyme";
import { Provider } from "react-redux";
import { AppLayout } from "../../src/app/views/layout/AppLayout/AppLayout";
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
export const testWrapper = (component: JSX.Element, history: History<{}>) => {
    const store = createStore(history);

    return mount(
        <Provider store={store}>
            <ConnectedRouter history={history}>
                <AppLayout>{component}</AppLayout>
            </ConnectedRouter>
        </Provider>
    );
};

/** Logs all requests to the console. */
export const logRequests = (mockAxios: AxiosMockType) => {
    mockAxios.queue().forEach((request) => {
        console.log(request);
    });
};

/** Clicks an element specified by a selector. */
export const clickElement = (wrapper: ReactWrapper<any, any>, cssSelector: string) => {
    const element = wrapper.find(cssSelector);
    // There should only be one element, if there are more, the selector needs to be more selective
    // For enzyme it often is not enough to specify the ID of an element, e.g. "#id", but prepend the concrete element type, e.g. "div#id".
    expect(element).toHaveLength(1);
    element.simulate("click");
    // console.log(`Clicked element with selector '${cssSelector}'.`);
};
