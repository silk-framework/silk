import React from "react";
import { createBrowserHistory, createMemoryHistory, History, LocationState } from "history";
import { Provider } from "react-redux";
import { configureStore, getDefaultMiddleware } from "@reduxjs/toolkit";
import rootReducer from "../../src/app/store/reducers";
import { ConnectedRouter, routerMiddleware } from "connected-react-router";
import { AxiosMockQueueItem, AxiosMockRequestCriteria, AxiosMockType, HttpResponse } from "jest-mock-axios";
import mockAxios from "../__mocks__/axios";
import { CONTEXT_PATH, SERVE_PATH } from "../../src/app/constants/path";
import { mergeDeepRight } from "ramda";
import { IStore } from "../../src/app/store/typings/IStore";
import { render, RenderResult, waitFor, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";

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

export const withRender = (component) => render(component);

export const renderWrapper = (
    ui: JSX.Element,
    history: History<LocationState> = createBrowserHistory<LocationState>(),
    initialState: RecursivePartial<IStore> = {},
    options = {},
): RenderResult => {
    const store = createStore(history, initialState);
    mockValues.history = history;
    return render(ui, {
        wrapper: ({ children }) => (
            <Provider store={store}>
                <ConnectedRouter history={history}>{children}</ConnectedRouter>
            </Provider>
        ),
        ...options,
    });
};

/** Returns a wrapper for the application. */
export const testWrapper = (
    component: React.ReactNode,
    history: History<LocationState> = createBrowserHistory<LocationState>(),
    initialState: RecursivePartial<IStore> = {},
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

/**Clicks an element specified by a selector. */
export const clickFoundElement = (root: RenderResult | Element, selector: string) => {
    const container = "container" in root ? root.container : root;
    const element = container.querySelector(selector);
    if (!element) {
        throw new Error(`No element found with selector: ${selector}`);
    }
    fireEvent.click(element);
};

/** Click the element represented by the given wrapper.
 * @param wrapper The element to click on.
 * @param times How many times to click.
 */
export const clickRenderedElement = (root: Element | RenderResult, times: number = 1) => {
    const container = "container" in root ? root.container : root;
    expect(container).toBeTruthy();
    for (let i = 0; i < times; i++) {
        fireEvent.click(container);
    }
};

export const clickMultipleRenderedElements = (roots: Element[] | RenderResult[]) => {
    for (let root of roots) {
        clickRenderedElement(root);
    }
};

/** Simulates a key down event on the element. */
export const pressKeyDown = async (element: HTMLElement, key: string = "Enter") => {
    fireEvent.keyDown(element, { key });
};

export const changeInputValue = (input: HTMLInputElement, value: string) => {
    return fireEvent.change(input, { target: { value } });
};

/** Finds a single element corresponding to the selector or fails. */
export const findElement = (root: RenderResult | Element, cssSelector: string) => {
    const container = "container" in root ? root.container : root;
    expect(container).toBeInTheDocument();
    const elements = container.querySelectorAll(cssSelector);
    expect(elements).toHaveLength(1);
    return elements[0] as HTMLElement;
};

export const elementHtmlToContain = async (wrapper: RenderResult | Element, selector: string, substring: string) => {
    await waitFor(() => expect(findElement(wrapper, selector).innerHTML).toContain(substring), {
        onTimeout: (err) => {
            console.warn(
                `Element with selector '${selector}' did not contain sub-string '${substring}'! Printing wrapper HTML:`,
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
export const byTestId = (testId: string) => `[data-test-id="${testId}"]`;

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
export const logWrapperHtml = (root: RenderResult | Element) => {
    const container = "container" in root ? root.container : root;
    console.log(container.innerHTML);
};

/** Returns a name selector. */
export const byName = (name: string): string => `[name="${name}"]`;

export const findAllDOMElements = (root: RenderResult | Element, cssSelector: string): Element[] => {
    const container = "container" in root ? root.container : root;
    expect(container).toBeInTheDocument();
    const elements = Array.from(container.querySelectorAll(cssSelector));
    return elements;
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
    criteria: string | ExtendedAxiosMockRequestCriteria,
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
                        "More than 1 request found for request criteria. Returning last match. Criteria: " + criteria,
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
    silentMode?: boolean,
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
    partialEquality: boolean = false,
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
            throw new Error(errorMessage);
        }
    };

    click = (cssSelector: string, idx: number = 0) => {
        const element = this.findAll(cssSelector)[idx] as HTMLButtonElement;
        this.assert(
            element,
            `No element with selector '${cssSelector}' ${idx !== 0 ? `at index ${idx} ` : ""}has been found!`,
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
