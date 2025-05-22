import React from "react";
import { createBrowserHistory, createMemoryHistory, History, LocationState } from "history";
import { EnzymePropSelector, mount, ReactWrapper, shallow } from "enzyme";
import { Provider } from "react-redux";
import { configureStore, getDefaultMiddleware } from "@reduxjs/toolkit";
import rootReducer from "../../src/app/store/reducers";
import { ConnectedRouter, routerMiddleware } from "connected-react-router";
import {
    AxiosMockQueueItem,
    AxiosMockRequestCriteria,
    AxiosMockType,
    HttpResponse,
} from "jest-mock-axios";
import mockAxios from "../__mocks__/axios";
import { CONTEXT_PATH, SERVE_PATH } from "../../src/app/constants/path";
import { mergeDeepRight } from "ramda";
import { IStore } from "../../src/app/store/typings/IStore";
import { render, RenderResult, waitFor } from "@testing-library/react";
import {
    responseInterceptorOnError,
    responseInterceptorOnSuccess,
} from "../../src/app/services/fetch/responseInterceptor";
import { AxiosError } from "axios";

interface IMockValues {
    history: History;
    useParams: Record<string, string>;
}

const mockValues: IMockValues = {
    history: createMemoryHistory(),
    useParams: {
        projectId: "Set me via TestHelper.setUseParams!",
        taskId: "Set me via TestHelper.setUseParams!",
    },
};
const host = process.env.HOST;

jest.mock("@codemirror/view", () => ({
    ...jest.requireActual("@codemirror/view"),
}));

jest.mock("@codemirror/language", () => ({
    ...jest.requireActual("@codemirror/language"),
}));

