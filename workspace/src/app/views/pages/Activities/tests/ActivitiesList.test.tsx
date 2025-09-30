import React from "react";
import "@testing-library/jest-dom";
import { render as rtlRender } from "@testing-library/react";
import { configureStore } from "@reduxjs/toolkit";
import { Provider } from "react-redux";
import workspaceReducer from "../../../../store/ducks/workspace";
import ActivityList, { nonStartableActivitiesBlacklist } from "../ActivityList";
import testData from "./test-data";
import { bluePrintClassPrefix } from "../../../../../../test/HierarchicalMapping/utils/TestHelpers";

const activityProperties: Record<
    string,
    {
        cacheActivity: boolean;
        blacklisted: boolean;
    }
> = testData.activities.reduce((cachedActivities: {}, activity) => {
    const label = activity.label.replace(/\s/g, "");
    cachedActivities[label] = {
        cacheActivity: activity.isCacheActivity,
        blacklisted: nonStartableActivitiesBlacklist[activity.id] ?? false,
    };
    return cachedActivities;
}, {});

const dummyState = {
    workspace: {
        preview: {
            searchResults: [...testData.activities],
        },
        filters: {
            appliedFilters: {
                textQuery: "",
            },
            facets: [...testData.facets],
            sorters: {},
            appliedFacets: [],
            pagination: {
                total: 100,
                limit: 25,
                current: 25,
                offset: 0,
            },
        },
        widgets: {},
    },
};

const render = (
    ui: React.JSX.Element,
    {
        store = configureStore({
            reducer: { workspace: workspaceReducer },
            preloadedState: {
                ...(dummyState as any),
            },
        }),
    } = {},
) => {
    function Wrapper({ children }) {
        return <Provider store={store}>{children}</Provider>;
    }
    return rtlRender(ui, { wrapper: Wrapper });
};

describe("ActivityList", () => {
    test("that ActivityList renders successfully", () => {
        const wrapper = render(<ActivityList />);
        expect(wrapper.container).not.toBeEmptyDOMElement();
    });

    test("that ActivityList has right number of activity control items", () => {
        const wrapper = render(<ActivityList />);
        expect(wrapper.container.querySelectorAll(`.${bluePrintClassPrefix}-card`).length).toBe(
            testData.activities.length,
        );
    });

    test("that reload icon button only for cache activities", () => {
        const wrapper = render(<ActivityList />);
        const activities = wrapper.container.querySelectorAll(`.${bluePrintClassPrefix}-card`);

        for (let activity of activities) {
            const label = activity
                .querySelector(".eccgui-typography__overflowtext.eccgui-typography__overflowtext--inline")
                ?.textContent?.replace(/of.*|\s+/g, "");

            //assumption is no operation can be performed at this point to give an error
            // where the error button will now be the first
            const firstButton = activity.querySelector(".eccgui-button.eccgui-button--icon");
            const dataTestId = firstButton?.getAttribute("data-test-id");
            expect(dataTestId).toBe(
                activityProperties[label!].blacklisted
                    ? "activity-stop-activity"
                    : activityProperties[label!].cacheActivity
                      ? "activity-reload-activity"
                      : "activity-start-activity",
            );
        }
    });

    test("that correct tags are displayed", () => {
        const wrapper = render(<ActivityList />);
        const activities = wrapper.container.querySelectorAll(`.${bluePrintClassPrefix}-card`);
        activities.forEach((activity, index) => {
            const [parentTypeTag, projectTag] = Array.from(
                activity.querySelectorAll(`.${bluePrintClassPrefix}-tag`),
            ).reverse();
            const activityData = testData.activities[index];
            expect(parentTypeTag?.textContent).toBe(
                activityData.parentType[0].toUpperCase() + activityData.parentType.substr(1),
            );
            //no project tag possibly global activity
            expect(projectTag?.textContent).toBe(!projectTag ? undefined : activityData.project);
        });
    });

    test("that activities have the right label of task or of a project", () => {
        const wrapper = render(<ActivityList />);
        const activities = wrapper.container.querySelectorAll(`.${bluePrintClassPrefix}-card`);

        activities.forEach((activity, index) => {
            const activityObj = testData.activities[index];
            const suffix = activityObj.task ? activityObj.taskLabel : activityObj.projectLabel;
            const activityLabelSuffix = activity
                .querySelector(".eccgui-typography__overflowtext.eccgui-typography__overflowtext--inline")
                ?.textContent?.replace(/.*of\s?/, "");
            expect(suffix).toBe(activityObj.parentType === "global" ? undefined : activityLabelSuffix);
        });
    });
});
