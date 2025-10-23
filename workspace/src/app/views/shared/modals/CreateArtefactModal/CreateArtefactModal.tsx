import React, { useEffect, useRef, useState } from "react";
import { batch, useDispatch, useSelector } from "react-redux";
import { useForm } from "react-hook-form";
import {
    Button,
    Card,
    CardActionsAux,
    Grid,
    GridColumn,
    GridRow,
    ClassNames,
    Highlighter,
    IconButton,
    Notification,
    OverflowText,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDepiction,
    OverviewItemDescription,
    OverviewItemLine,
    OverviewItemList,
    SimpleDialog,
    Spacing,
    highlighterUtils,
    Markdown,
    MenuItem,
    ContextMenu,
    Accordion,
    AccordionItem,
    Depiction,
    Spinner,
} from "@eccenca/gui-elements";
import { commonOp, commonSel } from "@ducks/common";
import {
    IArtefactModal,
    IPluginDetails,
    IPluginOverview,
    IProjectTaskUpdatePayload,
    TaskPreConfiguration,
} from "@ducks/common/typings";
import Loading from "../../Loading";
import { ProjectForm } from "./ArtefactForms/ProjectForm";
import { TaskForm } from "./ArtefactForms/TaskForm";
import { DATA_TYPES } from "../../../../constants";
import ArtefactTypesList from "./ArtefactTypesList";
import { SearchBar } from "../../SearchBar/SearchBar";
import { routerOp } from "@ducks/router";
import { useTranslation } from "react-i18next";
import { DatasetTaskPlugin, TaskType } from "@ducks/shared/typings";
import { ProjectImportModal } from "../ProjectImportModal";
import ItemDepiction from "../../../shared/ItemDepiction";
import ProjectSelection from "./ArtefactForms/ProjectSelection";
import { workspaceSel } from "@ducks/workspace";
import { requestSearchList } from "@ducks/workspace/requests";
import { objectToFlatRecord, uppercaseFirstChar } from "../../../../utils/transformers";
import { performAction, requestProjectMetadata } from "@ducks/shared/requests";
import { requestAutoConfiguredDataset } from "./CreateArtefactModal.requests";
import { diErrorMessage } from "@ducks/error/typings";
import useHotKey from "../../HotKeyHandler/HotKeyHandler";
import { CreateArtefactModalContext } from "./CreateArtefactModalContext";
import { TaskDocumentationModal } from "./TaskDocumentationModal";
import { PARAMETER_DOC_PREFIX } from "./ArtefactForms/TaskForm";

const ignorableFields = new Set(["label", "description"]);

export interface ProjectIdAndLabel {
    id: string;
    label: string;
}

export interface InfoMessage {
    message: string;
    removeAfterSeconds?: number;
}

export interface ArtefactDocumentation {
    key: string;
    title?: string;
    description?: string;
    markdownDocumentation?: string;
    namedAnchor: string | undefined;
}