// Mock global history object
jest.mock("../../src/app/store/configureStore", () => {
    return {
        getHistory: jest.fn().mockImplementation(() => {
            return mockValues.history;
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

/** Creates the Redux store.
 *
 * @param history      The initial history.
 * @param initialState
 */
export const createStore = (history: History<LocationState>, initialState: RecursivePartial<IStore>) => {
    const root = rootReducer(history);
    const middleware = [
        ...getDefaultMiddleware({
            serializableCheck: false,
        }),
        routerMiddleware(history),
    ];

    // Get the initial state (defaults) of the store
    // FIXME: Is there a better way to get the initial state of the store?
    const tempStore = configureStore({
        reducer: root,
        middleware,
    });

    const rootState = tempStore.getState();
    // Patch the state with user supplied state
    const state = mergeDeepRight(rootState, initialState) as IStore;
    // Create store with merged state
    return configureStore({
        reducer: root,
        middleware,
        preloadedState: state,
    });
};

/** Similar to Partial, but applies recursively. */
export type RecursivePartial<T> = {
    [P in keyof T]?: T[P] extends (infer U)[]
        ? RecursivePartial<U>[]
        : T[P] extends object
        ? RecursivePartial<T[P]>
        : T[P];
};

export const withShallow = (component) => shallow(component);

export const withMount = (component) => mount(component);

export const withRender = (component) => render(component);

/** Returns a wrapper for the application. */
export const testWrapper = (
    component: React.ReactNode,
    history: History<LocationState> = createBrowserHistory<LocationState>(),
    initialState: RecursivePartial<IStore> = {}
) => {
    const store = createStore(history, initialState);
    // Set path name of global mock
    mockValues.history = history;

    return (
        <Provider store={store}>
            <ConnectedRouter history={history}>{component}</ConnectedRouter>
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

/** Click the element represented by the given wrapper.
 *
 * @param wrapper The element to click on.
 * @param times How many times to click.
 */
export const clickWrapperElement = (wrapper: ReactWrapper<any, any>, times: number = 1) => {
    // There should only be one element, if there are more, the selector needs to be more selective
    expect(wrapper).toHaveLength(1);
    for (let i = 0; i < times; i++) {
        wrapper.simulate("click");
    }
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

export const elementHtmlToContain = async (
    wrapper: ReactWrapper<any, any>,
    selector: string | EnzymePropSelector,
    substring: string
) => {
    await waitFor(() => expect(findSingleElement(wrapper, selector).html()).toContain(substring), {
        onTimeout: (err) => {
            console.warn(
                `Element with selector '${selector}' did not contain sub-string '${substring}'! Printing wrapper HTML:`
            );
            logWrapperHtml(wrapper);
            return err;
        },
    });
};

/** Adds the document.createRange method */
export const addDocumentCreateRangeMethod = () => {
    (global as any).document.createRange = () => ({
        setStart: () => {},
        setEnd: () => {},
        commonAncestorContainer: {
            nodeName: "BODY",
            ownerDocument: document,
        },
    });
};

/** Returns a data test id selector. */
export const byTestId = (testId: string): EnzymePropSelector => {
    return { "data-test-id": testId };
};

/** Prints the complete page HTML string to console. */
export const logPageHtml = (): void => {
    process.stdout.write(window.document.documentElement.outerHTML);
};

/** Get the page HTML */
export const pageHtml = (): string => window.document.documentElement.outerHTML;

/** Returns a function that logs the page HTML and returns the error. */
export const logPageOnError = (err: Error) => {
    console.log(logPageHtml());
    return err;
};

/** Log the wrapper HTML to the console */
export const logWrapperHtml = (wrapper: ReactWrapper<any, any>) => {
    wrapper.update();
    console.log(wrapper.html());
};

/** Returns the HTML of th given React wrapper */
export const wrapperHtml = (wrapper: ReactWrapper<any, any>): string => {
    wrapper.update();
    return wrapper.html();
};

/** Logs the wrapper HTML on error. */
export const logWrapperHtmlOnError = (wrapper: ReactWrapper<any, any>) => {
    wrapper.update();
    return (err: Error) => {
        logWrapperHtml(wrapper);
        return err;
    };
};

/** Returns a name selector. */
export const byName = (name: string): EnzymePropSelector => {
    return { name: name };
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

/** Returns the Axios queue item based on the given criteria. */
const axiosMockItemByCriteria = (
    criteria: string | ExtendedAxiosMockRequestCriteria
): AxiosMockQueueItem | undefined => {
    if (typeof criteria === "string") {
        return mockAxios.getReqByUrl(criteria);
    } else {
        if (criteria.partialPayload) {
            const matchingQueueItems = mockAxios.queue().filter((queueItem) => {
                const methodMatch = criteria.method
                    ? criteria.method.toLowerCase() === queueItem.method.toLowerCase()
                    : true;
                const urlMatch = criteria.url ? criteria.url === queueItem.url : true;
                let dataMatch = true;
                if (criteria.partialPayload) {
                    try {
                        expect(queueItem.data).toEqual(expect.objectContaining(criteria.partialPayload));
                    } catch {
                        dataMatch = false;
                    }
                }
                return methodMatch && urlMatch && dataMatch;
            });
            switch (matchingQueueItems.length) {
                case 0:
                    return undefined;
                case 1:
                    return matchingQueueItems[0];
                default:
                    console.warn(
                        "More than 1 request found for request criteria. Returning last match. Criteria: " + criteria
                    );
                    return matchingQueueItems[matchingQueueItems.length - 1];
            }
        } else {
            return mockAxios.getReqMatching(criteria);
        }
    }
};

/** An Axios error mock that can be used with the mockAxiosResponse method. */
export const mockedAxiosError = (httpStatus?: number, errorData?: any): AxiosError => {
    return {
        name: "Mocked Axios error",
        message: "Mocked Axios error",
        config: {},
        response: {
            status: httpStatus ?? 500,
            data: errorData,
            statusText: "error status",
            headers: {},
            config: {},
        },
        isAxiosError: true,
        toJSON: () => ({}),
    };
};

/** Extends the Axios request criteria by a payload object that needs to partially match the actual payload data. */
interface ExtendedAxiosMockRequestCriteria extends AxiosMockRequestCriteria {
    // Payload that must partially match. NOTE: This is not recursive, ONLY the root object level is partially matched!
    partialPayload: any;
}

/** Mock an Axios request. Depending on the response object this is either a valid response or an error. */
export const mockAxiosResponse = (
    criteria: string | ExtendedAxiosMockRequestCriteria,
    response?: HttpResponse | AxiosError,
    silentMode?: boolean
): void => {
    mockAxios.interceptors.response.use(responseInterceptorOnSuccess, responseInterceptorOnError);
    const requestQueueItem = axiosMockItemByCriteria(criteria);
    if (requestQueueItem) {
        if (response) {
            if ((response as AxiosError).isAxiosError) {
                mockAxios.mockError(response, requestQueueItem);
            } else {
                mockAxios.mockResponseFor(criteria, response as HttpResponse, silentMode);
            }
        } else {
            mockAxios.mockResponseFor(criteria, undefined, silentMode);
        }
    } else {
        throw new Error("No request to mock for " + criteria);
    }
};

/** Jest does not allow to set the window.location. In order to test changes on that object, we need to mock it.
 * This function mocks the window.location object and restores it afterwards. */
export const withWindowLocation = async (block: () => void, location: any = {}) => {
    const oldLocation = window.location;
    //delete window.location;
    window.location = location;
    await block();
    window.location = oldLocation;
};

/** Returns the absolute path under the workspace path with the given path value appended. */
export const workspacePath = (path: string = ""): string => {
    return path ? SERVE_PATH + prependSlash(path) : SERVE_PATH;
};

/** Absolute URL of the legacy API. Basically all over the place. ;) */
export const legacyApiUrl = (path: string): string => {
    return host + CONTEXT_PATH + prependSlash(path);
};

// Prepend a "/" in front of the path if it is missing.
const prependSlash = function (path: string) {
    if (!path.startsWith("/") && !path.startsWith("?")) {
        return "/" + path;
    } else {
        return path;
    }
};

/** Returns the absolute URL under the api path with the given path value appended. */
export const apiUrl = (path: string): string => {
    return `${host}${CONTEXT_PATH}/api${prependSlash(path)}`;
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
                expect(reqInfo.data[key]).toEqual(value);
            });
        } else {
            expect(reqInfo.data).toEqual(data);
        }
    }
};

/** Cleans up the DOM. This is needed to avoid DOM elements from one test interfering with the subsequent tests. */
export const cleanUpDOM = () => (document.body.innerHTML = "");

export class RenderResultApi {
    renderResult: RenderResult;

    constructor(renderResult: RenderResult) {
        this.renderResult = renderResult;
    }

    find = (cssSelector: string): Element | null => {
        return this.renderResult.container.querySelector(cssSelector);
    };

    findExisting = (cssSelector: string): Element => {
        const element = this.find(cssSelector);
        this.assert(!!element, `Element with selector '${cssSelector}' does not exist!`);
        return element!;
    };

    findNth = (cssSelector: string, idx: number): Element => {
        const element = this.findAll(cssSelector)[idx];
        this.assert(!!element, `${idx + 1}th element with selector '${cssSelector}' does not exist!`);
        return element!;
    };

    findAll = (cssSelector: string): NodeListOf<Element> => {
        return this.renderResult.container.querySelectorAll(cssSelector);
    };

    assert = (predicate: any, errorMessage: string) => {
        if (!predicate) {
            fail(errorMessage);
        }
    };

    click = (cssSelector: string, idx: number = 0) => {
        const element = this.findAll(cssSelector)[idx] as HTMLButtonElement;
        this.assert(
            element,
            `No element with selector '${cssSelector}' ${idx !== 0 ? `at index ${idx} ` : ""}has been found!`
        );
        if (element.click) {
            element.click();
        } else {
            element.dispatchEvent(new Event("click"));
        }
    };

    printHtml = (selector?: string) => {
        const elementToPrint = selector ? this.findExisting(selector) : this.renderResult.container;
        console.log(elementToPrint.outerHTML);
    };

    static testId = (testId: string): string => {
        return `[data-test-id = '${testId}']`;
    };
}
