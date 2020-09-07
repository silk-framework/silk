import React from "react";
import "@testing-library/jest-dom";
import mockAxios from "../../../__mocks__/axios";
import { ReactWrapper } from "enzyme";
import { SERVE_PATH } from "../../../../src/app/constants/path";
import { createMemoryHistory } from "history";

import {
    apiUrl,
    byName,
    byTestId,
    changeValue,
    checkRequestMade,
    clickWrapperElement,
    findAll,
    findSingleElement,
    legacyApiUrl,
    mockAxiosResponse,
    mockedAxiosError,
    mockedAxiosResponse,
    RecursivePartial,
    testWrapper,
    withMount,
} from "../../TestHelper";
import { CreateArtefactModal } from "../../../../src/app/views/shared/modals/CreateArtefactModal/CreateArtefactModal";
import { waitFor } from "@testing-library/react";
import {
    IDetailedArtefactItem,
    IOverviewArtefactItemList,
    IProjectTaskUpdatePayload,
} from "../../../../src/app/store/ducks/common/typings";
import { atomicParamDescription, objectParamDescription } from "./CreateArtefactModalHelper";
import { INPUT_TYPES } from "../../../../src/app/constants";
import { TaskTypes } from "../../../../src/app/store/ducks/shared/typings";
import { MemoryHistory } from "history/createMemoryHistory";

