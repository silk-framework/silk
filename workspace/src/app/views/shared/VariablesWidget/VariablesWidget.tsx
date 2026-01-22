import React from "react";
import {DndContext, KeyboardSensor, useSensor, useSensors} from "@dnd-kit/core";
import {
    arrayMove,
    SortableContext,
    sortableKeyboardCoordinates,
    useSortable,
    verticalListSortingStrategy
} from "@dnd-kit/sortable";
import {CSS} from "@dnd-kit/utilities";
import {restrictToVerticalAxis} from "@dnd-kit/modifiers";

//typing
import {Variable, VariableDependencies, VariableWidgetProps} from "./typing";
import {
    Card,
    CardContent,
    CardHeader,
    CardOptions,
    CardTitle,
    Divider,
    Icon,
    IconButton,
    Link,
    Notification,
    OverviewItemList,
    PropertyName,
    PropertyValue,
    PropertyValuePair,
    Spacing,
    Toolbar,
    ToolbarSection,
    Tooltip,
} from "@eccenca/gui-elements";
import {useTranslation} from "react-i18next";
import {deleteVariableRequest, getVariableDependencies, getVariables, reorderVariablesRequest} from "./requests";
import useErrorHandler from "../../../hooks/useErrorHandler";
import Loading from "../Loading";
import NewVariableModal from "./modals/NewVariableModal";
import DeleteModal from "../modals/DeleteModal";
import {ErrorResponse, FetchError} from "../../../services/fetch/responseInterceptor";
import {useModalError} from "../../../hooks/useModalError";
import dndkitUtils from "../../../utils/dndkitUtils";

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
    const [errorNotification, setErrorNotification] = React.useState<React.JSX.Element | null>(null);
    const [t] = useTranslation();

    const sensors = useSensors(
        useSensor(dndkitUtils.DefaultMouseSensor),
        useSensor(KeyboardSensor, {
            coordinateGetter: sortableKeyboardCoordinates,
        })
    );

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

    const handleModalOpen = React.useCallback((variable: Variable | undefined = undefined) => {
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
                t("widget.VariableWidget.errorMessages.dependencyRetrievalFailure", "Failed to retrieve variable"),
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
                t("widget.VariableWidget.errorMessages.variableDeletionFailure", "Failed to delete variable"),
            );
        } finally {
            setIsDeleting(false);
        }
    }, [selectedVariable]);

    const handleVariableDragEnd = React.useCallback(
        async (event) => {
            const { active, over } = event;

            if (variables.length === 1) return;
            // dropped outside the list
            if (!over) {
                return;
            }
            // no actual movement
            if (active.id === over.id) {
                return;
            }

            const oldIndex = variables.findIndex(v => v.name === active.id);
            const newIndex = variables.findIndex(v => v.name === over.id);

            const reorderedVariables = arrayMove(variables, oldIndex, newIndex);

            try {
                setErrorNotification(null);
                setDropChangeLoading(true);
                const res = await reorderVariablesRequest(
                    projectId,
                    reorderedVariables.map((v) => v.name),
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
        [variables],
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
                        <DndContext sensors={sensors} onDragEnd={handleVariableDragEnd} modifiers={[restrictToVerticalAxis]}>
                            <SortableContext items={variables.map((v) => v.name)} strategy={verticalListSortingStrategy}>
                                <OverviewItemList hasDivider>
                                    {variables.map((variable) => (
                                        <SortableVariableItem
                                            key={variable.name}
                                            variable={variable}
                                            isOnlyItem={variables.length === 1}
                                            onEdit={handleModalOpen}
                                            onDelete={handleDeleteModalOpen}
                                        />
                                    ))}
                                </OverviewItemList>
                            </SortableContext>
                        </DndContext>
                    )}
                </CardContent>
            </Card>
        </>
    );
};

interface SortableVariableItemProps {
    variable: Variable;
    isOnlyItem: boolean;
    onEdit: (variable: Variable) => void;
    onDelete: (variable: Variable) => void;
}

/** A single variable entry. */
const SortableVariableItem: React.FC<SortableVariableItemProps> = ({ variable, isOnlyItem, onEdit, onDelete }) => {
    const [t] = useTranslation()
    const {
        attributes,
        listeners,
        setNodeRef,
        transform,
        transition,
        isDragging,
    } = useSortable({ id: variable.name, disabled: isOnlyItem });

    const style = {
        transform: CSS.Transform.toString(transform),
        transition,
        opacity: isDragging ? 0.5 : 1,
    };

    const draggableProps = isOnlyItem ? {} : { ...attributes, ...listeners };

    return (
        <div
            data-test-id="variable-list-item"
            ref={setNodeRef}
            style={style}
            {...draggableProps}
        >
            <Toolbar noWrap>
                {!isOnlyItem ? (
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
                            <span className={"nodrag"}>{variable.name}</span>
                        </PropertyName>
                        <PropertyValue
                            style={{
                                marginLeft: "calc(31.25% + 14px)",
                            }}
                        >
                            <code className={"nodrag"}>{variable.value}</code>
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
                                    t("widget.TaskConfigWidget.templateValueInfo") +
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
                        name="item-edit"
                        data-test-id="variable-edit-btn"
                        onClick={() => onEdit(variable)}
                        size={"small"}
                        className={"nodrag"}
                    />
                    <IconButton
                        name="item-remove"
                        data-test-id="variable-delete-btn"
                        onClick={() => onDelete(variable)}
                        disruptive
                        size={"small"}
                        className={"nodrag"}
                    />
                </ToolbarSection>
            </Toolbar>
        </div>
    );
};

export default VariablesWidget;
