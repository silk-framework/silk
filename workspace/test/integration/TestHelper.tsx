import React from "react";
import { History } from "history";
import { mount, ReactWrapper } from "enzyme";
import { Provider } from "react-redux";
import { AppLayout } from "../../src/app/views/layout/AppLayout/AppLayout";
import { configureStore } from "@reduxjs/toolkit";
import rootReducer from "../../src/app/store/reducers";
import { ConnectedRouter } from "connected-react-router";
import { AxiosMockType } from "jest-mock-axios/dist/lib/mock-axios-types";

const mockValues = {
    pathName: "/what?",
};

// Mock global history object
jest.mock("../../src/app/store/configureStore", () => {
    return {
        getHistory: jest.fn().mockImplementation(() => {
            return {
                location: {
                    pathname: mockValues.pathName,
                },
            };
        }),
    };
});

/** Creates the Redux store. */
export const createStore = (history: History<{}>) =>
    configureStore({
        reducer: rootReducer(history),
    });

/** Returns a wrapper for the application. */
export const testWrapper = (component: JSX.Element, history: History<{}>) => {
    const store = createStore(history);
    // Set path name of global mock
    mockValues.pathName = history?.location?.pathname;

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
    const element = findSingleElement(wrapper, cssSelector);
    // There should only be one element, if there are more, the selector needs to be more selective
    expect(element).toHaveLength(1);
    element.simulate("click");
    // console.log(`Clicked element with selector '${cssSelector}'.`);
};

/** Finds a single element corresponding to the selector or fails. */
export const findSingleElement = (wrapper: ReactWrapper<any, any>, cssSelector: string) => {
    const element = wrapper.find(cssSelector);
    if (element.length === 3) {
        // Enzyme's find() method returns not always just the DOM elements, but also companion objects for each DOM element.
        // Filter out these companion objects and see if 1 element is left and return it.
        let validElementIdx = -1;
        const nrValidElements = element.getElements().filter((elem) => typeof elem.type === "string").length;
        expect(nrValidElements).toBe(1);
        element.getElements().forEach((elem, idx) => {
            // The companion objects have a function as type value
            if (typeof elem.type === "string") {
                validElementIdx = idx;
            }
        });
        expect(validElementIdx).toBeGreaterThan(-1);
        return element.at(validElementIdx);
    } else {
        expect(element).toHaveLength(1);
        return element;
    }
};

/** Convenience method to create axios mock responses */
export const mockAxiosResponse = (status: number = 200, data: any = "") => {
    return {
        status: status,
        data: data,
    };
};

/** Checks if a predicate becomes true before the specified timeout.
 * The predicate is true if it does not throw an exception and if it's returning a boolean it evaluates to true. */
export const eventually = async (predicate: () => any, timeoutMs: number = 2000): Promise<void> => {
    expect(timeoutMs).toBeGreaterThan(0);
    // Needed to complete the stacktrace to show from where eventually has been called.
    const tempError = Error();
    const start = Date.now();
    let count = 0;
    const checkPredicate = (resolve, reject): void => {
        let success = false;
        let error = null;
        try {
            count += 1;
            const result = predicate();
            if (typeof result === "boolean") {
                success = result;
            } else {
                success = true;
            }
        } catch (ex) {
            error = ex;
        }
        if (success) {
            resolve();
        } else {
            if (Date.now() - start < timeoutMs) {
                // Still time left, try again
                setTimeout(() => checkPredicate(resolve, reject), 1);
            } else {
                // Failed
                let timeoutError = new Error(
                    `The predicate did not become true in the specified timeout of ${timeoutMs}ms.`
                );
                if (error) {
                    timeoutError = new Error(
                        `Tried predicate ${count} times during ${timeoutMs}ms. Last error: ${error.message}`
                    );
                }
                timeoutError.stack += tempError.stack;
                reject(timeoutError);
            }
        }
    };
    return new Promise<void>((resolve, reject) => {
        checkPredicate(resolve, reject);
    });
};

/** Jest does not allow to set the window.location. In order to test changes on that object, we need to mock it.
 * This function mocks the window.location object and restores it afterwards. */
export const withWindowLocation = async (block: () => void, location: any = {}) => {
    const oldLocation = window.location;
    delete window.location;
    window.location = location;
    await block();
    window.location = oldLocation;
};
