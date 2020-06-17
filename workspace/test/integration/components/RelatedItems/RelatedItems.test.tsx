import React from "react";
import "@testing-library/jest-dom";
import { createBrowserHistory } from "history";
import mockAxios from "../../../__mocks__/axios";
import { eventually, findAll, findSingleElement, mockedAxiosResponse, testWrapper } from "../../TestHelper";
import { RelatedItems } from "../../../../src/app/views/shared/RelatedItems/RelatedItems";
import { RelatedItemsTestHelper } from "./RelatedItemsTestHelper";
import { SERVE_PATH } from "../../../../src/app/constants/path";
import { ReactWrapper } from "enzyme";

describe("Related items", () => {
    let hostPath = process.env.HOST;
    afterEach(() => {
        mockAxios.reset();
    });

    it("should display related items according to the project ID and task ID from the URL", async () => {
        const nrItems = 11;
        let wrapper = loadRelatedItems();
        await checkRelatedItems(nrItems, wrapper);
    });

    it("should display related items according to the project ID and task ID from the props", async () => {
        const nrItems = 11;
        let wrapper = loadRelatedItems({ projectId: PROJECT_ID, taskId: TASK_ID }, `${SERVE_PATH}`);
        await checkRelatedItems(nrItems, wrapper);
    });

    const PROJECT_ID = "cmem";
    const TASK_ID = "someTask";
    const ITEM_PREFIX = "item";
    const DEFAULT_PAGE_SIZE = 5;

    const loadRelatedItems = (
        props: { projectId?: string; taskId?: string } = {},
        currentUrl: string = `${SERVE_PATH}/projects/${PROJECT_ID}/task/${TASK_ID}`
    ) => {
        const history = createBrowserHistory();
        history.location.pathname = currentUrl;

        return testWrapper(<RelatedItems {...props} />, history);
    };

    /** Check the initial representation of the related items component. */
    const checkRelatedItems = async function (nrItems: number, wrapper: ReactWrapper<any, any, React.Component>) {
        mockAxios.mockResponseFor(
            { url: hostPath + `/api/workspace/projects/${PROJECT_ID}/tasks/${TASK_ID}/relatedItems` },
            mockedAxiosResponse({ data: RelatedItemsTestHelper.generateRelatedItemsJson(nrItems, ITEM_PREFIX) })
        );

        // Wait for render
        await eventually(() => {
            expect(wrapper.text()).toContain(`(${nrItems})`);
        });
        // Check items that are displayed in the list
        const shownRelatedItems = findAll(wrapper, "li .ecc-overviewitem__item");
        expect(shownRelatedItems).toHaveLength(DEFAULT_PAGE_SIZE);
        shownRelatedItems.forEach((elem, idx) => {
            expect(findSingleElement(elem, ".ecc-link").text()).toBe(`${ITEM_PREFIX + idx} label`);
            expect(findSingleElement(elem, ".ecc-tag__item").text()).toBe("Item");
            // Check item actions
            const itemActions = findSingleElement(elem, ".ecc-overviewitem__actions").children();
            expect(itemActions).toHaveLength(2);
            // Check detail page link
            const detailPageLink = findSingleElement(itemActions.at(0), "a").get(0);
            expect(detailPageLink.props.href).toBe("/workspace-beta/projects/cmem/task/item" + idx);
            // Check item links
            // TODO: How? Menu overlay is not part of the component.
        });
    };
});
