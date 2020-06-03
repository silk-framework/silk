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
    OverviewItem,
    OverviewItemDepiction,
    OverviewItemDescription,
    OverviewItemLine,
    OverviewItemActions,
    OverviewItemList,
    OverflowText,
    SimpleDialog,
    Spacing,
} from "@wrappers/index";
import { commonOp, commonSel } from "@ducks/common";
import { IArtefactItem, IArtefactModal, IDetailedArtefactItem } from "@ducks/common/typings";
import Loading from "../../Loading";
import { ProjectForm } from "./ArtefactForms/ProjectForm";
import { TaskForm } from "./ArtefactForms/TaskForm";
import { DATA_TYPES } from "../../../../constants";
import { Highlighter } from "../../Highlighter/Highlighter";
import ArtefactTypesList from "./ArtefactTypesList";
import { SearchBar } from "../../SearchBar/SearchBar";

export function CreateArtefactModal() {
    const dispatch = useDispatch();
    const form = useForm();

    const [searchValue, setSearchValue] = useState("");
    const [idEnhancedDescription, setIdEnhancedDescription] = useState("");

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
    const DOUBLE_CLICK_LIMIT_MS = 500;

    // Fetch Artefact list
    useEffect(() => {
        if (projectId) {
            dispatch(commonOp.fetchArtefactsListAsync());
        }
    }, [projectId]);

    useEffect(() => {
        setSelected({} as IArtefactItem);
        if (artefactsList.length > 0 && searchValue) {
            setSelected(artefactsList[0]);
        }
    }, [artefactsList]);

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
    }

    const handleEnter = (e) => {
        if (e.key === 'Enter' && selected) {
            handleAdd();
        }
    };

    const handleBack = () => {
        resetModal();
    };

    const taskType = (artefactId) => {
        if (artefactId === "project") {
            return "Project";
        } else {
            return (cachedArtefactProperties[artefactId] as IDetailedArtefactItem).taskType;
        }
    };

    const handleCreate = async (e) => {
        e.preventDefault();

        const isValidFields = await form.triggerValidation();
        if (isValidFields) {
            if (updateExistingTask) {
                dispatch(
                    commonOp.fetchUpdateTaskAsync(
                        updateExistingTask.projectId,
                        updateExistingTask.taskId,
                        form.getValues()
                    )
                );
            } else {
                dispatch(commonOp.createArtefactAsync(form.getValues(), taskType(selectedArtefact.key)));
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
    };

    const closeModal = () => {
        resetModal(true);
    };

    const isErrorPresented = () => !!Object.keys(form.errors).length;

    const handleSelectDType = (value: string) => {
        dispatch(commonOp.setSelectedArtefactDType(value));
    };

    const resetModal = (closeModal?: boolean) => {
        setSelected({} as IArtefactItem);
        form.clearError();
        dispatch(commonOp.resetArtefactModal(closeModal));
    };

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
            if (selectedArtefact.key === DATA_TYPES.PROJECT) {
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
        (artefact) => selectedDType === "all" || commonOp.itemTypeToPath(artefact.taskType) === selectedDType
    );
    if (showProjectItem && selectedDType === "all") {
        artefactListWithProject = [
            {
                key: DATA_TYPES.PROJECT,
                title: "Project",
                description:
                    "Projects let you group related items. All items that " +
                    "depend on each other need to be in the same project.",
                taskType: "project",
            },
            ...artefactListWithProject,
        ];
    }

    const renderDepiction = (artefact, large=true) => {
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

    return (
        <SimpleDialog
            size="large"
            preventSimpleClosing={true}
            hasBorder
            title={
                updateExistingTask
                    ? `Update '${updateExistingTask.metaData.label}' (${updateExistingTask.taskPluginDetails.title})`
                    : `Create new item of type ${selectedArtefact.title || ""}`
            }
            onClose={closeModal}
            isOpen={isOpen}
            actions={
                selectedArtefact.key || updateExistingTask
                    ? [
                          <Button key="create" affirmative={true} onClick={handleCreate} disabled={isErrorPresented()}>
                              {updateExistingTask ? "Update" : "Create"}
                          </Button>,
                          <Button key="cancel" onClick={closeModal}>
                              Cancel
                          </Button>,
                          <CardActionsAux key="aux">
                              {!updateExistingTask && (
                                  <Button key="back" onClick={handleBack}>
                                      Back
                                  </Button>
                              )}
                          </CardActionsAux>,
                      ]
                    : [
                          <Button
                              key="add"
                              affirmative={true}
                              onClick={handleAdd}
                              disabled={!Object.keys(selected).length}
                          >
                              Add
                          </Button>,
                          <Button key="cancel" onClick={closeModal}>
                              Cancel
                          </Button>,
                      ]
            }
        >
            {
                <>
                    {artefactForm ? (
                        <>
                            {artefactForm}
                            {!!error.detail && (
                                <Notification
                                    message={`${updateExistingTask ? "Update" : "Create"} action failed. Details: ${
                                        error.detail
                                    }`}
                                    danger
                                />
                            )}
                        </>
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
                                        <Loading description="Loading artefact type list." />
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
                                                            <IconButton name="item-info" onClick={(e) => {handleShowEnhancedDescription(e, artefact.key)}} />
                                                        </OverviewItemActions>
                                                    </OverviewItem>
                                                    {
                                                        idEnhancedDescription === artefact.key && (
                                                            <SimpleDialog
                                                                isOpen
                                                                title={artefact.title}
                                                                actions={
                                                                    <Button text="Close" onClick={() => { setIdEnhancedDescription("") }} />
                                                                }
                                                                size="small"
                                                            >
                                                                <HtmlContentBlock>
                                                                    <ReactMarkdown source={artefact.description} />
                                                                </HtmlContentBlock>
                                                            </SimpleDialog>
                                                        )
                                                    }
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
}
