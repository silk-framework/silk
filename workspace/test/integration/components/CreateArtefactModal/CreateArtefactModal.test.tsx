import React from "react";
import "@testing-library/jest-dom";
import mockAxios from "../../../__mocks__/axios";
import { ReactWrapper } from "enzyme";
import { SERVE_PATH } from "../../../../src/app/constants/path";
import { createBrowserHistory } from "history";
import {
    apiUrl,
    byTestId,
    checkRequestMade,
    clickWrapperElement,
    findAll,
    findSingleElement,
    logRequests,
    mockedAxiosResponse,
    testWrapper,
    withMount,
} from "../../TestHelper";
import { CreateArtefactModal } from "../../../../src/app/views/shared/modals/CreateArtefactModal/CreateArtefactModal";
import { waitFor } from "@testing-library/react";
import { IDetailedArtefactItem, IOverviewArtefactItemList } from "../../../../src/app/store/ducks/common/typings";

describe("Task creation widget", () => {
    afterEach(() => {
        mockAxios.reset();
    });

    const PROJECT_ID = "projectId";

    const createArtefactWrapper = (currentUrl: string = `${SERVE_PATH}`) => {
        const history = createBrowserHistory();
        history.location.pathname = currentUrl;
        return withMount(
            testWrapper(<CreateArtefactModal />, history, {
                common: {
                    artefactModal: {
                        isOpen: true,
                    },
                },
            })
        );
    };

    // Loads the selection list modal with mocked artefact list
    const createMockedListWrapper = async () => {
        const wrapper = createArtefactWrapper(`${SERVE_PATH}/projects/${PROJECT_ID}`);
        mockAxios.mockResponseFor(
            { url: apiUrl("core/taskPlugins") },
            mockedAxiosResponse({ data: mockArtefactListResponse })
        );
        await waitFor(() => expect(selectionItems(wrapper)).toHaveLength(3));
        return wrapper;
    };

    const fetchDialog = async (wrapper: ReactWrapper<any, any>) => {
        return await waitFor(() => {
            return findSingleElement(wrapper, byTestId("simpleDialogWidget"));
        });
    };

    const selectionItems: (dialogWrapper: ReactWrapper<any, any>) => ReactWrapper[] = (
        dialogWrapper: ReactWrapper<any, any>
    ) => {
        return findAll(dialogWrapper, ".ecc-overviewitem__list .ecc-overviewitem__item");
    };

    const mockArtefactListResponse: IOverviewArtefactItemList = {
        pluginA: {
            title: "Plugin A",
            description: "This is plugin A",
            taskType: "CustomTask",
            categories: [],
        },
        pluginB: {
            title: "Plugin B",
            description: "This is plugin B",
            taskType: "Dataset",
            categories: [],
        },
    };

    const mockPluginDescription: IDetailedArtefactItem = {
        title: "Plugin A",
        description: "This is plugin A",
        type: "object",
        taskType: "CustomTask",
        categories: ["category A", "category B"],
        required: [],
        pluginId: "pluginA",
        properties: {
            linkLimit: {
                advanced: true,
                description:
                    "The maximum number of links that should be generated. The execution will stop once this limit is reached.",
                parameterType: "int",
                title: "Link Limit",
                type: "string",
                value: "1000000",
                visibleInDialog: true,
            },
        },
    };

    it("should show only the project artefact to select when on the main search page", async () => {
        const wrapper = createArtefactWrapper();
        const dialog = await fetchDialog(wrapper);
        const items = selectionItems(dialog);
        expect(items).toHaveLength(1);
        expect(items[0].html()).toContain("Project");
    });

    it("should show the project artefact and all task artefacts when being in a project context", async () => {
        const wrapper = await createMockedListWrapper();
        const items = selectionItems(wrapper);
        expect(items[0].html()).toContain("Project");
        expect(items[1].html()).toContain("Plugin A");
        expect(items[2].html()).toContain("Plugin B");
        // Double click list item to trigger create dialog
        clickWrapperElement(items[2], 2);
        checkRequestMade(apiUrl("/core/plugins/pluginB"));
    });

    it("should open the plugin configuration dialog when double-clicking an item from the list", async () => {
        const wrapper = await createMockedListWrapper();
        const pluginA = selectionItems(wrapper)[1];
        // Double-click "Plugin A"
        clickWrapperElement(pluginA);
        clickWrapperElement(pluginA);
        mockAxios.mockResponseFor(
            { url: apiUrl("core/plugins/pluginA") },
            mockedAxiosResponse({ data: mockPluginDescription })
        );
        await waitFor(() => {
            expect(findAll(wrapper, "form label")).toHaveLength(1);
        });
        logRequests();
    });
});
