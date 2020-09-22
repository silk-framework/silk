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
    HtmlContentBlock,
    Icon,
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
import { commonOp, commonSel } from "@ducks/common";
import { IArtefactItem, IArtefactModal, IDetailedArtefactItem } from "@ducks/common/typings";
import Loading from "../../Loading";
import { ProjectForm } from "./ArtefactForms/ProjectForm";
import { TaskForm } from "./ArtefactForms/TaskForm";
import { DATA_TYPES } from "../../../../constants";
import { extractSearchWords, Highlighter, multiWordRegex } from "../../Highlighter/Highlighter";
import ArtefactTypesList from "./ArtefactTypesList";
import { SearchBar } from "../../SearchBar/SearchBar";
import { routerOp } from "@ducks/router";
import { useTranslation } from "react-i18next";
import { TaskType } from "@ducks/shared/typings";
import { ProjectImportModal } from "../ProjectImportModal";

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

    // initially take from redux
    const [selected, setSelected] = useState<IArtefactItem>(selectedArtefact);
    const [lastSelectedClick, setLastSelectedClick] = useState<number>(0);
    const [isProjectImport, setIsProjectImport] = useState<boolean>(false);
    const DOUBLE_CLICK_LIMIT_MS = 500;

    // Fetch Artefact list
    useEffect(() => {
        if (projectId) {
            dispatch(commonOp.fetchArtefactsListAsync());
        } else {
            dispatch(commonOp.resetArtefactsList());
        }
    }, [projectId]);

    const handleAdd = () => {
        if (selected.key === DATA_TYPES.PROJECT) {
            return dispatch(commonOp.selectArtefact(selected));
        }
        dispatch(commonOp.getArtefactPropertiesAsync(selected));
    };

    const handleSearch = (textQuery: string) => {
        if (!projectId) {
            return;
        }
        setSearchValue(textQuery);
        dispatch(
            commonOp.fetchArtefactsListAsync({
                textQuery,
            })
        );
    };

    const handleArtefactSelect = (artefact: IArtefactItem) => {
        if (
            selected.key === artefact.key &&
            lastSelectedClick &&
            Date.now() - lastSelectedClick < DOUBLE_CLICK_LIMIT_MS
        ) {
            handleAdd();
        } else {
            setSelected(artefact);
        }
        setLastSelectedClick(Date.now);
    };

    const handleShowEnhancedDescription = (event, artefactId) => {
        event.preventDefault();
        event.stopPropagation();
        setIdEnhancedDescription(artefactId);
    };

    const handleEnter = (e) => {
        if (e.key === "Enter" && selected) {
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
                    await dispatch(commonOp.createArtefactAsync(form.getValues(), taskType(selectedArtefact.key)));
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
            setActionLoading(false);
        }
    };

    const closeModal = () => {
        resetModal(true);
    };

    const isErrorPresented = () => !!Object.keys(form.errors).length;

    const handleSelectDType = (value: string) => {
        dispatch(commonOp.setSelectedArtefactDType(value));
    };

    const resetModal = (closeModal?: boolean) => {
        setIsProjectImport(false);
        setSelected({} as IArtefactItem);
        form.clearError();
        dispatch(commonOp.resetArtefactModal(closeModal));
    };

    const switchToProjectImport = () => {
        setIsProjectImport(true);
    };

    const projectArtefactSelected = selectedArtefact.key === DATA_TYPES.PROJECT;

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
        if (selectedArtefact.key) {
            if (projectArtefactSelected) {
                artefactForm = <ProjectForm form={form} projectId={projectId} />;
            } else {
                const detailedArtefact = cachedArtefactProperties[selectedArtefact.key];
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
        const regex = multiWordRegex(extractSearchWords(searchValue));
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
        setSelected({} as IArtefactItem);
        if (artefactListWithProject.length > 0 && searchValue) {
            setSelected(artefactListWithProject[0]);
        }
    }, [artefactListWithProject.map((item) => item.key).join("|"), selectedDType]);

    const renderDepiction = (artefact, large = true) => {
        const iconNameStack = []
            .concat([(artefact.taskType ? artefact.taskType + "-" : "") + artefact.key])
            .concat(artefact.taskType ? [artefact.taskType] : [])
            .concat(artefact.categories ? artefact.categories : []);
        return (
            <Icon
                name={iconNameStack
                    .map((type) => {
                        return "artefact-" + type.toLowerCase();
                    })
                    .filter((x, i, a) => a.indexOf(x) === i)}
                large={large}
            />
        );
    };

    const isCreationUpdateDialog = selectedArtefact.key || updateExistingTask;

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
                    : selectedArtefact.title
                    ? t("CreateModal.createTitle", { type: selectedArtefact.title })
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
                            <div key="optionalProjectActions">
                                {projectArtefactSelected && (
                                    <Button key="importProject" onClick={switchToProjectImport} affirmative={true}>
                                        {t("form.projectForm.importProjectButton")}
                                    </Button>
                                )}
                            </div>,
                            <CardActionsAux key="aux">
                                {!updateExistingTask && (
                                    <Button key="back" onClick={handleBack}>
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
                            disabled={!Object.keys(selected).length}
                        >
                            {t("common.action.add")}
                        </Button>,
                        <Button key="cancel" onClick={closeModal}>
                            {t("common.action.cancel")}
                        </Button>,
                    ]
                )
            }
            notifications={
                !!error.detail && (
                    <Notification
                        message={t("common.messages.actionFailed", {
                            action: updateExistingTask ? t("common.action.update") : t("common.action.create"),
                            error: error.detail.replace(/^(assertion failed: )/, ""),
                        })}
                        danger
                    />
                )
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
                                        <p>{t("CreateModal.noMatch", "No match found.")}</p>
                                    ) : (
                                        <OverviewItemList hasSpacing columns={2}>
                                            {artefactListWithProject.map((artefact) => (
                                                <Card
                                                    isOnlyLayout
                                                    key={artefact.key}
                                                    className={
                                                        selected.key === artefact.key ? HelperClasses.Intent.ACCENT : ""
                                                    }
                                                >
                                                    <OverviewItem
                                                        hasSpacing
                                                        onClick={() => handleArtefactSelect(artefact)}
                                                        onKeyDown={handleEnter}
                                                    >
                                                        <OverviewItemDepiction>
                                                            {renderDepiction(artefact)}
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
    return isProjectImport ? (
        <ProjectImportModal close={closeModal} back={() => setIsProjectImport(false)} />
    ) : (
        createDialog
    );
}
