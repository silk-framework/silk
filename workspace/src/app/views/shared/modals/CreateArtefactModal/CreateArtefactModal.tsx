import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useForm } from "react-hook-form";
import {
    Button,
    Card,
    CardActionsAux,
    Grid,
    GridColumn,
    GridRow,
    HelperClasses,
    Highlighter,
    HtmlContentBlock,
    IconButton,
    Markdown,
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
} from "@eccenca/gui-elements";
import { createMultiWordRegex, extractSearchWords } from "@eccenca/gui-elements/src/components/Typography/Highlighter";
import { commonOp, commonSel } from "@ducks/common";
import { IArtefactModal, IPluginDetails, IPluginOverview } from "@ducks/common/typings";
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
import { uppercaseFirstChar } from "../../../../utils/transformers";
import { requestProjectMetadata } from "@ducks/shared/requests";
import { requestAutoConfiguredDataset } from "./CreateArtefactModal.requests";
import { diErrorMessage } from "@ducks/error/typings";

const ignorableFields = new Set(["label", "description"]);

export interface ProjectIdAndLabel {
    id: string;
    label: string;
}

export interface InfoMessage {
    message: string;
    removeAfterSeconds?: number;
}

export function CreateArtefactModal() {
    const dispatch = useDispatch();
    const form = useForm();

    const [searchValue, setSearchValue] = useState("");
    const [idEnhancedDescription, setIdEnhancedDescription] = useState("");
    const [actionLoading, setActionLoading] = useState(false);
    const [t] = useTranslation();

    const { maxFileUploadSize } = useSelector(commonSel.initialSettingsSelector);
    const modalStore = useSelector(commonSel.artefactModalSelector);

    const {
        selectedArtefact,
        isOpen,
        artefactsList,
        cachedArtefactProperties,
        selectedDType,
        loading,
        updateExistingTask,
        error,
    }: IArtefactModal = modalStore;

    // The artefact that is selected from the artefact selection list. This can be pre-selected via the Redux state.
    // A successive 'Add' action will open the creation dialog for this artefact.
    const [toBeAdded, setToBeAdded] = useState<IPluginOverview | undefined>(selectedArtefact);
    const [lastSelectedClick, setLastSelectedClick] = useState<number>(0);
    const [isProjectImport, setIsProjectImport] = useState<boolean>(false);
    const [autoConfigPending, setAutoConfigPending] = useState(false);
    const DOUBLE_CLICK_LIMIT_MS = 500;

    const selectedArtefactKey: string | undefined = selectedArtefact?.key;
    const selectedArtefactTitle: string | undefined = selectedArtefact?.title;

    const toBeAddedKey: string | undefined = toBeAdded?.key;
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
        new Map<string, (value: { value: string; label?: string }) => any>()
    );
    const NOTIFICATION_ID = "create-update-dialog";

    React.useEffect(() => {
        if (infoMessage?.removeAfterSeconds && infoMessage.removeAfterSeconds > 0) {
            const timeoutId = setTimeout(() => {
                setInfoMessage(undefined);
            }, infoMessage.removeAfterSeconds * 1000);
            return () => clearTimeout(timeoutId);
        }
    }, [infoMessage]);

    const registerError = (errorId: string, errorMessage: string, error: any, notificationId: string) => {
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
    };

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
                        NOTIFICATION_ID
                    );
                }
            })();
        }
    }, [projectId, selectedArtefactKey, isOpen]);

    useEffect(() => {
        if (!isOpen) {
            // Reset modal when it was closed
            resetModal(true);
        }
    }, [isOpen]);

    // Fetch Artefact list
    useEffect(() => {
        if (isOpen && !isEmptyWorkspace) {
            dispatch(
                commonOp.fetchArtefactsListAsync({
                    textQuery: searchValue,
                })
            );
        } else {
            dispatch(commonOp.resetArtefactsList());
        }
    }, [isOpen]);

    const handleAdd = () => {
        if (toBeAddedKey === DATA_TYPES.PROJECT) {
            return dispatch(commonOp.selectArtefact(toBeAdded));
        } else if (toBeAdded) {
            dispatch(commonOp.getArtefactPropertiesAsync(toBeAdded));
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
                })
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
            registerError(
                "CreateArtefactModal-getWorkspaceProjects",
                "Could not fetch project list.",
                err,
                NOTIFICATION_ID
            );
            return [];
        }
    };

    // Handles that an artefact is selected (highlighted) in the artefact selection list (not added, yet    )
    const handleArtefactSelect = (artefact: IPluginOverview) => {
        if (
            toBeAddedKey === artefact.key &&
            lastSelectedClick &&
            Date.now() - lastSelectedClick < DOUBLE_CLICK_LIMIT_MS
        ) {
            handleAdd();
        } else {
            setToBeAdded(artefact);
        }
        setLastSelectedClick(Date.now);
    };

    const handleShowEnhancedDescription = (event, artefactId) => {
        event.preventDefault();
        event.stopPropagation();
        setIdEnhancedDescription(artefactId);
    };

    const handleEnter = (e) => {
        if (e.key === "Enter" && toBeAdded) {
            handleAdd();
        }
    };

    const handleBack = () => {
        resetModal();
    };

    const taskType = (artefactId): TaskType | "Project" => {
        if (artefactId === "project") {
            return "Project";
        } else {
            return (cachedArtefactProperties[artefactId] as IPluginDetails).taskType;
        }
    };

    const handleCreate = async (e) => {
        e.preventDefault();
        setActionLoading(true);
        const isValidFields = await form.triggerValidation();
        try {
            if (isValidFields) {
                const formValues = form.getValues()
                const type = updateExistingTask?.taskPluginDetails.taskType ?? taskType(selectedArtefactKey)
                let dataParameters: any
                if(type === "Dataset") {
                    dataParameters = commonOp.extractDataAttributes(formValues)
                }
                if (updateExistingTask) {
                    await dispatch(
                        commonOp.fetchUpdateTaskAsync(
                            updateExistingTask.projectId,
                            updateExistingTask.taskId,
                            formValues,
                            dataParameters
                        )
                    );
                } else {
                    !projectId && currentProject && dispatch(commonOp.setProjectId(currentProject.id));
                    await dispatch(commonOp.createArtefactAsync(formValues, type, dataParameters));
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
        } finally {
            setSearchValue("");
            setActionLoading(false);
        }
    };

    const closeModal = () => {
        setSearchValue("");
        resetModal(true);
    };

    const isErrorPresented = () => !!Object.keys(form.errors).length;

    const handleSelectDType = (value: string) => {
        dispatch(commonOp.setSelectedArtefactDType(value));
    };

    const resetModal = (closeModal?: boolean) => {
        setIsProjectImport(false);
        setToBeAdded(undefined);
        setCurrentProject(undefined);
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
        const resetValue = {};
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
        if (currentProject)
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
                                        "Select a different project"
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

    const registerForExternalChanges = (
        paramId: string,
        handleUpdates: (value: { value: string; label?: string }) => any
    ) => {
        externalParameterUpdateMap.current.set(paramId, handleUpdates);
    };

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
                    dataParameters: updateExistingTask.dataParameters
                }}
                registerForExternalChanges={registerForExternalChanges}
            />
        );
    } else {
        // Project / task creation
        if (selectedArtefactKey) {
            if (projectArtefactSelected) {
                artefactForm = <ProjectForm form={form} />;
            } else {
                const detailedArtefact = cachedArtefactProperties[selectedArtefactKey];
                const activeProjectId = currentProject?.id ?? projectId;
                if (detailedArtefact && activeProjectId) {
                    artefactForm = addChangeProjectHandler(
                        <TaskForm
                            detectChange={detectFormChange}
                            form={form}
                            artefact={detailedArtefact}
                            projectId={activeProjectId}
                            registerForExternalChanges={registerForExternalChanges}
                        />
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
    let artefactListWithProject = artefactsList
        .filter(
            (artefact) =>
                selectedDType === "all" ||
                (artefact.taskType && routerOp.itemTypeToPath(artefact.taskType) === selectedDType)
        )
        .sort((a, b) => a.title!.localeCompare(b.title!));
    if (showProjectItem && (selectedDType === "all" || selectedDType === "project")) {
        artefactListWithProject = [
            {
                key: DATA_TYPES.PROJECT,
                title: uppercaseFirstChar(t("common.dataTypes.project")),
                description: t(
                    "common.dataTypes.projectDesc",
                    "Projects let you group related items. All items that depend on each other need to be in the same project."
                ),
                taskType: "project",
            },
            ...artefactListWithProject,
        ];
    }

    // Rank title matches higher
    if (searchValue.trim() !== "") {
        const regex = createMultiWordRegex(extractSearchWords(searchValue), false);
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
            const parameters = commonOp.buildTaskObject(form.getValues());
            const requestBody: DatasetTaskPlugin<any> = {
                taskType: taskType(artefactId) as TaskType,
                type: artefactId,
                parameters,
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
            registerError(
                "CreateArtefactModal.handleAutConfigure",
                "Auto-configuration has failed.",
                ex,
                NOTIFICATION_ID
            );
        } finally {
            setAutoConfigPending(false);
        }
    };

    const isCreationUpdateDialog = selectedArtefactKey || updateExistingTask;
    const additionalButtons: JSX.Element[] = [];
    if (
        (projectId  || currentProject) &&
        ((updateExistingTask && updateExistingTask.taskPluginDetails.autoConfigurable) ||
            (selectedArtefactKey && cachedArtefactProperties[selectedArtefactKey]?.autoConfigurable))
    ) {
        additionalButtons.push(
            <Button
                data-test-id={"autoConfigureItem-btn"}
                key="autoConfig"
                tooltip={t("CreateModal.autoConfigTooltip")}
                onClick={() =>
                    handleAutoConfigure(
                        projectId ?? currentProject!.id,
                        selectedArtefactKey ?? updateExistingTask!.taskPluginDetails.pluginId
                    )
                }
                loading={autoConfigPending}
            >
                {t("CreateModal.autoConfigButton")}
            </Button>
        );
    }

    const headerOptions: JSX.Element[] = [];
    if (selectedArtefactTitle && (selectedArtefact?.markdownDocumentation || selectedArtefact?.description)) {
        headerOptions.push(
            <IconButton name="item-question" onClick={(e) => handleShowEnhancedDescription(e, selectedArtefact.key)} />
        );
    }

    const createDialog = (
        <SimpleDialog
            size="large"
            preventSimpleClosing={true}
            hasBorder
            title={
                updateExistingTask
                    ? t("CreateModal.updateTitle", {
                          type: `'${updateExistingTask.metaData.label}' (${updateExistingTask.taskPluginDetails.title})`,
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
                            <Button key="cancel" onClick={closeModal}>
                                {t("common.action.cancel")}
                            </Button>,
                            ...additionalButtons,
                            <CardActionsAux key="aux">
                                {!updateExistingTask && (
                                    <Button data-test-id={"create-dialog-back-btn"} key="back" onClick={handleBack}>
                                        {t("common.words.back", "Back")}
                                    </Button>
                                )}
                            </CardActionsAux>,
                        ]
                    )
                ) : (
                    [
                        <Button
                            key="add"
                            affirmative={true}
                            onClick={handleAdd}
                            disabled={!toBeAddedKey}
                            data-test-id={"item-add-btn"}
                        >
                            {t("common.action.add")}
                        </Button>,
                        <Button data-test-id="create-dialog-cancel-btn" key="cancel" onClick={closeModal}>
                            {t("common.action.cancel")}
                        </Button>,
                        ...additionalButtons,
                    ]
                )
            }
            notifications={
                ((!!error.detail || !!error.errorMessage) && (
                    <Notification
                        message={
                            error.errorMessage ||
                            t("common.messages.actionFailed", {
                                action: updateExistingTask ? t("common.action.update") : t("common.action.create"),
                                error: error.detail.replace(/^(assertion failed: )/, ""),
                            })
                        }
                        danger
                    />
                )) ||
                (infoMessage && <Notification message={infoMessage.message} />) ||
                (projectArtefactSelected && (
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
                    />
                ))
            }
        >
            {idEnhancedDescription === selectedArtefactKey && (
                <SimpleDialog
                    isOpen
                    title={selectedArtefactTitle}
                    actions={
                        <Button
                            text="Close"
                            onClick={() => {
                                setIdEnhancedDescription("");
                            }}
                        />
                    }
                    size="small"
                >
                    <HtmlContentBlock>
                        <Markdown allowHtml>
                            {selectedArtefact?.markdownDocumentation || selectedArtefact?.description || ""}
                        </Markdown>
                    </HtmlContentBlock>
                </SimpleDialog>
            )}
            {
                <>
                    {artefactForm ? (
                        <>{artefactForm}</>
                    ) : (
                        <Grid>
                            <GridRow>
                                <GridColumn small>
                                    <ArtefactTypesList onSelect={handleSelectDType} />
                                </GridColumn>
                                <GridColumn>
                                    <SearchBar textQuery={searchValue} focusOnCreation={true} onSearch={handleSearch} />
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
                                                            toBeAddedKey === artefact.key
                                                                ? HelperClasses.Intent.ACCENT
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
                                                                            handleShowEnhancedDescription(
                                                                                e,
                                                                                artefact.key
                                                                            );
                                                                        }}
                                                                    />
                                                                </OverviewItemActions>
                                                            ) : null}
                                                        </OverviewItem>
                                                        {idEnhancedDescription === artefact.key && (
                                                            <SimpleDialog
                                                                isOpen
                                                                title={artefact.title}
                                                                actions={
                                                                    <Button
                                                                        text="Close"
                                                                        onClick={() => {
                                                                            setIdEnhancedDescription("");
                                                                        }}
                                                                    />
                                                                }
                                                                size="small"
                                                            >
                                                                <HtmlContentBlock>
                                                                    <Markdown allowHtml>{description}</Markdown>
                                                                </HtmlContentBlock>
                                                            </SimpleDialog>
                                                        )}
                                                    </Card>
                                                );
                                            })}
                                        </OverviewItemList>
                                    )}
                                </GridColumn>
                            </GridRow>
                        </Grid>
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
        createDialog
    );
}
