import { RenderResult } from "@testing-library/react";
import { clickRenderedElement, findAllDOMElements } from "../../TestHelper";

/** Clicks the "next" button of a paging element.
 *
 * @param wrapper The element the paging component is contained in.
 */
export const clickNextPageButton = (wrapper: RenderResult) => {
    const navButtons = findAllDOMElements(wrapper, ".cds--pagination__right button");
    expect(navButtons).toHaveLength(2);
    clickRenderedElement(navButtons[1]);
};