describe("Task creation widget", () => {
    beforeAll(() => {
        window.HTMLElement.prototype.scrollIntoView = function () {};
    });

    afterEach(() => {
        mockAxios.reset();
    });

    const PROJECT_ID = "projectId";
    const TASK_ID = "taskId";

    interface IWrapper {
        wrapper: ReactWrapper<any, any>;
        history: MemoryHistory<{}>;
    }

    const createArtefactWrapper = (
        currentUrl: string = `${SERVE_PATH}`,
        existingTask?: RecursivePartial<IProjectTaskUpdatePayload>
    ): IWrapper => {
        const history = createMemoryHistory();
        history.push(currentUrl);

        const provider = testWrapper(<CreateArtefactModal />, history, {
            common: {
                artefactModal: {
                    isOpen: true,
                    updateExistingTask: existingTask,
                },
            },
        });
        return { wrapper: withMount(provider), history };
    };

    // Loads the selection list modal with mocked artefact list
    const createMockedListWrapper = async (existingTask?: RecursivePartial<IProjectTaskUpdatePayload>) => {
        const wrapper = createArtefactWrapper(`${SERVE_PATH}/projects/${PROJECT_ID}`, existingTask);
        mockAxios.mockResponseFor(
            { url: apiUrl("core/taskPlugins") },
            mockedAxiosResponse({ data: mockArtefactListResponse })
        );
        if (!existingTask) {
            await waitFor(() => expect(selectionItems(wrapper.wrapper)).toHaveLength(3));
        }
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
        return findAll(dialogWrapper, ".eccgui-overviewitem__list .eccgui-overviewitem__item");
    };

    const pluginCreationDialogWrapper = async (existingTask?: RecursivePartial<IProjectTaskUpdatePayload>) => {
        const wrapper = await createMockedListWrapper(existingTask);
        const pluginA = selectionItems(wrapper.wrapper)[1];
        if (!existingTask) {
            // Double-click "Plugin A"
            clickWrapperElement(pluginA);
            clickWrapperElement(pluginA);
            mockAxios.mockResponseFor(
                { url: apiUrl("core/plugins/pluginA") },
                mockedAxiosResponse({ data: mockPluginDescription })
            );
        }
        await waitFor(() => {
            const labels = findAll(wrapper.wrapper, ".eccgui-label .eccgui-label__text").map((e) => e.text());
            Object.entries(mockPluginDescription.properties).forEach(([paramId, attributes]) =>
                expect(labels).toContain(attributes.title)
            );
        });
        return wrapper;
    };

    const mockArtefactListResponse: IOverviewArtefactItemList = {
        pluginA: {
            title: "Plugin A",
            description: "This is plugin A",
            taskType: TaskTypes.CUSTOM_TASK,
            categories: [],
        },
        pluginB: {
            title: "Plugin B",
            description: "This is plugin B",
            taskType: TaskTypes.DATASET,
            categories: [],
        },
    };

    const mockPluginDescription: IDetailedArtefactItem = {
        title: "Plugin A",
        description: "This is plugin A",
        type: "object",
        taskType: TaskTypes.CUSTOM_TASK,
        categories: ["category A", "category B"],
        required: ["intParam"],
        pluginId: "pluginA",
        properties: {
            intParam: atomicParamDescription({ title: "integer param", parameterType: INPUT_TYPES.INTEGER }),
            booleanParam: atomicParamDescription({ title: "boolean param", parameterType: INPUT_TYPES.BOOLEAN }),
            stringParam: atomicParamDescription({
                title: "string param",
                parameterType: INPUT_TYPES.STRING,
                value: "default string",
            }),
            restrictionParam: atomicParamDescription({
                title: "restriction param",
                parameterType: INPUT_TYPES.RESTRICTION,
            }),
            multiLineParam: atomicParamDescription({
                title: "multi-line param",
                parameterType: INPUT_TYPES.MULTILINE_STRING,
            }),
            passwordParam: atomicParamDescription({ title: "password param", parameterType: INPUT_TYPES.PASSWORD }),
            resourceParam: atomicParamDescription({ title: "resource param", parameterType: INPUT_TYPES.RESOURCE }),
            enumerationParam: atomicParamDescription(
                { title: "enumeration param", parameterType: INPUT_TYPES.ENUMERATION },
                {}
            ),
            objectParameter: objectParamDescription(
                "pluginX",
                {
                    subProperty: atomicParamDescription(
                        { title: "nested auto-complete param", parameterType: INPUT_TYPES.STRING },
                        {}
                    ),
                    subStringParam: atomicParamDescription({
                        title: "string param",
                        parameterType: INPUT_TYPES.STRING,
                    }),
                },
                ["subStringParam"],
                {}
            ),
        },
    };

    it("should show only the project artefact to select when on the main search page", async () => {
        const { wrapper } = createArtefactWrapper();
        const dialog = await fetchDialog(wrapper);
        const items = selectionItems(dialog);
        expect(items).toHaveLength(1);
        expect(items[0].html()).toContain("Project");
    });

    it("should show the project artefact and all task artefacts when being in a project context", async () => {
        const { wrapper } = await createMockedListWrapper();
        const items = selectionItems(wrapper);
        expect(items[0].html()).toContain("Project");
        expect(items[1].html()).toContain("Plugin A");
        expect(items[2].html()).toContain("Plugin B");
        // Double click list item to trigger create dialog
        clickWrapperElement(items[2], 2);
        checkRequestMade(apiUrl("/core/plugins/pluginB"));
    });

    it("should open the plugin configuration dialog when double-clicking an item from the list", async () => {
        await pluginCreationDialogWrapper();
    });

    it("should show a form with parameters of different types", async () => {
        const { wrapper } = await pluginCreationDialogWrapper();
        // boolean parameter
        expect(findAll(wrapper, 'input[type="checkbox"]')).toHaveLength(1);
        // password parameter
        expect(findAll(wrapper, 'input[type="password"]')).toHaveLength(1);
        // resource parameter radio options
        expect(findAll(wrapper, 'input[type="radio"]')).toHaveLength(3);
        // resource and object parameter
        expect(findAll(wrapper, "legend")).toHaveLength(2);
        // restriction and multi-line use code mirror widget
        expect(findAll(wrapper, byTestId("codemirror-wrapper"))).toHaveLength(2);
    });

    // Click the create button in the create dialog
    const clickCreate = (wrapper) => clickWrapperElement(findSingleElement(wrapper, byTestId("createArtefactButton")));
    // Checks the number of expected validation errors
    const expectValidationErrors = async (wrapper, nrErrors: number) =>
        await waitFor(() => {
            // label, intParam and subProperty should be marked with validation errors
            expect(findAll(wrapper, ".eccgui-intent--danger").length).toBe(nrErrors);
        });

    it("should show validation errors for an unfinished form when clicking 'Create'", async () => {
        const { wrapper } = await pluginCreationDialogWrapper();
        clickCreate(wrapper);
        await expectValidationErrors(wrapper, 3);
        // Enter valid value for int parameter
        changeValue(findSingleElement(wrapper, "#intParam"), "100");
        await expectValidationErrors(wrapper, 2);
        // Enter invalid value for int parameter
        changeValue(findSingleElement(wrapper, "#intParam"), "abc");
        await expectValidationErrors(wrapper, 3);
    });

    it("should send the correct request when clicking 'Create' on a valid form", async () => {
        const { wrapper, history } = await pluginCreationDialogWrapper();
        changeValue(findSingleElement(wrapper, "#intParam"), "100");
        changeValue(findSingleElement(wrapper, "#label"), "Some label");
        changeValue(findSingleElement(wrapper, byName("objectParameter.subStringParam")), "Something");
        clickCreate(wrapper);
        await expectValidationErrors(wrapper, 0);
        const tasksUri = legacyApiUrl("workspace/projects/projectId/tasks");
        const request = mockAxios.getReqByUrl(tasksUri);
        expect(request).toBeTruthy();
        const metaData = request.data.metadata;
        const data = request.data.data;
        expect(metaData.label).toBe("Some label");
        expect(data.taskType).toBe(TaskTypes.CUSTOM_TASK);
        expect(data.type).toBe("pluginA");
        expect(data.parameters.intParam).toEqual("100");
        expect(data.parameters.booleanParam).toEqual("false");
        expect(data.parameters.objectParameter.subStringParam).toEqual("Something");
        expect(data.parameters.stringParam).toEqual("default string");
        // Test redirection to task details page
        const newTaskId = "newTaskId";
        mockAxiosResponse(tasksUri, { data: { id: newTaskId } });
        await waitFor(() => {
            expect(history.location.pathname).toEqual(
                expect.stringMatching(new RegExp(`projects/${PROJECT_ID}/task/${newTaskId}$`))
            );
        });
    });

    it("should show an error message if task creation failed in the backend", async () => {
        const { wrapper } = await pluginCreationDialogWrapper();
        changeValue(findSingleElement(wrapper, "#intParam"), "100");
        changeValue(findSingleElement(wrapper, "#label"), "Some label");
        changeValue(findSingleElement(wrapper, byName("objectParameter.subStringParam")), "Something");
        clickCreate(wrapper);
        await expectValidationErrors(wrapper, 0);
        const expectedErrorMsg = "internal server error ;)";
        await waitFor(() => {
            mockAxiosResponse(
                legacyApiUrl("workspace/projects/projectId/tasks"),
                mockedAxiosError(500, { title: "error", detail: expectedErrorMsg })
            );
        });
        await waitFor(() => {
            const error = findSingleElement(wrapper, ".eccgui-intent--danger");
            expect(error.text().toLowerCase()).toContain(expectedErrorMsg);
        });
    });

    const value = (value: string, label?: string) => {
        const result: { value: string; label?: string } = { value };
        if (label) {
            result.label = label;
        }
        return result;
    };
    const expectedParams = {
        intParam: value("100"),
        booleanParam: value("true"),
        stringParam: value("string value"),
        restrictionParam: value("restriction value"),
        multiLineParam: value("multiline value"),
        passwordParam: value("password value"),
        resourceParam: value("resource value"),
        enumerationParam: value("enumeration value", "enumeration label"),
        objectParameter: {
            value: {
                subProperty: value("subProperty value", "subProperty label"),
                subStringParam: value("subStringParam value"),
            },
        },
    };

    it("should use existing values to set the initial parameter values on update", async () => {
        const { wrapper } = await pluginCreationDialogWrapper({
            projectId: PROJECT_ID,
            taskId: TASK_ID,
            taskPluginDetails: mockPluginDescription,
            metaData: {
                label: "Task label",
            },
            currentParameterValues: expectedParams,
        });
        clickCreate(wrapper);
        await expectValidationErrors(wrapper, 0);
        const updateRequest = mockAxios.getReqMatching({
            url: legacyApiUrl("workspace/projects/projectId/tasks/taskId"),
            method: "PATCH",
        }).data;
        // Build expected request parameter object
        const expectedObject: any = {};
        Object.entries(expectedParams).forEach(([key, value]) => (expectedObject[key] = value.value));
        const objectParameterObject: any = {};
        Object.entries(expectedParams.objectParameter.value).forEach(
            ([key, value]) => (objectParameterObject[key] = value.value)
        );
        expectedObject.objectParameter = objectParameterObject;
        expect(updateRequest.data.parameters).toEqual(expectedObject);
    });
});
