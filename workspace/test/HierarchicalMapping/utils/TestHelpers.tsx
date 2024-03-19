import React from "react";
import { EnzymePropSelector, mount, ReactWrapper } from "enzyme";
import { render } from "@testing-library/react";
import { ClassNames } from "@eccenca/gui-elements";

/** Similar to Partial, but applies recursively. */
export type RecursivePartial<T> = {
    [P in keyof T]?: T[P] extends (infer U)[]
        ? RecursivePartial<U>[]
        : T[P] extends object
        ? RecursivePartial<T[P]>
        : T[P];
};

export const withMount = (component) => mount(component);

export const withRender = (component, rerender?: Function) => (rerender ? rerender(component) : render(component));

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

/** Returns a data test id selector. */
export const byTestId = (testId: string): EnzymePropSelector => {
    return { "data-test-id": testId };
};

/** Prints the complete page HTML string to console. */
export const logPageHtml = (): void => {
    process.stdout.write(window.document.documentElement.outerHTML);
};

/** Returns a function that logs the page HTML and returns the error. */
export const logPageOnError = (err: Error) => {
    console.log(logPageHtml());
    return err;
};

export const logWrapperHtml = (wrapper: ReactWrapper<any, any>) => {
    wrapper.update();
    console.log(wrapper.html());
};

// Returns an array with values 0 ... (nrItems - 1)
export const rangeArray = (nrItems: number): number[] => {
    const indexes = Array(nrItems).keys();
    // @ts-ignore
    return [...indexes];
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

export const bluePrintClassPrefix = ClassNames.Blueprint.getClassNamespace();
