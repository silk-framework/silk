import React from "react";
import { render, RenderResult } from "@testing-library/react";
import { ClassNames } from "@eccenca/gui-elements";

/** Similar to Partial, but applies recursively. */
export type RecursivePartial<T> = {
    [P in keyof T]?: T[P] extends (infer U)[]
        ? RecursivePartial<U>[]
        : T[P] extends object
          ? RecursivePartial<T[P]>
          : T[P];
};

export const withRender = (component, rerender?: Function) => (rerender ? rerender(component) : render(component));

/** Click the element represented by the given wrapper.
 *
 * @param wrapper The element to click on.
 * @param times How many times to click.
 */

/** Prints the complete page HTML string to console. */
export const logPageHtml = (): void => {
    process.stdout.write(window.document.documentElement.outerHTML);
};

/** Returns a function that logs the page HTML and returns the error. */
export const logPageOnError = (err: Error) => {
    console.log(logPageHtml());
    return err;
};

export const logWrapperHtml = (wrapper: RenderResult) => {
    expect(wrapper).toBeInTheDocument();
    console.log(wrapper.container.innerHTML);
};

// Returns an array with values 0 ... (nrItems - 1)
export const rangeArray = (nrItems: number): number[] => {
    const indexes = Array(nrItems).keys();
    // @ts-ignore
    return [...indexes];
};

/** Logs the wrapper HTML on error. */
export const logWrapperHtmlOnError = (wrapper: RenderResult) => {
    expect(wrapper.baseElement).toBeInTheDocument();
    return (err: Error) => {
        logWrapperHtml(wrapper);
        return err;
    };
};

export const bluePrintClassPrefix = ClassNames.Blueprint.getClassNamespace();
