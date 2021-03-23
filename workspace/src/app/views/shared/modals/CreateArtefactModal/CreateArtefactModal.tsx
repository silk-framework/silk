import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useForm } from "react-hook-form";
import ReactMarkdown from "react-markdown";
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
} from "@gui-elements/index";
import { createMultiWordRegex, extractSearchWords } from "@gui-elements/src/components/Typography/Highlighter";
import { commonOp, commonSel } from "@ducks/common";
import { IArtefactItem, IArtefactModal, IDetailedArtefactItem } from "@ducks/common/typings";
import Loading from "../../Loading";
import { ProjectForm } from "./ArtefactForms/ProjectForm";
import { TaskForm } from "./ArtefactForms/TaskForm";
import { DATA_TYPES } from "../../../../constants";
import ArtefactTypesList from "./ArtefactTypesList";
import { SearchBar } from "../../SearchBar/SearchBar";
import { routerOp } from "@ducks/router";
import { useTranslation } from "react-i18next";
import { TaskType } from "@ducks/shared/typings";
import { ProjectImportModal } from "../ProjectImportModal";
import ItemDepiction from "../../../shared/ItemDepiction";
import { ErrorBoundary } from "carbon-components-react/lib/components/ErrorBoundary";

export function CreateArtefactModal() {
    const dispatch = useDispatch();
    const form = useForm();

    const [searchValue, setSearchValue] = useState("");
    const [idEnhancedDescription, setIdEnhancedDescription] = useState("");
    const [actionLoading, setActionLoading] = useState(false);
    const [t] = useTranslation();

    const modalStore = useSelector(commonSel.artefactModalSelector);
    const projectId = useSelector(commonSel.currentProjectIdSelector);

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
    // A successive 'Add' action will open die creation dialog for this artefact.
    const [toBeAdded, setToBeAdded] = useState<IArtefactItem | undefined>(selectedArtefact);
    const [lastSelectedClick, setLastSelectedClick] = useState<number>(0);
    const [isProjectImport, setIsProjectImport] = useState<boolean>(false);
    const DOUBLE_CLICK_LIMIT_MS = 500;

    const selectedArtefactKey: string | undefined = selectedArtefact?.key;
    const selectedArtefactTitle: string | undefined = selectedArtefact?.title;

    const toBeAddedKey: string | undefined = toBeAdded?.key;

    // Fetch Artefact list
    useEffect(() => {
        if (projectId && isOpen) {
            dispatch(
                commonOp.fetchArtefactsListAsync({
                    textQuery: searchValue,
                })
            );
        } else {
            dispatch(commonOp.resetArtefactsList());
        }
    }, [!!projectId, isOpen]);

    const handleAdd = () => {
        if (toBeAddedKey === DATA_TYPES.PROJECT) {
            return dispatch(commonOp.selectArtefact(toBeAdded));
        }
        dispatch(commonOp.getArtefactPropertiesAsync(toBeAdded));
    };

    const handleSearch = (textQuery: string) => {
        setSearchValue(textQuery);
        if (projectId) {
            dispatch(
                commonOp.fetchArtefactsListAsync({
                    textQuery,
                })
            );
        }
    };

    // Handles that an artefact is selected (highlighted) in the artefact selection list (not added, yet    )
    const handleArtefactSelect = (artefact: IArtefactItem) => {
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
            return (cachedArtefactProperties[artefactId] as IDetailedArtefactItem).taskType;
        }
    };

    const handleCreate = async (e) => {
        e.preventDefault();
        setActionLoading(true);
        const isValidFields = await form.triggerValidation();
        try {
            if (isValidFields) {
                if (updateExistingTask) {
                    await dispatch(
                        commonOp.fetchUpdateTaskAsync(
                            updateExistingTask.projectId,
                            updateExistingTask.taskId,
                            form.getValues()
                        )
                    );
                } else {
                    await dispatch(commonOp.createArtefactAsync(form.getValues(), taskType(selectedArtefactKey)));
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
        form.clearError();
        dispatch(commonOp.resetArtefactModal(closeModal));
    };

    const switchToProjectImport = () => {
        setIsProjectImport(true);
    };

    const projectArtefactSelected = selectedArtefactKey === DATA_TYPES.PROJECT;

    let artefactForm = null;
    if (updateExistingTask) {
        // Task update
        artefactForm = (
            <TaskForm
                form={form}
                artefact={updateExistingTask.taskPluginDetails}
                projectId={updateExistingTask.projectId}
                updateTask={{ parameterValues: updateExistingTask.currentParameterValues }}
            />
        );
    } else {
        // Project / task creation
        if (selectedArtefactKey) {
            if (projectArtefactSelected) {
                artefactForm = <ProjectForm form={form} projectId={projectId} />;
            } else {
                const detailedArtefact = cachedArtefactProperties[selectedArtefactKey];
                if (detailedArtefact && projectId) {
                    artefactForm = <TaskForm form={form} artefact={detailedArtefact} projectId={projectId} />;
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
    let artefactListWithProject = artefactsList.filter(
        (artefact) => selectedDType === "all" || routerOp.itemTypeToPath(artefact.taskType) === selectedDType
    );
    if (showProjectItem && (selectedDType === "all" || selectedDType === "project")) {
        artefactListWithProject = [
            {
                key: DATA_TYPES.PROJECT,
                title: t("common.dataTypes.project"),
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
        const regex = createMultiWordRegex(extractSearchWords(searchValue));
        const titleMatches = [];
        const nonTitleMatches = [];
        artefactListWithProject.forEach((artefactItem) => {
            if (regex.test(artefactItem.title)) {
                titleMatches.push(artefactItem);
            } else {
                nonTitleMatches.push(artefactItem);
            }
        });
        artefactListWithProject = [...titleMatches, ...nonTitleMatches];
    }

    // If search is active pre-select first item in (final) list
    useEffect(() => {
        setToBeAdded(undefined);
        if (artefactListWithProject.length > 0 && searchValue) {
            setToBeAdded(artefactListWithProject[0]);
        }
    }, [artefactListWithProject.map((item) => item.key).join("|"), selectedDType]);

    const isCreationUpdateDialog = selectedArtefactKey || updateExistingTask;

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
            onClose={closeModal}
            isOpen={isOpen}
            actions={
                isCreationUpdateDialog ? (
                    actionLoading ? (
                        <Loading size={"small"} color={"primary"} />
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
                    ]
                )
            }
            notifications={
                (!!error.detail && (
                    <Notification
                        message={t("common.messages.actionFailed", {
                            action: updateExistingTask ? t("common.action.update") : t("common.action.create"),
                            error: error.detail.replace(/^(assertion failed: )/, ""),
                        })}
                        danger
                    />
                )) ||
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
                                        />
                                    ) : artefactListWithProject.length === 0 ? (
                                        <Notification message={t("CreateModal.noMatch", "No match found.")} />
                                    ) : (
                                        <OverviewItemList
                                            data-test-id="item-to-create-selection-list"
                                            hasSpacing
                                            columns={2}
                                        >
                                            {artefactListWithProject.map((artefact) => (
                                                <Card
                                                    isOnlyLayout
                                                    key={artefact.key}
                                                    className={
                                                        toBeAddedKey === artefact.key ? HelperClasses.Intent.ACCENT : ""
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
                                                        <OverviewItemActions>
                                                            <IconButton
                                                                name="item-info"
                                                                onClick={(e) => {
                                                                    handleShowEnhancedDescription(e, artefact.key);
                                                                }}
                                                            />
                                                        </OverviewItemActions>
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
                                                                <ReactMarkdown source={artefact.description} />
                                                            </HtmlContentBlock>
                                                        </SimpleDialog>
                                                    )}
                                                </Card>
                                            ))}
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
    return (
        <ErrorBoundary>
            {isProjectImport ? (
                <ProjectImportModal close={closeModal} back={() => setIsProjectImport(false)} />
            ) : (
                createDialog
            )}
        </ErrorBoundary>
    );
}
