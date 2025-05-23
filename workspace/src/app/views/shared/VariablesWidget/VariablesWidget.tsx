import React from "react";
import { DragDropContext, Draggable, Droppable } from "react-beautiful-dnd";

//typing
import { Variable, VariableDependencies, VariableWidgetProps } from "./typing";
import {
    Card,
    CardContent,
    CardHeader,
    CardOptions,
    CardTitle,
    Divider,
    Icon,
    IconButton,
    Label,
    Link,
    Notification,
    OverflowText,
    OverviewItemList,
    PropertyName,
    PropertyValue,
    PropertyValuePair,
    Spacing,
    Toolbar,
    ToolbarSection,
    Tooltip,
} from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { deleteVariableRequest, getVariableDependencies, getVariables, reorderVariablesRequest } from "./requests";
import useErrorHandler from "../../../hooks/useErrorHandler";
import Loading from "../Loading";
import NewVariableModal from "./modals/NewVariableModal";
import reorderArray from "../../../views/pages/MappingEditor/HierarchicalMapping/utils/reorderArray";
import DeleteModal from "../modals/DeleteModal";
import { ErrorResponse, FetchError } from "../../../services/fetch/responseInterceptor";
import { useModalError } from "../../../hooks/useModalError";

const VariablesWidget: React.FC<VariableWidgetProps> = ({ projectId, taskId }) => {
    const { registerError } = useErrorHandler();
    const [loadingVariables, setLoadingVariables] = React.useState<boolean>(false);
    const [variables, setVariables] = React.useState<Array<Variable>>([]);
    const [selectedVariable, setSelectedVariable] = React.useState<Variable>();
    const [modalOpen, setModalOpen] = React.useState<boolean>(false);
    const [refetch, setRefetch] = React.useState<number>(0);
    const [isDeleting, setIsDeleting] = React.useState<boolean>(false);
    const [deleteModalOpen, setDeleteModalOpen] = React.useState<boolean>(false);
    const [deleteError, setDeleteError] = React.useState<ErrorResponse | undefined>();
    const checkAndDisplayDeletionError = useModalError({ setError: setDeleteError });
    const [dropChangeLoading, setDropChangeLoading] = React.useState<boolean>(false);
    const [dependencies, setVariableDependencies] = React.useState<VariableDependencies>();
    const [errorNotification, setErrorNotification] = React.useState<JSX.Element | null>(null);
    const [t] = useTranslation();

    const variableHasDependencies = dependencies?.dependentTasks.length || dependencies?.dependentVariables.length;

    // initial loading of variables
    React.useEffect(() => {
        (async () => {
            try {
                setLoadingVariables(true);
                const { data } = await getVariables(projectId);
                setVariables(data?.variables ?? []);
            } catch (err) {
                registerError("variable-config", t("widget.VariableWidget.errorMessages.loadingVariables"), err);
            } finally {
                setLoadingVariables(false);
            }
        })();
    }, [refetch, projectId]);

    const handleModalOpen = React.useCallback((variable = undefined) => {
        setSelectedVariable(variable);
        setErrorNotification(null);
        setModalOpen(true);
    }, []);

    const handleDeleteModalOpen = React.useCallback(async (variable: Variable) => {
        setDeleteError(undefined);
        setSelectedVariable(variable);
        setDeleteModalOpen(true);
        try {
            setErrorNotification(null);
            setVariableDependencies((await getVariableDependencies(projectId, variable.name)).data);
        } catch (err) {
            checkAndDisplayDeletionError(
                err,
                t("widget.VariableWidget.errorMessages.dependencyRetrievalFailure", "Failed to retrieve variable")
            );
        }
    }, []);

    /**
     * upon acceptance on the delete prompt, it deletes the selected variable.
     */
    const handleDeleteVariable = React.useCallback(async () => {
        if (!selectedVariable) return;
        setIsDeleting(true);
        setDeleteError(undefined);
        try {
            await deleteVariableRequest(projectId, selectedVariable.name);
            setRefetch((r) => ++r);
            setDeleteModalOpen(false);
        } catch (err) {
            checkAndDisplayDeletionError(
                err,
                t("widget.VariableWidget.errorMessages.variableDeletionFailure", "Failed to delete variable")
            );
        } finally {
            setIsDeleting(false);
        }
    }, [selectedVariable]);

    const handleVariableDragStart = React.useCallback(() => {
        if (variables.length === 1) return;
    }, [variables]);

    const handleVariableDragEnd = React.useCallback(
        async (result) => {
            if (variables.length === 1) return;
            // dropped outside the list
            if (!result.destination) {
                return;
            }
            const fromPos = result.source.index;
            const toPos = result.destination.index;
            // no actual movement
            if (fromPos === toPos) {
                return;
            }
            const reorderedVariables = reorderArray(variables, fromPos, toPos) as Variable[];
            try {
                setErrorNotification(null);
                setDropChangeLoading(true);
                const res = await reorderVariablesRequest(
                    projectId,
                    reorderedVariables.map((v) => v.name)
                );
                if (res.axiosResponse.status === 200) {
                    setVariables(reorderedVariables);
                }
            } catch (err) {
                if (err && (err as FetchError).isFetchError) {
                    const errorNotification = registerError("VariableWidgetError", err.body.title, err, {
                        errorNotificationInstanceId: "VariablesWidget",
                        onDismiss: () => setErrorNotification(null),
                    });
                    setErrorNotification(errorNotification);
                }
            } finally {
                setDropChangeLoading(false);
            }
        },
        [variables]
    );

    const renderDeleteVariable = React.useCallback(() => {
        if (!selectedVariable || !dependencies) return <></>;
        const varyingDeleteTranslation = variableHasDependencies ? "deletePromptWithDependencies" : "deletePromptNoDep";
        return (
            <div>
                {t(`widget.VariableWidget.modalMessages.${varyingDeleteTranslation}`, {
                    varname: selectedVariable.name,
                })}
                {(dependencies.dependentVariables?.length && (
                    <>
                        <Spacing />
                        <p>{t("widget.VariableWidget.dependencyTypes.variables", "Dependent variables")}</p>
                        <ul>
                            {dependencies.dependentVariables.map((variable) => (
                                <li key={variable}>{variable}</li>
                            ))}
                        </ul>
                    </>
                )) ||
                    null}

                {(dependencies.dependentTasks?.length && (
                    <>
                        <Spacing />
                        <p>{t("widget.VariableWidget.dependencyTypes.tasks", "Dependent tasks")}</p>
                        <ul>
                            {dependencies.dependentTasks.map((task) => (
                                <li key={task.id}>
                                    <Link href={task.taskLink} target="_blank">
                                        <Tooltip content={t("common.action.openInNewTab")}>
                                            {task.label ?? task.id}
                                        </Tooltip>
                                    </Link>
                                </li>
                            ))}
                        </ul>
                    </>
                )) ||
                    null}
            </div>
        );
    }, [selectedVariable, dependencies, deleteError]);

    return (
        <>
            {modalOpen && (
                <NewVariableModal
                    closeModal={() => setModalOpen(false)}
                    variables={variables}
                    targetVariable={selectedVariable}
                    projectId={projectId}
                    taskId={taskId}
                    refresh={() => setRefetch((r) => ++r)}
                />
            )}
            <DeleteModal
                data-test-id="delete-variable-modal"
                title={t("widget.VariableWidget.modalMessages.deleteModalTitle", "Delete variable")}
                onConfirm={handleDeleteVariable}
                isOpen={deleteModalOpen}
                onDiscard={() => setDeleteModalOpen(false)}
                removeLoading={isDeleting}
                deleteDisabled={!!variableHasDependencies}
                errorMessage={deleteError && deleteError.detail}
                render={renderDeleteVariable}
            />
            <Card>
                <CardHeader>
                    <CardTitle>
                        <h2>{t("widget.VariableWidget.title", "Project Variables")}</h2>
                    </CardTitle>
                    <CardOptions>
                        <IconButton
                            data-test-id="variable-add"
                            name={"item-add-artefact"}
                            text={t("widget.VariableWidget.actions.add", "Add")}
                            onClick={() => handleModalOpen()}
                            loading={dropChangeLoading}
                        />
                    </CardOptions>
                </CardHeader>
                {errorNotification}
                <Divider />
                <CardContent style={{ maxHeight: "25vh" }}>
                    {loadingVariables ? (
                        <Loading />
                    ) : !variables.length ? (
                        <Notification message={t("widget.VariableWidget.noVariables", "No  Variables set")} />
                    ) : (
                        <DragDropContext onDragStart={handleVariableDragStart} onDragEnd={handleVariableDragEnd}>
                            <Droppable droppableId="variableDroppable">
                                {(provided) => (
                                    <div ref={provided.innerRef} {...provided.droppableProps}>
                                        <OverviewItemList hasDivider>
                                            {variables.map((variable, i) => (
                                                <Draggable
                                                    key={variable.name}
                                                    pos={i}
                                                    index={i}
                                                    draggableId={variable.name}
                                                >
                                                    {(provided) => {
                                                        const draggablePlugins =
                                                            variables.length === 1
                                                                ? {}
                                                                : {
                                                                      ...provided.draggableProps,
                                                                      ...provided.dragHandleProps,
                                                                  };
                                                        return (
                                                            <div
                                                                data-test-id="variable-list-item"
                                                                ref={provided.innerRef}
                                                                {...draggablePlugins}
                                                            >
                                                                <Toolbar noWrap>
                                                                    {variables.length > 1 ? (
                                                                        <ToolbarSection>
                                                                            <Icon small name="item-draggable" />
                                                                            <Spacing size="tiny" vertical />
                                                                        </ToolbarSection>
                                                                    ) : null}
                                                                    <ToolbarSection canGrow canShrink>
                                                                        <PropertyValuePair nowrap>
                                                                            <PropertyName
                                                                                title={variable.name}
                                                                                size="large"
                                                                                labelProps={{
                                                                                    tooltip: variable.description,
                                                                                    style: { lineHeight: "normal" },
                                                                                }}
                                                                            >
                                                                                {variable.name}
                                                                            </PropertyName>
                                                                            <PropertyValue
                                                                                style={{
                                                                                    marginLeft: "calc(31.25% + 14px)",
                                                                                }}
                                                                            >
                                                                                <code>{variable.value}</code>
                                                                            </PropertyValue>
                                                                        </PropertyValuePair>
                                                                    </ToolbarSection>
                                                                    <ToolbarSection
                                                                        style={{
                                                                            minWidth: "75px",
                                                                            justifyContent: "right",
                                                                        }}
                                                                    >
                                                                        {variable.template != null && (
                                                                            <>
                                                                                <Icon
                                                                                    name={"form-template"}
                                                                                    intent={"info"}
                                                                                    data-test-id="template-variable-delimiter"
                                                                                    tooltipText={
                                                                                        t(
                                                                                            "widget.TaskConfigWidget.templateValueInfo"
                                                                                        ) +
                                                                                        `\n\n\`\`\`${variable.template}\`\`\``
                                                                                    }
                                                                                    tooltipProps={{
                                                                                        placement: "top",
                                                                                        markdownEnabler: "```",
                                                                                    }}
                                                                                />
                                                                                <Spacing size="tiny" vertical />
                                                                            </>
                                                                        )}
                                                                        <IconButton
                                                                            small
                                                                            name="item-edit"
                                                                            data-test-id="variable-edit-btn"
                                                                            onClick={() => handleModalOpen(variable)}
                                                                        />
                                                                        <IconButton
                                                                            small
                                                                            name="item-remove"
                                                                            data-test-id="variable-delete-btn"
                                                                            onClick={() =>
                                                                                handleDeleteModalOpen(variable)
                                                                            }
                                                                            disruptive
                                                                        />
                                                                    </ToolbarSection>
                                                                </Toolbar>
                                                            </div>
                                                        );
                                                    }}
                                                </Draggable>
                                            ))}
                                        </OverviewItemList>
                                    </div>
                                )}
                            </Droppable>
                        </DragDropContext>
                    )}
                </CardContent>
            </Card>
        </>
    );
};

export default VariablesWidget;
