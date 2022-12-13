import { ReactWrapper } from "enzyme";

import { clickWrapperElement, findAll } from "../../TestHelper";

/** Clicks the "next" button of a paging element.
 *
 * @param wrapper The element the paging component is contained in.
 */
export const clickNextPageButton = (wrapper: ReactWrapper<any, any>) => {
    const navButtons = findAll(wrapper, ".cds--pagination__right button");
    expect(navButtons).toHaveLength(2);
    clickWrapperElement(navButtons[1]);
};
