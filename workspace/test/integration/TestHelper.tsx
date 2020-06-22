import React from "react";
import { History } from "history";
import { EnzymePropSelector, mount, ReactWrapper } from "enzyme";
import { Provider } from "react-redux";
import { AppLayout } from "../../src/app/views/layout/AppLayout/AppLayout";
import { configureStore } from "@reduxjs/toolkit";
import rootReducer from "../../src/app/store/reducers";
import { ConnectedRouter } from "connected-react-router";
import { AxiosMockType } from "jest-mock-axios/dist/lib/mock-axios-types";
import mockAxios from "../__mocks__/axios";
import { CONTEXT_PATH, SERVE_PATH } from "../../src/app/constants/path";

const mockValues = {
    pathName: "/what?",
    useParams: {
        projectId: "Set me via TestHelper.setUseParams!",
        taskId: "Set me via TestHelper.setUseParams!",
    },
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

// Mock useParams hook
jest.mock("react-router", () => ({
    ...jest.requireActual("react-router"), // use actual for all non-hook parts
    useParams: () => ({
        projectId: mockValues.useParams.projectId,
        taskId: mockValues.useParams.taskId,
    }),
}));

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

/** Sets what should be returned from the useParams hook. */
export const setUseParams = (projectId: string, taskId: string): void => {
    mockValues.useParams = {
        projectId: projectId,
        taskId: taskId,
    };
};

/** Logs all requests to the console. */
export const logRequests = (axiosMock?: AxiosMockType) => {
    const mock = axiosMock ? axiosMock : mockAxios;
    mock.queue().forEach((request) => {
        console.log(request);
    });
};

/** Clicks an element specified by a selector. */
export const clickElement = (wrapper: ReactWrapper<any, any>, cssSelector: string | EnzymePropSelector) => {
    const element = findSingleElement(wrapper, cssSelector);
    clickWrapperElement(element);
    // console.log(`Clicked element with selector '${cssSelector}'.`);
};

/** Click the element represented by the given wrapper. */
export const clickWrapperElement = (wrapper: ReactWrapper<any, any>) => {
    // There should only be one element, if there are more, the selector needs to be more selective
    expect(wrapper).toHaveLength(1);
    wrapper.simulate("click");
};

/** Simulates a keyboard key press on an element. */
export const pressKey = (wrapper: ReactWrapper<any, any>, key: string = "Enter") => {
    wrapper.simulate("keypress", { key: key });
};

/** Simulates a key down event on the element. */
export const keyDown = (wrapper: ReactWrapper<any, any>, key: string = "Enter") => {
    wrapper.simulate("keydown", { key: key });
};

/** Triggers a change event on an element. */
export const changeValue = (wrapper: ReactWrapper<any, any>, value: string) => {
    wrapper.simulate("change", { target: { value: value } });
};

/** Finds a single element corresponding to the selector or fails. */
export const findSingleElement = (
    wrapper: ReactWrapper<any, any>,
    cssSelector: string | EnzymePropSelector
): ReactWrapper<any, any> => {
    wrapper.update();
    const element = findAll(wrapper, cssSelector);
    expect(element).toHaveLength(1);
    return element[0];
};

/** Returns a data test id selector. */
export const byTestId = (testId: string): EnzymePropSelector => {
    return { "data-test-id": testId };
};

/** Enzyme's find() method returns not always just the DOM elements, but also companion objects for each DOM element.
 * Filter out these companion objects and see if 1 element is left and return it. */
const extractValidElements = function (element: ReactWrapper<any, any>) {
    const validElementIdx: number[] = [];
    element.getElements().forEach((elem, idx) => {
        if (typeof elem.type === "string") {
            validElementIdx.push(idx);
        }
    });
    return validElementIdx.map((idx) => element.at(idx));
};
/** Finds all wrapper elements that are actual elements in the DOM */
export const findAll = (wrapper: ReactWrapper<any, any>, cssSelector: string | EnzymePropSelector): ReactWrapper[] => {
    wrapper.parent();
    wrapper.update();
    const element =
        typeof cssSelector === "string"
            ? wrapper.find(cssSelector as string)
            : wrapper.find(cssSelector as EnzymePropSelector);
    return extractValidElements(element);
};

interface IAxiosResponse {
    status?: number;
    data?: any;
}

/** Convenience method to create axios mock responses */
export const mockedAxiosResponse = ({ status = 200, data = "" }: IAxiosResponse = {}) => {
    return {
        status: status,
        data: data,
    };
};

// Returns an array with values 0 ... (nrItems - 1)
export const rangeArray = (nrItems: number): number[] => {
    const indexes = Array(nrItems).keys();
    return [...indexes];
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

/** Returns the absolute URL under the workspace path with the given path value appended. */
export const workspaceUrl = (path: string): string => {
    const hostPath = process.env.HOST;
    return hostPath + workspacePath(path);
};

/** Returns the absolute path under the workspace path with the given path value appended. */
export const workspacePath = (path: string): string => {
    return SERVE_PATH + prependSlash(path);
};

const host = process.env.HOST;

/** Absolute URL of the legacy API. Basically all over the place. ;) */
export const legacyApiUrl = (path: string): string => {
    return host + CONTEXT_PATH + prependSlash(path);
};

// Prepend a "/" in front of the path if it is missing.
const prependSlash = function (path: string) {
    if (!path.startsWith("/")) {
        return "/" + path;
    } else {
        return path;
    }
};

/** Returns the absolute URL under the api path with the given path value appended. */
export const apiUrl = (path: string): string => {
    return host + CONTEXT_PATH + "/api" + prependSlash(path);
};

/** Checks if a request to a specific URL was made.
 *
 * @param url             The URL the request was made to.
 * @param method          The HTTP method of the request.
 * @param data            The expected data of the request. Either the request body or query parameters.
 * @param partialEquality Should the request data only be checked partially, i.e. only the values that are actually given in the parameter?
 */
export const checkRequestMade = (
    url: string,
    method: string = "GET",
    data: any = null,
    partialEquality: boolean = false
): void => {
    const reqInfo = mockAxios.getReqMatching({
        url: url,
    });
    if (!reqInfo) {
        throw new Error(`No request was made to URL ${url} with method '${method}'.`);
    }
    if (data !== null) {
        if (partialEquality && typeof data === "object") {
            Object.entries(data).forEach(([key, value]) => {
                expect(reqInfo.data[key]).toStrictEqual(value);
            });
        } else {
            expect(reqInfo.data).toStrictEqual(data);
        }
    }
};
