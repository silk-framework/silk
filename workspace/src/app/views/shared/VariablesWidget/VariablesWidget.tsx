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
import { FetchError } from "../../../services/fetch/responseInterceptor";

const VariablesWidget: React.FC<VariableWidgetProps> = ({ projectId, taskId }) => {
    const { registerError } = useErrorHandler();
    const [loadingVariables, setLoadingVariables] = React.useState<boolean>(false);
    const [variables, setVariables] = React.useState<Array<Variable>>([]);
    const [selectedVariable, setSelectedVariable] = React.useState<Variable>();
    const [modalOpen, setModalOpen] = React.useState<boolean>(false);
    const [refetch, setRefetch] = React.useState<number>(0);
    const [isDeleting, setIsDeleting] = React.useState<boolean>(false);
    const [deleteModalOpen, setDeleteModalOpen] = React.useState<boolean>(false);
    const [deleteErrorMsg, setDeleteErrMsg] = React.useState<string>("");
    const [dropChangeLoading, setDropChangeLoading] = React.useState<boolean>(false);
    const [dependencies, setVariableDependencies] = React.useState<VariableDependencies>();
    const [t] = useTranslation();

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
        setModalOpen(true);
    }, []);

    const handleDeleteModalOpen = React.useCallback(async (variable: Variable) => {
        setSelectedVariable(variable);
        setDeleteModalOpen(true);
        setVariableDependencies((await getVariableDependencies(projectId, variable.name)).data);
        setDeleteErrMsg("");
    }, []);

    /**
     * upon acceptance on the delete prompt, it deletes the selected variable.
     */
    const handleDeleteVariable = React.useCallback(async () => {
        if (!selectedVariable) return;
        setIsDeleting(true);
        setDeleteErrMsg("");
        try {
            await deleteVariableRequest(projectId, selectedVariable.name);
            setRefetch((r) => ++r);
            setDeleteModalOpen(false);
        } catch (err) {
            setDeleteErrMsg(err?.body?.detail);
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
                    registerError("VariableWidgetError", err.body.title, err);
                }
            } finally {
                setDropChangeLoading(false);
            }
        },
        [variables]
    );

    const renderDeleteVariable = React.useCallback(() => {
        if (!selectedVariable || !dependencies) return <></>;
        const varyingDeleteTranslation =
            dependencies.dependentTasks.length || dependencies.dependentVariables.length
                ? "deletePromptWithDependencies"
                : "deletePromptNoDep";
        return (
            <div>
                {t(`widget.VariableWidget.modalMessages.${varyingDeleteTranslation}`, {
                    varname: selectedVariable.name,
                })}
                {(dependencies.dependentVariables?.length && (
                    <>
                        <Spacing />
                        <p>Dependent variables</p>
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
                        <p>Dependent tasks</p>
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
    }, [selectedVariable, dependencies]);

    return (
        <>
            <NewVariableModal
                modalOpen={modalOpen}
                closeModal={() => setModalOpen(false)}
                variables={variables}
                targetVariable={selectedVariable}
                projectId={projectId}
                taskId={taskId}
                refresh={() => setRefetch((r) => ++r)}
            />
            <DeleteModal
                data-test-id="delete-variable-modal"
                title={t("widget.VariableWidget.modalMessages.deleteModalTitle", "Delete variable")}
                onConfirm={handleDeleteVariable}
                isOpen={deleteModalOpen}
                onDiscard={() => setDeleteModalOpen(false)}
                removeLoading={isDeleting}
                errorMessage={deleteErrorMsg}
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
                                                                        <PropertyValuePair style={{ width: "100%" }}>
                                                                            <PropertyName
                                                                                style={{
                                                                                    whiteSpace: "nowrap",
                                                                                    overflow: "visible",
                                                                                }}
                                                                                title={variable.name}
                                                                                size="large"
                                                                            >
                                                                                <Label
                                                                                    isLayoutForElement="span"
                                                                                    text={
                                                                                        <OverflowText inline>
                                                                                            {variable.name}
                                                                                        </OverflowText>
                                                                                    }
                                                                                    tooltip={variable.description}
                                                                                    style={{ lineHeight: "normal" }}
                                                                                />
                                                                            </PropertyName>
                                                                            <PropertyValue
                                                                                style={{
                                                                                    marginLeft: "calc(31.25% + 14px)",
                                                                                }}
                                                                            >
                                                                                <OverflowText>
                                                                                    <code>{variable.value}</code>
                                                                                </OverflowText>
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
