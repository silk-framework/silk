import React from "react";
import { createMemoryHistory } from "history";
import "@testing-library/jest-dom";
import MappingCreatorBanner from "../MappingCreatorBanner";
import { byTestId, clickFoundElement, renderWrapper } from "../../../../../../test/integration/TestHelper";
import { SERVE_PATH } from "../../../../constants/path";

const projectId = "proj1";
const taskId = "task1";
const dismissedStorageKey = "di:mappingCreatorV2:bannerDismissed";
const tryItButton = byTestId("mapping-creator-banner-tryit-btn");
const dismissButton = byTestId("mapping-creator-banner-dismiss-btn");

const getWrapper = (mappingCreatorEnabled: boolean = true) => {
    const history = createMemoryHistory();
    const wrapper = renderWrapper(<MappingCreatorBanner projectId={projectId} taskId={taskId} />, history, {
        common: { initialSettings: { mappingCreatorEnabled } },
    });
    return { wrapper, history };
};

describe("MappingCreatorBanner", () => {
    beforeEach(() => {
        localStorage.clear();
    });

    it("does not render when the mappingCreatorEnabled flag is off", () => {
        const { wrapper } = getWrapper(false);
        expect(wrapper.container).toBeEmptyDOMElement();
    });

    it("renders the announcement when the mappingCreatorEnabled flag is on", () => {
        const { wrapper } = getWrapper(true);
        expect(wrapper.container).not.toBeEmptyDOMElement();
        expect(wrapper.container.querySelector(tryItButton)).toBeInTheDocument();
        expect(wrapper.container.querySelector(dismissButton)).toBeInTheDocument();
    });

    it("does not render when it has already been dismissed before", () => {
        localStorage.setItem(dismissedStorageKey, "true");
        const { wrapper } = getWrapper(true);
        expect(wrapper.container).toBeEmptyDOMElement();
    });

    it("hides itself and remembers the dismissal in local storage when dismissed", () => {
        const { wrapper } = getWrapper(true);
        expect(localStorage.getItem(dismissedStorageKey)).not.toBe("true");

        clickFoundElement(wrapper, dismissButton);

        expect(wrapper.container).toBeEmptyDOMElement();
        expect(localStorage.getItem(dismissedStorageKey)).toBe("true");
    });

    it("navigates to the embedded mapping creator tab when the try-it button is clicked", () => {
        const { wrapper, history } = getWrapper(true);

        clickFoundElement(wrapper, tryItButton);

        // The banner deep-links to the task page's tab bookmark URL, keeping the
        // workbench chrome — not the chrome-less /item/…/view/ embedding route.
        expect(history.location.pathname).toBe(
            `${SERVE_PATH}/projects/${projectId}/transform/${taskId}/di:mappingCreatorV2`,
        );
    });
});
