import React from "react";
import "@testing-library/jest-dom";
import mockAxios from "../../../__mocks__/axios";
import { SERVE_PATH } from "../../../../src/app/constants/path";
import { createMemoryHistory } from "history";

import {
    addDocumentCreateRangeMethod,
    apiUrl,
    byName,
    byTestId,
    changeInputValue,
    checkRequestMade,
    cleanUpDOM,
    clickRenderedElement,
    elementHtmlToContain,
    findAllDOMElements,
    findElement,
    legacyApiUrl,
    mockAxiosResponse,
    mockedAxiosError,
    mockedAxiosResponse,
    RecursivePartial,
    renderWrapper,
} from "../../TestHelper";
import { CreateArtefactModal } from "../../../../src/app/views/shared/modals/CreateArtefactModal/CreateArtefactModal";
import { fireEvent, RenderResult, waitFor, screen, within } from "@testing-library/react";
import {
    IOverviewArtefactItemList,
    IPluginDetails,
    IProjectTaskUpdatePayload,
} from "../../../../src/app/store/ducks/common/typings";
import { atomicParamDescription, mockAutoCompleteResponse, objectParamDescription } from "./CreateArtefactModalHelper";
import { INPUT_TYPES } from "../../../../src/app/constants";
import { TaskTypes } from "../../../../src/app/store/ducks/shared/typings";
import { MemoryHistory } from "history/createMemoryHistory";
import { bluePrintClassPrefix } from "../../../HierarchicalMapping/utils/TestHelpers";