export function CreateArtefactModal() {
    const MAX_SINGLEPLUGINBUTTONS = 2;

    const dispatch = useDispatch();
    const form = useForm();

    const [searchValue, setSearchValue] = useState("");
    const [documentationToShow, setDocumentationToShow] = useState<ArtefactDocumentation | undefined>(undefined);
    const documentationIsShown = React.useRef(false);
    documentationIsShown.current = !!documentationToShow;
    const [actionLoading, setActionLoading] = useState(false);
    const [t] = useTranslation();

    const { maxFileUploadSize } = useSelector(commonSel.initialSettingsSelector);
    const modalStore = useSelector(commonSel.artefactModalSelector);

    const {
        selectedArtefact: selectedArtefactFromStore,
        isOpen,
        artefactsList,
        cachedArtefactProperties,
        selectedDType,
        loading,
        updateExistingTask,
        error,
        info,
        newTaskPreConfiguration,
    }: IArtefactModal = modalStore;

    React.useEffect(() => {
        if (newTaskPreConfiguration?.taskPluginId && artefactsList) {
            const pluginOverview = artefactsList.find((plugin) => plugin.key === newTaskPreConfiguration.taskPluginId);
            if (pluginOverview) {
                dispatch(commonOp.getArtefactPropertiesAsync(pluginOverview));
            }
        }
    }, [newTaskPreConfiguration?.taskPluginId, artefactsList]);

    // The artefact that is selected from the artefact selection list. This can be pre-selected via the Redux state.
    // A successive 'Add' action will open the creation dialog for this artefact.
    const toBeAdded = useRef<IPluginOverview | undefined>(selectedArtefactFromStore);
    const toBeAddedKey = useRef<string | undefined>(selectedArtefactFromStore?.key);
    const [lastSelectedClick, setLastSelectedClick] = useState<number>(0);
    const [isProjectImport, setIsProjectImport] = useState<boolean>(false);
    const [autoConfigPending, setAutoConfigPending] = useState(false);
    const DOUBLE_CLICK_LIMIT_MS = 500;

    const updateTaskPluginDetails: IPluginOverview | undefined = updateExistingTask
        ? { ...updateExistingTask.taskPluginDetails, key: updateExistingTask.taskPluginDetails.pluginId }
        : undefined;
    const selectedArtefact: IPluginOverview | undefined = updateTaskPluginDetails ?? selectedArtefactFromStore;
    const selectedArtefactKey: string | undefined = selectedArtefactFromStore?.key;
    const selectedArtefactTitle: string | undefined = selectedArtefact?.title;
    const [currentProject, setCurrentProject] = useState<ProjectIdAndLabel | undefined>(undefined);
    const [showProjectSelection, setShowProjectSelection] = useState<boolean>(false);
    const [formValueChanges, setFormValueChanges] = React.useState<{
        [key: string]: {
            initialValue: any;
            isModified: boolean;
        };
    }>({});
    const [infoMessage, setInfoMessage] = useState<InfoMessage | undefined>(undefined);
    const isEmptyWorkspace = useSelector(workspaceSel.isEmptyPageSelector);
    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const externalParameterUpdateMap = React.useRef(
        new Map<string, (value: { value: string; label?: string }) => any>(),
    );
    const templateParameters = React.useRef(new Set<string>());

    const setToBeAdded = React.useCallback((plugin: IPluginOverview | undefined) => {
        toBeAdded.current = plugin;
        toBeAddedKey.current = plugin?.key;
    }, []);
    const [taskActionResult, setTaskActionResult] = React.useState<{ label: string; message: string }>();
    const [taskActionLoading, setTaskActionLoading] = React.useState<string | null>(null);
    const [taskFormGeneralWarning, setTaskFormGeneralWarning] = React.useState<string | undefined>();
    const generalWarningTimeout = React.useRef<number | undefined>()

    const taskFormWarning = React.useCallback((message: string) => {
        if(generalWarningTimeout.current) {
            clearTimeout(generalWarningTimeout.current)
        }
        generalWarningTimeout.current = window.setTimeout(() => setTaskFormGeneralWarning(message), 250)
    }, [])

    React.useEffect(() => {
        if (infoMessage?.removeAfterSeconds && infoMessage.removeAfterSeconds > 0) {
            const timeoutId = setTimeout(() => {
                setInfoMessage(undefined);
            }, infoMessage.removeAfterSeconds * 1000);
            return () => clearTimeout(timeoutId);
        }
    }, [infoMessage]);

    const registerError = React.useCallback((errorId: string, errorMessage: string, error: any) => {
        const diServerMessage = diErrorMessage(error);
        const errorDetails = diServerMessage ? ` Details: ${diServerMessage}` : "";
        const m = errorMessage.trim().endsWith(".")
            ? `${errorMessage}${errorDetails}`
            : `${errorMessage.trim()}.${errorDetails}`;
        const newError = {
            ...error,
            errorMessage: m,
            details: diErrorMessage(error),
            cause: error,
        };
        dispatch(commonOp.setModalError(newError));
    }, []);

    const resetModalError = (didTimeoutExpire: boolean = false) => {
        !didTimeoutExpire && dispatch(commonOp.setModalError({}));
    };

    const resetModalInfo = () => {
        dispatch(commonOp.setModalInfo(undefined));
    };

    // Function to set template parameter flag for a parameter
    const setTemplateFlag = React.useCallback((parameterId: string, isTemplate: boolean) => {
        if (isTemplate) {
            templateParameters.current.add(parameterId);
        } else {
            templateParameters.current.delete(parameterId);
        }
    }, []);

    const templateFlag = React.useCallback((parameterId: string) => templateParameters.current.has(parameterId), []);

    /** set the current Project when opening modal from a project
     * i.e project id already exists **/
    React.useEffect(() => {
        if (projectId && isOpen) {
            (async () => {
                try {
                    const projectLabel = (await requestProjectMetadata(projectId)).data.label;
                    setCurrentProject({ id: projectId, label: projectLabel });
                } catch (e) {
                    registerError(
                        "CreateArtefactModal-fetch-project-meta-data",
                        "Could not fetch project information",
                        e,
                    );
                }
            })();
        }
    }, [projectId, selectedArtefactKey, isOpen]);

    useEffect(() => {
        if (!isOpen) {
            // Reset modal when it was closed
            resetModal(true);
        } else {
            // Clear errors when freshly opened
            form.clearError();
        }
    }, [isOpen]);

    // Fetch Artefact list
    useEffect(() => {
        if (isOpen && !isEmptyWorkspace) {
            batch(() => {
                dispatch(commonOp.fetchAvailableDTypesAsync(projectId));
                dispatch(
                    commonOp.fetchArtefactsListAsync({
                        textQuery: searchValue,
                    }),
                );
            });
        } else {
            dispatch(commonOp.resetArtefactsList());
        }
    }, [isOpen, projectId]);

    const handleAdd = () => {
        if (toBeAddedKey.current === DATA_TYPES.PROJECT) {
            return dispatch(commonOp.selectArtefact(toBeAdded.current));
        } else if (toBeAdded.current) {
            dispatch(commonOp.getArtefactPropertiesAsync(toBeAdded.current));
        } else {
            console.error("No item plugin selected, cannot add new item!");
        }
    };

    const handleSearch = (textQuery: string) => {
        setSearchValue(textQuery);
        if (!isEmptyWorkspace) {
            dispatch(
                commonOp.fetchArtefactsListAsync({
                    textQuery,
                }),
            );
        }
    };

    const getWorkspaceProjects = async (textQuery: string = "") => {
        try {
            const payload = {
                limit: 10,
                offset: 0,
                itemType: "project",
                textQuery,
            };
            const results = (await requestSearchList(payload)).results;
            return results;
        } catch (err) {
            registerError("CreateArtefactModal-getWorkspaceProjects", "Could not fetch project list.", err);
            return [];
        }
    };

    // Handles that an artefact is selected (highlighted) in the artefact selection list (not added, yet    )
    const handleArtefactSelect = (artefact: IPluginOverview) => {
        if (
            toBeAddedKey.current === artefact.key &&
            lastSelectedClick &&
            Date.now() - lastSelectedClick < DOUBLE_CLICK_LIMIT_MS
        ) {
            handleAdd();
        } else {
            setToBeAdded(artefact);
        }
        setLastSelectedClick(Date.now);
    };

    const handleShowEnhancedDescriptionClickHandler = (event, artefactDocumentation: ArtefactDocumentation) => {
        event.preventDefault();
        event.stopPropagation();
        showEnhancedDescription(artefactDocumentation);
    };

    const showEnhancedDescription = React.useCallback((artefactDocumentation: ArtefactDocumentation) => {
        setDocumentationToShow(artefactDocumentation);
    }, []);

    const showDetailedParameterDocumentation = React.useCallback(
        (parameterId: string) => {
            if (selectedArtefact) {
                const artefactDocumentation: ArtefactDocumentation = pluginOverviewToArtefactDocumentation(
                    selectedArtefact,
                    PARAMETER_DOC_PREFIX + parameterId,
                );
                showEnhancedDescription(artefactDocumentation);
            }
        },
        [selectedArtefact],
    );

    const handleEnter = (e) => {
        if (e.key === "Enter" && toBeAdded.current) {
            handleAdd();
        }
    };

    const handleBack = (closeModal?: boolean) => {
        // Ignore back or close actions when (task) documentation is shown, else the create/update dialog will also close.
        if (!documentationIsShown.current) {
            resetModal(closeModal);
        }
    };

    const taskType = React.useCallback(
        (artefactId): TaskType | "Project" => {
            if (artefactId === "project") {
                return "Project";
            } else {
                return (cachedArtefactProperties[artefactId] as IPluginDetails).taskType;
            }
        },
        [cachedArtefactProperties],
    );

    const handleCreate = React.useCallback(
        async (e) => {
            if ((e as KeyboardEvent).key === "Enter") {
                if (e.target.type === "button" && e.target.popoverTargetAction === "toggle") {
                    // This should trigger a toggle, e.g. on an accordion element, do not trigger create.
                    return;
                }
            }
            e.preventDefault();
            setActionLoading(true);
            const isValidFields = await form.triggerValidation();
            try {
                if (isValidFields) {
                    const formValues = form.getValues();
                    const type = updateExistingTask?.taskPluginDetails.taskType ?? taskType(selectedArtefactKey);
                    let dataParameters: any;
                    if (type === "Dataset") {
                        dataParameters = commonOp.extractDataAttributes(formValues);
                    }
                    if (updateExistingTask) {
                        await dispatch(
                            commonOp.fetchUpdateTaskAsync(
                                updateExistingTask.projectId,
                                updateExistingTask.taskId,
                                formValues,
                                dataParameters,
                                templateParameters.current,
                                updateExistingTask?.alternativeUpdateFunction,
                            ),
                        );
                        updateExistingTask.successHandler?.({
                            projectId: updateExistingTask.projectId,
                            taskId: updateExistingTask.taskId,
                        });
                    } else {
                        !projectId && currentProject && dispatch(commonOp.setProjectId(currentProject.id));
                        await dispatch(
                            commonOp.createArtefactAsync(
                                formValues,
                                type,
                                dataParameters,
                                templateParameters.current,
                                newTaskPreConfiguration?.alternativeCallback,
                            ),
                        );
                        setSearchValue("");
                    }
                } else {
                    const errKey = Object.keys(form.errors)[0];
                    const el = document.getElementById(errKey);
                    if (el) {
                        el.scrollIntoView({
                            block: "start",
                            inline: "start",
                        });
                    }
                }
            } catch (error) {
                registerError(
                    "CreateArtefactModal.handleCreate",
                    `Could not ${updateExistingTask ? "update" : "create"} task.`,
                    error,
                );
            } finally {
                setActionLoading(false);
            }
        },
        [form, updateExistingTask, taskType],
    );

    const closeModal = () => {
        setSearchValue("");
        resetModal(true);
        newTaskPreConfiguration?.onCloseCallback?.();
    };

    const isErrorPresented = () => !!Object.keys(form.errors).length;

    const handleSelectDType = (value: string) => {
        dispatch(commonOp.setSelectedArtefactDType(value));
    };

    const resetModal = (closeModal?: boolean) => {
        templateParameters.current = new Set<string>();
        setIsProjectImport(false);
        setToBeAdded(undefined);
        setCurrentProject(undefined);
        setTaskActionResult(undefined);
        form.reset();
        setFormValueChanges({});
        form.clearError();
        dispatch(commonOp.resetArtefactModal(closeModal));
    };

    const switchToProjectImport = () => {
        setIsProjectImport(true);
    };

    /**
     * Tracks changes. When the changed deviate from the initial value it will set a isModified flag.
     */
    const detectFormChange = (key: string, val: any, oldValue: any) => {
        if (formValueChanges[key]) {
            const initialValue = formValueChanges[key].initialValue;
            if (initialValue !== val) {
                formValueChanges[key].isModified = true;
            } else {
                formValueChanges[key].isModified = false;
            }
        } else {
            formValueChanges[key] = { initialValue: oldValue, isModified: true };
        }
    };

    /**
     *
     * Returns true if any of item parameters has been modified. Meta data fields like label and description are excluded.
     * @returns {boolean}
     */
    const modifiedParameterValuesExist = (): boolean => {
        let shouldShow = false;
        for (let field in formValueChanges) {
            if (!ignorableFields.has(field) && formValueChanges[field].isModified && !shouldShow) shouldShow = true;
            else continue;
        }
        return shouldShow;
    };

    // reset to defaults, if label/description already existed they remain.
    const resetFormOnConfirmation = () => {
        const resetValue = Object.create(null);
        Object.keys(formValueChanges).forEach((field) => {
            if (!ignorableFields.has(field)) {
                delete formValueChanges[field];
            } else {
                resetValue[field] = form.getValues()[field];
            }
        });
        form.reset(resetValue);
    };

    /**
     * sets to selected project from ProjectSelection
     * @param item Project
     */
    const updateCurrentSelectedProject = (item: ProjectIdAndLabel) => {
        setShowProjectSelection(false);
        setCurrentProject(item);
    };

    /**
     * Adds Notification Icon and ProjectSelection component to task form.
     * @param artefactForm
     * @returns
     */
    const addChangeProjectHandler = (artefactForm: JSX.Element): JSX.Element => {
        if (
            currentProject &&
            (newTaskPreConfiguration?.showProjectChangeWidget == null ||
                newTaskPreConfiguration?.showProjectChangeWidget)
        )
            return (
                <>
                    {showProjectSelection ? (
                        <ProjectSelection
                            resetForm={resetFormOnConfirmation}
                            setCurrentProject={updateCurrentSelectedProject}
                            modifiedValuesExist={modifiedParameterValuesExist}
                            selectedProject={currentProject}
                            onClose={() => setShowProjectSelection(false)}
                            getWorkspaceProjects={getWorkspaceProjects}
                        />
                    ) : (
                        <Notification
                            message={`${t("CreateModal.projectContext.selectedProject", "Selected project")}: ${
                                currentProject.label
                            }`}
                            actions={
                                <IconButton
                                    data-test-id="project-selection-btn"
                                    name="item-edit"
                                    text={t(
                                        "CreateModal.projectContext.changeProjectButton",
                                        "Select a different project",
                                    )}
                                    onClick={() => setShowProjectSelection(true)}
                                />
                            }
                        />
                    )}
                    <Spacing size="tiny" vertical />
                    {artefactForm}
                </>
            );
        return artefactForm;
    };

    const projectArtefactSelected = selectedArtefactKey === DATA_TYPES.PROJECT;

    let artefactForm: JSX.Element | null = null;

    /** if no current Project context, redirect to project selection first */
    if (selectedArtefactKey && !currentProject) {
        artefactForm = (
            <ProjectSelection
                resetForm={resetFormOnConfirmation}
                modifiedValuesExist={modifiedParameterValuesExist}
                setCurrentProject={updateCurrentSelectedProject}
                selectedProject={currentProject}
                onClose={() => setShowProjectSelection(false)}
                getWorkspaceProjects={getWorkspaceProjects}
            />
        );
    }

    const registerForExternalChanges = React.useCallback(
        (paramId: string, handleUpdates: (value: { value: string; label?: string }) => any) => {
            externalParameterUpdateMap.current.set(paramId, handleUpdates);
        },
        [],
    );

    const propagateExternallyChangedParameterValue = React.useCallback((fullParamId: string, value: string) => {
        externalParameterUpdateMap.current.get(fullParamId)?.({ value });
    }, []);

    if (updateExistingTask) {
        // Task update
        artefactForm = (
            <TaskForm
                form={form}
                detectChange={detectFormChange}
                artefact={updateExistingTask.taskPluginDetails}
                projectId={updateExistingTask.projectId}
                taskId={updateExistingTask.taskId}
                updateTask={{
                    parameterValues: updateExistingTask.currentParameterValues,
                    dataParameters: updateExistingTask.dataParameters,
                    variableTemplateValues: objectToFlatRecord(updateExistingTask.currentTemplateValues, {}, false),
                }}
                parameterCallbacks={{
                    setTemplateFlag,
                    registerForExternalChanges,
                    templateFlag,
                    showDetailedParameterDocumentation,
                }}
                // Close modal immediately from update dialog
                goBackOnEscape={() => handleBack(true)}
                propagateExternallyChangedParameterValue={propagateExternallyChangedParameterValue}
                showWarningMessage={taskFormWarning}
            />
        );
    } else {
        // Project / task creation
        if (selectedArtefactKey) {
            if (projectArtefactSelected) {
                artefactForm = <ProjectForm form={form} goBackOnEscape={handleBack} />;
            } else {
                const detailedArtefact = cachedArtefactProperties[selectedArtefactKey];
                const activeProjectId = currentProject?.id ?? projectId;
                if (detailedArtefact && activeProjectId) {
                    let updatedNewTaskPreConfiguration: TaskPreConfiguration | undefined = {
                        ...newTaskPreConfiguration,
                    };
                    if (newTaskPreConfiguration?.metaDataFactoryFunction) {
                        const generatedMetaData = newTaskPreConfiguration.metaDataFactoryFunction(detailedArtefact);
                        updatedNewTaskPreConfiguration = {
                            ...updatedNewTaskPreConfiguration,
                            metaData: {
                                ...newTaskPreConfiguration?.metaData,
                                ...generatedMetaData,
                            },
                        };
                    }
                    artefactForm = addChangeProjectHandler(
                        <TaskForm
                            detectChange={detectFormChange}
                            form={form}
                            artefact={detailedArtefact}
                            projectId={activeProjectId}
                            parameterCallbacks={{
                                setTemplateFlag,
                                registerForExternalChanges,
                                templateFlag,
                                showDetailedParameterDocumentation,
                            }}
                            goBackOnEscape={handleBack}
                            newTaskPreConfiguration={updatedNewTaskPreConfiguration}
                            propagateExternallyChangedParameterValue={propagateExternallyChangedParameterValue}
                            showWarningMessage={taskFormWarning}
                        />,
                    );
                }
            }
        }
    }

    const showProjectItem = searchValue
        .trim()
        .toLowerCase()
        .split(/\s+/)
        .every((searchWord) => "project".includes(searchWord));

    // Filter artefact list and add project item
    let artefactListWithProject: IPluginOverview[] = artefactsList
        .filter(
            (artefact) =>
                selectedDType === "all" ||
                (artefact.taskType && routerOp.itemTypeToPath(artefact.taskType) === selectedDType),
        )
        .sort((a, b) => a.title!.localeCompare(b.title!));
    const removeProjectCategoryAndItem = newTaskPreConfiguration && !newTaskPreConfiguration.showProjectItem;
    if (showProjectItem && (selectedDType === "all" || selectedDType === "project") && !removeProjectCategoryAndItem) {
        artefactListWithProject = [
            {
                key: DATA_TYPES.PROJECT,
                title: uppercaseFirstChar(t("common.dataTypes.project")),
                description: t(
                    "common.dataTypes.projectDesc",
                    "Projects let you group related items. All items that depend on each other need to be in the same project.",
                ),
            },
            ...artefactListWithProject,
        ];
    }

    // Rank title matches higher
    if (searchValue.trim() !== "") {
        const regex = highlighterUtils.createMultiWordRegex(highlighterUtils.extractSearchWords(searchValue), false);
        const titleMatches: IPluginOverview[] = [];
        const nonTitleMatches: IPluginOverview[] = [];
        artefactListWithProject.forEach((artefactItem) => {
            if (artefactItem.title && regex.test(artefactItem.title)) {
                titleMatches.push(artefactItem);
            } else {
                nonTitleMatches.push(artefactItem);
            }
        });
        titleMatches.sort((a, b) => (a.title!!.length < b.title!!.length ? -1 : 1));
        artefactListWithProject = [...titleMatches, ...nonTitleMatches];
    }

    // If search is active pre-select first item in (final) list
    useEffect(() => {
        setToBeAdded(undefined);
        if (artefactListWithProject.length > 0 && searchValue) {
            setToBeAdded(artefactListWithProject[0]);
        }
    }, [artefactListWithProject.map((item) => item.key).join("|"), selectedDType]);

    const handleAutoConfigure = async (projectId: string, artefactId: string) => {
        const isValidFields = await form.triggerValidation();
        if (!isValidFields) {
            return;
        }
        try {
            setAutoConfigPending(true);
            resetModalError();
            const { parameters, variableTemplateParameters } = commonOp.splitParameterAndVariableTemplateParameters(
                form.getValues(),
                templateParameters.current,
            );
            const parameterData = commonOp.buildNestedTaskParameterObject(parameters);
            const variableTemplateData = commonOp.buildNestedTaskParameterObject(variableTemplateParameters);
            const requestBody: DatasetTaskPlugin<any> = {
                taskType: taskType(artefactId) as TaskType,
                type: artefactId,
                parameters: parameterData,
                templates: variableTemplateData as Record<string, string>,
            };
            const parameterChanges: Record<string, string> = (
                await requestAutoConfiguredDataset(projectId, requestBody)
            ).data.parameters;
            const formValues = form.getValues();
            let valuesUpdated = 0;
            Object.entries(parameterChanges).forEach(([paramId, value]) => {
                if (
                    formValues[paramId] != null &&
                    `${formValues[paramId]}` !== value &&
                    externalParameterUpdateMap.current.has(paramId)
                ) {
                    externalParameterUpdateMap.current.get(paramId)!({ value });
                    valuesUpdated += 1;
                }
            });
            setInfoMessage({
                message: t("CreateModal.autoConfigParametersUpdated", { number: valuesUpdated }),
                removeAfterSeconds: 5,
            });
        } catch (ex) {
            registerError("CreateArtefactModal.handleAutConfigure", "Auto-configuration has failed.", ex);
        } finally {
            setAutoConfigPending(false);
        }
    };

    const isCreationUpdateDialog = selectedArtefactKey || updateExistingTask;
    const additionalButtons: JSX.Element[] = [];
    if (
        (projectId || currentProject) &&
        ((updateExistingTask && updateExistingTask.taskPluginDetails.autoConfigurable) ||
            (selectedArtefactKey && cachedArtefactProperties[selectedArtefactKey]?.autoConfigurable))
    ) {
        additionalButtons.push(
            <Button
                outlined
                data-test-id={"autoConfigureItem-btn"}
                key="autoConfig"
                tooltip={t("CreateModal.autoConfigTooltip")}
                onClick={() =>
                    handleAutoConfigure(
                        projectId ?? currentProject!.id,
                        selectedArtefactKey ?? updateExistingTask!.taskPluginDetails.pluginId,
                    )
                }
                loading={autoConfigPending}
            >
                {t("CreateModal.autoConfigButton")}
            </Button>,
        );
    }

    const pluginActions: JSX.Element[] = [];
    if (selectedArtefact?.actions) {
        const describedActions = Object.entries(selectedArtefact.actions);
        pluginActions.push(
            ...describedActions.map(([actionKey, action]) => {
                const executeAction = async () => {
                    try {
                        setTaskActionLoading(actionKey);
                        const project = updateExistingTask?.projectId || currentProject?.id || projectId;
                        if (!project) return;
                        const formValues = form.getValues();
                        const { parameters, variableTemplateParameters } =
                            commonOp.splitParameterAndVariableTemplateParameters(
                                formValues,
                                templateParameters.current,
                            );
                        const parameterData = commonOp.buildNestedTaskParameterObject(parameters);
                        const variableTemplateData =
                            commonOp.buildNestedTaskParameterObject(variableTemplateParameters);
                        const result = await performAction({
                            projectId: project,
                            taskId: updateExistingTask?.taskId ?? "task",
                            actionKey,
                            taskPayload: {
                                taskType: (updateExistingTask?.taskPluginDetails.taskType ??
                                    taskType(selectedArtefactKey)) as TaskType,
                                type: selectedArtefact.key,
                                parameters: {
                                    ...parameterData,
                                },
                                templates: {
                                    ...variableTemplateData,
                                },
                            },
                        });
                        resetModalError();
                        setTaskActionResult({ label: action.label, message: result.data.message });
                    } catch (err) {
                        setTaskActionResult(undefined);
                        registerError("CreateArtefactModal.action", `Could not perform action '${action.label}'.`, err);
                    } finally {
                        setTaskActionLoading(null);
                    }
                };
                return describedActions.length > MAX_SINGLEPLUGINBUTTONS ? (
                    <MenuItem
                        text={action.label}
                        tooltip={action.description}
                        data-test-id={`${actionKey}-btn`}
                        key={actionKey}
                        onClick={executeAction}
                        icon={
                            action.icon ? (
                                <Depiction image={<img src={action.icon} />} forceInlineSvg ratio="1:1" size="tiny" />
                            ) : undefined
                        }
                    />
                ) : (
                    <Button
                        outlined
                        fill
                        ellipsizeText
                        disabled={!!taskActionLoading}
                        data-test-id={`${actionKey}-btn`}
                        key={actionKey}
                        onClick={executeAction}
                        tooltip={action.description}
                        icon={
                            taskActionLoading == actionKey ? (
                                <Spinner size="tiny" />
                            ) : action.icon ? (
                                <Depiction image={<img src={action.icon} />} forceInlineSvg ratio="1:1" size="tiny" />
                            ) : undefined
                        }
                    >
                        {action.label}
                    </Button>
                );
            }),
        );
    }

    const pluginActionsMenu =
        pluginActions.length > MAX_SINGLEPLUGINBUTTONS
            ? [
                  <ContextMenu
                      key="menu-pluginactions"
                      disabled={!!taskActionLoading}
                      togglerElement={
                          <Button
                              outlined
                              disabled={!!taskActionLoading}
                              text={t("CreateModal.pluginActions")}
                              rightIcon={taskActionLoading ? <Spinner size="tiny" /> : "toggler-caretdown"}
                          />
                      }
                  >
                      {pluginActions}
                  </ContextMenu>,
              ]
            : pluginActions;

    const headerOptions: JSX.Element[] = [];
    if (selectedArtefactTitle && (selectedArtefact?.markdownDocumentation || selectedArtefact?.description)) {
        headerOptions.push(
            <IconButton
                key={"show-enhanced-description-btn"}
                name="item-question"
                onClick={(e) =>
                    handleShowEnhancedDescriptionClickHandler(
                        e,
                        pluginOverviewToArtefactDocumentation(selectedArtefact),
                    )
                }
            />,
        );
    }

    const updateModalTitle = (updateData: IProjectTaskUpdatePayload) => updateData.metaData.label ?? updateData.taskId;
    const notifications: JSX.Element[] = [];

    if (!!error.detail || !!error.errorMessage || !!error.body?.taskLoadingError?.errorMessage || error.isFetchError) {
        // Special case for fix task loading error
        const taskLoadingError = error.body?.taskLoadingError?.errorMessage ? (
            <div
                data-test-id={"action-error-notification"}
            >{`${error.body.detail} ${error.body?.taskLoadingError?.errorMessage}`}</div>
        ) : undefined;
        const actionFailed = () => {
            const actionValue = updateExistingTask ? t("common.action.update") : t("common.action.create");
            const errorMessage = (diErrorMessage(error) ?? "Unknown error").replace(/^(assertion failed: )/, "");
            return t("common.messages.actionFailed", {
                action: actionValue,
                error: errorMessage,
            });
        };

        notifications.push(
            <Notification
                onDismiss={resetModalError}
                message={taskLoadingError || error.errorMessage || actionFailed()}
                intent="danger"
            />,
        );
    }

    if (info) {
        notifications.push(<Notification onDismiss={resetModalInfo} message={info} timeout={30000} />);
    }

    if (infoMessage) {
        notifications.push(<Notification message={infoMessage.message} />);
    }

    if (projectArtefactSelected) {
        notifications.push(
            <Notification
                message={t("ProjectImportModal.restoreNotice", "Want to restore an existing project?")}
                actions={[
                    <Button
                        data-test-id="project-import-link"
                        key="importProject"
                        onClick={switchToProjectImport}
                        href="#import-project"
                    >
                        {t("ProjectImportModal.restoreStarter", "Import project file")}
                    </Button>,
                ]}
            />,
        );
    }

    if(taskFormGeneralWarning) {
        notifications.push(
            <Notification
                message={taskFormGeneralWarning}
                onDismiss={() => setTaskFormGeneralWarning(undefined)}
            />,
        );
    }

    const submitEnabled = !!isCreationUpdateDialog && !isErrorPresented();
    useHotKey({ hotkey: "enter", handler: handleCreate, enabled: submitEnabled });

    if (taskActionResult) {
        notifications.push(
            <Notification
                intent="success"
                message={
                    <Accordion whitespaceSize={"none"}>
                        <AccordionItem
                            open={(taskActionResult?.message ?? "").length < 500}
                            label={taskActionResult?.label}
                            noBorder
                            fullWidth
                        >
                            <Spacing size="small" />
                            <Markdown
                                htmlContentBlockProps={{
                                    style: {
                                        maxHeight: "25vh",
                                        overflow: "auto",
                                    },
                                }}
                            >
                                {taskActionResult?.message ?? ""}
                            </Markdown>
                        </AccordionItem>
                    </Accordion>
                }
                actions={[
                    <IconButton
                        key="cancel"
                        name="navigation-close"
                        text={t("common.action.close")}
                        onClick={() => {
                            setTaskActionResult(undefined);
                        }}
                    />,
                ]}
            />,
        );
    }

    const createDialog = (
        <SimpleDialog
            size="large"
            preventSimpleClosing={!!artefactForm || searchValue.trim().length > 0}
            hasBorder
            title={
                updateExistingTask
                    ? updateExistingTask.alternativeTitle
                        ? updateExistingTask.alternativeTitle
                        : t("CreateModal.updateTitle", {
                              type: `'${updateModalTitle(updateExistingTask)}' (${
                                  updateExistingTask.taskPluginDetails.title
                              })`,
                          })
                    : selectedArtefactTitle
                      ? t("CreateModal.createTitle", { type: selectedArtefactTitle })
                      : t("CreateModal.createTitleGeneric")
            }
            headerOptions={headerOptions}
            onClose={closeModal}
            isOpen={isOpen}
            actions={
                isCreationUpdateDialog ? (
                    actionLoading ? (
                        <Loading size={"small"} color={"primary"} delay={0} />
                    ) : (
                        [
                            <Button
                                data-test-id={"createArtefactButton"}
                                key="create"
                                affirmative={true}
                                onClick={handleCreate}
                                disabled={isErrorPresented()}
                            >
                                {updateExistingTask ? t("common.action.update") : t("common.action.create")}
                            </Button>,
                            <Button key="cancel" data-test-id="create-dialog-cancel-btn" onClick={closeModal}>
                                {t("common.action.cancel")}
                            </Button>,
                            <CardActionsAux key="aux">
                                {!updateExistingTask && (
                                    <>
                                        <Button
                                            data-test-id={"create-dialog-back-btn"}
                                            key="back"
                                            onClick={() => handleBack(false)}
                                        >
                                            {t("common.words.back", "Back")}
                                        </Button>
                                        {additionalButtons.length + pluginActions.length > 0 && (
                                            <Spacing vertical hasDivider />
                                        )}
                                    </>
                                )}
                                {additionalButtons}
                                {pluginActionsMenu}
                            </CardActionsAux>,
                        ]
                    )
                ) : (
                    [
                        <Button
                            key="add"
                            affirmative={true}
                            onClick={handleAdd}
                            disabled={!toBeAddedKey.current}
                            data-test-id={"item-add-btn"}
                        >
                            {t("common.action.add")}
                        </Button>,
                        <Button data-test-id="create-dialog-cancel-btn" key="cancel" onClick={closeModal}>
                            {t("common.action.cancel")}
                        </Button>,
                        <CardActionsAux key="aux">
                            {additionalButtons}
                            {pluginActionsMenu}
                        </CardActionsAux>,
                    ]
                )
            }
            actionsProps={{ noWrap: true }}
            notifications={
                notifications.length > 0
                    ? notifications.map((notification, idx) => (
                          <>
                              {notification}
                              {idx < notifications.length - 1 && <Spacing size="small" />}
                          </>
                      ))
                    : undefined
            }
        >
            {
                <>
                    {artefactForm ? (
                        <>{artefactForm}</>
                    ) : (
                        <Grid>
                            <GridRow>
                                <GridColumn small>
                                    <ArtefactTypesList
                                        onSelect={handleSelectDType}
                                        typesToRemove={removeProjectCategoryAndItem ? new Set(["project"]) : new Set()}
                                    />
                                </GridColumn>
                                <GridColumn>
                                    <SearchBar
                                        textQuery={searchValue}
                                        focusOnCreation={true}
                                        onSearch={handleSearch}
                                        onEnter={handleAdd}
                                        disableEnterDuringPendingSearch={true}
                                    />
                                    <Spacing />
                                    {loading ? (
                                        <Loading
                                            description={t("CreateModal.loading", "Loading artefact type list.")}
                                            delay={0}
                                        />
                                    ) : artefactListWithProject.length === 0 ? (
                                        <Notification message={t("CreateModal.noMatch", "No match found.")} />
                                    ) : (
                                        <OverviewItemList
                                            data-test-id="item-to-create-selection-list"
                                            hasSpacing
                                            columns={2}
                                        >
                                            {artefactListWithProject.map((artefact) => {
                                                const description =
                                                    artefact.markdownDocumentation || artefact.description || "";
                                                return (
                                                    <Card
                                                        isOnlyLayout
                                                        key={artefact.key}
                                                        className={
                                                            toBeAddedKey.current === artefact.key
                                                                ? ClassNames.Intent.ACCENT
                                                                : ""
                                                        }
                                                    >
                                                        <OverviewItem
                                                            hasSpacing
                                                            data-test-id={`artefact-plugin-${artefact.key}`}
                                                            onClick={() => handleArtefactSelect(artefact)}
                                                            onKeyDown={handleEnter}
                                                        >
                                                            <OverviewItemDepiction>
                                                                <ItemDepiction
                                                                    itemType={artefact.taskType}
                                                                    pluginId={artefact.key}
                                                                />
                                                            </OverviewItemDepiction>
                                                            <OverviewItemDescription>
                                                                <OverviewItemLine>
                                                                    <strong>
                                                                        <Highlighter
                                                                            label={artefact.title}
                                                                            searchValue={searchValue}
                                                                        />
                                                                    </strong>
                                                                </OverviewItemLine>
                                                                <OverviewItemLine small>
                                                                    <OverflowText useHtmlElement="p">
                                                                        <Highlighter
                                                                            label={artefact.description}
                                                                            searchValue={searchValue}
                                                                        />
                                                                    </OverflowText>
                                                                </OverviewItemLine>
                                                            </OverviewItemDescription>
                                                            {description ? (
                                                                <OverviewItemActions>
                                                                    <IconButton
                                                                        name="item-question"
                                                                        onClick={(e) => {
                                                                            handleShowEnhancedDescriptionClickHandler(
                                                                                e,
                                                                                pluginOverviewToArtefactDocumentation(
                                                                                    artefact,
                                                                                ),
                                                                            );
                                                                        }}
                                                                    />
                                                                </OverviewItemActions>
                                                            ) : null}
                                                        </OverviewItem>
                                                    </Card>
                                                );
                                            })}
                                        </OverviewItemList>
                                    )}
                                </GridColumn>
                            </GridRow>
                        </Grid>
                    )}
                    {documentationToShow && (
                        <TaskDocumentationModal
                            documentationToShow={documentationToShow}
                            onClose={() => setDocumentationToShow(undefined)}
                        />
                    )}
                </>
            }
        </SimpleDialog>
    );
    return isProjectImport ? (
        <ProjectImportModal
            close={closeModal}
            back={() => setIsProjectImport(false)}
            maxFileUploadSizeBytes={maxFileUploadSize}
        />
    ) : (
        <CreateArtefactModalContext.Provider
            value={{
                registerModalError: registerError,
            }}
        >
            {isOpen ? createDialog : null}
        </CreateArtefactModalContext.Provider>
    );
}

const pluginOverviewToArtefactDocumentation = (
    pluginOverview: IPluginOverview,
    namedAnchor?: string,
): ArtefactDocumentation => {
    return {
        key: pluginOverview.key,
        title: pluginOverview.title,
        description: pluginOverview.description,
        markdownDocumentation: pluginOverview.markdownDocumentation,
        namedAnchor,
    };
};