describe("Task creation widget", () => {
    beforeAll(() => {
        window.HTMLElement.prototype.scrollIntoView = function () {};
    });

    afterEach(() => {
        mockAxios.reset();
        cleanUpDOM();
    });

    const PROJECT_ID = "projectId";
    const TASK_ID = "taskId";

    interface IWrapper {
        element: HTMLElement;
        history: MemoryHistory<{}>;
    }

    const createArtefactWrapper = (
        currentUrl: string = `${SERVE_PATH}`,
        existingTask?: RecursivePartial<IProjectTaskUpdatePayload>,
    ): IWrapper => {
        const history = createMemoryHistory<{}>();
        history.push(currentUrl);

        const wrapper = renderWrapper(<CreateArtefactModal />, history, {
            common: {
                initialSettings: {
                    emptyWorkspace: false,
                    templatingEnabled: true,
                },
                artefactModal: {
                    isOpen: true,
                    updateExistingTask: existingTask,
                },
            },
        });
        return { element: wrapper.baseElement, history };
    };

    // Loads the selection list modal with mocked artefact list
    const createMockedListWrapper = async (existingTask?: RecursivePartial<IProjectTaskUpdatePayload>) => {
        const wrapper = createArtefactWrapper(`${SERVE_PATH}/projects/${PROJECT_ID}`, existingTask);
        const url = apiUrl("core/taskPlugins?addMarkdownDocumentation=true");
        mockAxios.mockResponseFor({ url }, mockedAxiosResponse({ data: mockArtefactListResponse }));
        if (!existingTask) {
            await waitFor(() => {
                expect(selectionItems(wrapper.element)).toHaveLength(3);
            });
        }
        return wrapper;
    };

    const fetchDialog = async (wrapper: RenderResult | Element) => {
        return await waitFor(() => {
            return findElement(wrapper, byTestId("simpleDialogWidget"));
        });
    };

    const selectionItems = (dialogWrapper: RenderResult | Element): Element[] => {
        return findAllDOMElements(dialogWrapper, ".eccgui-overviewitem__list .eccgui-overviewitem__item");
    };

    const pluginCreationDialogWrapper = async (
        doubleClickToAdd: boolean = true,
        // The current data of a task that is being updated
        existingTask?: RecursivePartial<IProjectTaskUpdatePayload>,
    ) => {
        const wrapper = await createMockedListWrapper(existingTask);
        const pluginA = selectionItems(wrapper.element)[1];
        if (!existingTask) {
            if (doubleClickToAdd) {
                // Double-click "Plugin A"
                clickRenderedElement(pluginA);
                clickRenderedElement(pluginA);
            } else {
                // Use Add button
                clickRenderedElement(pluginA);
                clickRenderedElement(findElement(wrapper.element, byTestId("item-add-btn")));
            }
            mockAxios.mockResponseFor(
                { url: apiUrl("core/plugins/pluginA") },
                mockedAxiosResponse({ data: mockPluginDescription }),
            );
        }
        await waitFor(() => {
            const labels = findAllDOMElements(wrapper.element, ".eccgui-label .eccgui-label__text").map(
                (e) => e.textContent,
            );
            Object.entries(mockPluginDescription.properties).forEach(([paramId, attributes]) =>
                expect(labels).toContain(attributes.title),
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

    const mockPluginDescription: IPluginDetails = {
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
                {},
            ),
            autoCompletionParamCustom: atomicParamDescription(
                { title: "auto-complete param that allows custom values", parameterType: INPUT_TYPES.STRING },
                { allowOnlyAutoCompletedValues: false },
            ),
            optionalAutoCompletionParamCustom: atomicParamDescription(
                { title: "auto-complete param that allows resetting it's value", parameterType: INPUT_TYPES.STRING },
                {},
            ),
            objectParameter: objectParamDescription(
                "pluginX",
                {
                    subProperty: atomicParamDescription(
                        { title: "nested auto-complete param", parameterType: INPUT_TYPES.STRING },
                        {},
                    ),
                    subStringParam: atomicParamDescription({
                        title: "string param",
                        parameterType: INPUT_TYPES.STRING,
                    }),
                },
                ["subStringParam"],
                {},
            ),
        },
    };

    it("should show only the project artefact to select when on the main search page", async () => {
        const { element } = await createMockedListWrapper();
        const dialog = await fetchDialog(element);
        const items = selectionItems(dialog);
        expect(items).toHaveLength(3);
        expect(items[0].innerHTML).toContain("Project");
    });

    it("should show the project artefact and all task artefacts when being in a project context", async () => {
        const { element } = await createMockedListWrapper();
        const items = selectionItems(element);
        expect(items[0].innerHTML).toContain("Project");
        expect(items[1].innerHTML).toContain("Plugin A");
        expect(items[2].innerHTML).toContain("Plugin B");
        // Double click list item to trigger create dialog
        clickRenderedElement(items[2], 2);
        checkRequestMade(apiUrl("/core/plugins/pluginB"));
    });

    it("should open the plugin configuration dialog when double-clicking an item from the list", async () => {
        await pluginCreationDialogWrapper();
    });

    it("should open the plugin configuration dialog when clicking an item from the list and then 'Add'", async () => {
        await pluginCreationDialogWrapper(false);
    });

    //failing for some reason
    it("should show a form with parameters of different types", async () => {
        const { element } = await pluginCreationDialogWrapper();
        // boolean parameter
        expect(findAllDOMElements(element, 'input[type="checkbox"]')).toHaveLength(1);
        // password parameter
        expect(findAllDOMElements(element, 'input[type="password"]')).toHaveLength(1);
        // resource parameter radio options
        expect(findAllDOMElements(element, 'input[type="radio"]')).toHaveLength(3);
        // resource and object parameter
        expect(findAllDOMElements(element, "legend")).toHaveLength(2);
        // restriction and multi-line use code mirror widget
        expect(findAllDOMElements(element, byTestId("codemirror-wrapper"))).toHaveLength(3);
        // Default values should be set
        // await elementHtmlToContain(element, "#stringParam", "default string");
    });

    // Click the create button in the create dialog
    const clickCreate = (wrapper) => clickRenderedElement(findElement(wrapper, byTestId("createArtefactButton")));
    // Checks the number of expected validation errors
    const expectValidationErrors = async (wrapper, nrErrors: number) =>
        await waitFor(() => {
            // label, intParam and subProperty should be marked with validation errors
            expect(findAllDOMElements(wrapper, ".eccgui-intent--danger").length).toBe(nrErrors);
        });

    it("should show validation errors for an unfinished form when clicking 'Create'", async () => {
        const { element } = await pluginCreationDialogWrapper();
        clickCreate(element);
        await expectValidationErrors(element, 6);
        // Enter valid value for int parameter
        changeInputValue(findElement(element, "#intParam") as HTMLInputElement, "100");
        await expectValidationErrors(element, 4);
        // Enter invalid value for int parameter
        changeInputValue(findElement(element, "#intParam") as HTMLInputElement, "abc");
        await expectValidationErrors(element, 6);
    });

    it("should send the correct request when clicking 'Create' on a valid form", async () => {
        const { element, history } = await pluginCreationDialogWrapper();
        changeInputValue(findElement(element, "#intParam") as HTMLInputElement, "100");
        changeInputValue(findElement(element, "#label") as HTMLInputElement, "Some label");
        /** FIXME: CodeMirror Editor refed in the codemirror-wrapper div doesn't show and is still null even at this point
         * This wasn't the case with version 5 where I could do this document.querySelector('#description .CodeMirror').CodeMirror.setValue('')
         * In v6 I should be able to do cmView.view.dispatch({ changes: {from:0, to: document.querySelector('.cm-content').cmView.view.state.doc.length, insert:''}})
         * but again the editor returns null, even after waiting
         * created follow up issue https://jira.eccenca.com/browse/CMEM-6208
         */
        changeInputValue(
            findElement(element, byName("objectParameter.subStringParam")) as HTMLInputElement,
            "Something",
        );
        clickCreate(element);
        await expectValidationErrors(element, 0);
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
                expect.stringMatching(new RegExp(`projects/${PROJECT_ID}/task/${newTaskId}$`)),
            );
        });
    });

    it("should show an error message if task creation failed in the backend", async () => {
        const { element } = await pluginCreationDialogWrapper();
        changeInputValue(findElement(element, "#intParam") as HTMLInputElement, "100");
        changeInputValue(findElement(element, "#label") as HTMLInputElement, "Some label");
        changeInputValue(
            findElement(element, byName("objectParameter.subStringParam")) as HTMLInputElement,
            "Something",
        );
        clickCreate(element);
        await expectValidationErrors(element, 0);
        const expectedErrorMsg = "internal server error ;)";
        await waitFor(() => {
            mockAxiosResponse(
                legacyApiUrl("workspace/projects/projectId/tasks"),
                mockedAxiosError(500, { title: "error", detail: expectedErrorMsg }),
            );
        });
        await waitFor(() => {
            const error = findElement(element, ".eccgui-intent--danger");
            expect(error.textContent.toLowerCase()).toContain(expectedErrorMsg);
        });
    });

    it("should allow to create a new project", async () => {
        const { element } = await createMockedListWrapper();
        const PROJECT_LABEL = "Project label";
        const PROJECT_DESCRIPTION = "Project description";
        const project = selectionItems(element)[0];
        clickRenderedElement(project);
        clickRenderedElement(project);
        expect(findAllDOMElements(element, "#label")).toHaveLength(1);
        changeInputValue(findElement(element, "#label") as HTMLInputElement, PROJECT_LABEL);
        /** FIXME: CodeMirror Editor refed in the codemirror-wrapper div doesn't show and is still null even at this point
         * This wasn't the case with version 5 where I could do this document.querySelector('#description .CodeMirror').CodeMirror.setValue('')
         * In v6 I should be able to do cmView.view.dispatch({ changes: {from:0, to: document.querySelector('.cm-content').cmView.view.state.doc.length, insert:''}})
         * but again the editor returns null, even after waiting
         * created follow up issue https://jira.eccenca.com/browse/CMEM-6208
         */
        clickCreate(element);
        await expectValidationErrors(element, 0);
        await waitFor(() => {
            const expectedPayload = {
                metaData: {
                    label: PROJECT_LABEL,
                },
            };
            checkRequestMade(apiUrl("/workspace/projects"), "POST", expectedPayload);
        });
    });

    it("should allow to reset optional auto-completed values", async () => {
        // document.createRange is needed from the popover of the auto-complete element
        addDocumentCreateRangeMethod();
        const { element } = await pluginCreationDialogWrapper();
        const autoCompleteInput = findElement(element, "#optionalAutoCompletionParamCustom");
        expect(window.document.querySelectorAll(".eccgui-spinner").length).toBe(0);
        // input must be focused in order to fire requests
        autoCompleteInput.focus();
        changeInputValue(autoCompleteInput as HTMLInputElement, "abc");
        const beforePortals = window.document.querySelectorAll(`div.${bluePrintClassPrefix}-portal`).length;
        await waitFor(() => {
            expect(window.document.querySelectorAll(".eccgui-spinner").length).toBe(1);
        });
        await waitFor(() => {
            // Request is delayed by 200ms
            mockAutoCompleteResponse(
                { textQuery: "abc" },
                mockedAxiosResponse({ data: [{ value: "abc1" }, { value: "abc2" }] }),
            );
        });
        await waitFor(() => {
            expect(window.document.querySelectorAll(".eccgui-spinner").length).toBe(0);
        });
        // FIXME: Blueprint portal with suggestion results is not shown
        // await waitFor(() => {
        //     expect(window.document.querySelectorAll(`div.${bluePrintClassPrefix}-portal`).length).toBeGreaterThan(beforePortals)
        // })
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
        autoCompletionParamCustom: value(""),
        optionalAutoCompletionParamCustom: value(""),
        objectParameter: {
            value: {
                subProperty: value("subProperty value", "subProperty label"),
                subStringParam: value("subStringParam value"),
            },
        },
    };

    const existingTask = {
        projectId: PROJECT_ID,
        taskId: TASK_ID,
        taskPluginDetails: mockPluginDescription,
        metaData: {
            label: "Task label",
        },
        currentParameterValues: expectedParams,
        currentTemplateValues: {},
    };

    // Updates a task via update button and returns the request data
    const updateTask = async (wrapper) => {
        clickCreate(wrapper);
        await expectValidationErrors(wrapper, 0);
        return mockAxios.getReqMatching({
            url: legacyApiUrl("workspace/projects/projectId/tasks/taskId"),
            method: "PATCH",
        }).data;
    };

    it("should use existing values to set the initial parameter values on update", async () => {
        const { element } = await pluginCreationDialogWrapper(true, existingTask);
        const updateRequest = await updateTask(element);
        // Build expected request parameter object
        const expectedObject: any = {};
        Object.entries(expectedParams).forEach(([key, value]) => (expectedObject[key] = value.value));
        const objectParameterObject: any = {};
        Object.entries(expectedParams.objectParameter.value).forEach(
            ([key, value]) => (objectParameterObject[key] = value.value),
        );
        expectedObject.objectParameter = objectParameterObject;
        expect(updateRequest.data.parameters).toEqual(expectedObject);
    });

    it("should use existing template values on initialization and update", async () => {
        const { element } = await pluginCreationDialogWrapper(true, {
            ...existingTask,
            currentTemplateValues: {
                stringParam: "{{globalVariable}}",
            },
        });
        await waitFor(() => findElement(element, byTestId("stringParam-template-switch-back-btn")));
        /** FIXME: CodeMirror Editor refed in the codemirror-wrapper div doesn't show and is still null even at this point
         * This wasn't the case with version 5 where I could do this document.querySelector('#description .CodeMirror').CodeMirror.setValue('')
         * In v6 I should be able to do cmView.view.dispatch({ changes: {from:0, to: document.querySelector('.cm-content').cmView.view.state.doc.length, insert:''}})
         * but again the editor returns null, even after waiting 
         * created follow up issue https://jira.eccenca.com/browse/CMEM-6208
         *  await waitFor(() =>
             expect(findSingleElement(wrapper, "#restrictionParam").text()).toContain("restriction value") 
           );
         */
        const updateRequest = await updateTask(element);
        // Build expected request parameter object
        const expectedObject: any = {};
        Object.entries(expectedParams).forEach(
            ([key, value]) => key !== "stringParam" && (expectedObject[key] = value.value),
        );
        const objectParameterObject: any = {};
        Object.entries(expectedParams.objectParameter.value).forEach(
            ([key, value]) => (objectParameterObject[key] = value.value),
        );
        expectedObject.objectParameter = objectParameterObject;
        expect(updateRequest.data.parameters).toEqual(expectedObject);
    });

    it("should check if the info Icon for task artefact exist", async () => {
        const { element } = await createMockedListWrapper();
        const dialog = await fetchDialog(element);
        const items = selectionItems(dialog);
        const randomItem = items[0];
        const iconButton = randomItem.querySelector(".eccgui-overviewitem__actions .eccgui-button--icon");
        expect(iconButton !== null).toBeTruthy();
    });

    it("should show the info dialog when info icon is clicked", async () => {
        const { element } = await createMockedListWrapper();
        const dialog = await fetchDialog(element);
        const items = selectionItems(dialog);
        const randomItem = items[0];
        const iconButton = randomItem.querySelector(".eccgui-overviewitem__actions button.eccgui-button--icon");
        iconButton && fireEvent.click(iconButton);
        const infoDialog = element.querySelector(".eccgui-card");
        expect(infoDialog !== null).toBeTruthy();
    });
});
