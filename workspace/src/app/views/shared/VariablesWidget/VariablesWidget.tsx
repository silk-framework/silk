import React from "react";
import { Draggable, DragDropContext, Droppable } from "react-beautiful-dnd";

//typing
import { Variable, VariableWidgetProps } from "./typing";
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
    Notification,
    OverflowText,
    OverviewItemList,
    PropertyName,
    PropertyValue,
    PropertyValuePair,
    Spacing,
    Toolbar,
    ToolbarSection,
} from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { getVariables } from "./requests";
import useErrorHandler from "../../../hooks/useErrorHandler";
import Loading from "../Loading";
import NewVariableModal from "./modals/NewVariableModal";
import { createNewVariable } from "./requests";
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
    const [t] = useTranslation();

    // initial loading of variables
    React.useEffect(() => {
        (async () => {
            try {
                setLoadingVariables(true);
                const { data } = await getVariables(projectId);
                setVariables(data?.variables ?? []);
            } catch (err) {
                registerError("variable-config", "Could get load variables", err);
            } finally {
                setLoadingVariables(false);
            }
        })();
    }, [refetch]);

    const handleModalOpen = React.useCallback((variable = undefined) => {
        setSelectedVariable(variable);
        setModalOpen(true);
    }, []);

    const handleDeleteModalOpen = React.useCallback((variable: Variable) => {
        setSelectedVariable(variable);
        setDeleteModalOpen(true);
        setDeleteErrMsg("");
    }, []);

    /**
     * upon acceptance on the delete prompt, it deletes the selected variable.
     */
    const handleDeleteVariable = React.useCallback(async () => {
        setIsDeleting(true);
        setDeleteErrMsg("");
        try {
            const filteredVariables = {
                variables: variables.filter((variable) => variable.name !== selectedVariable?.name),
            };
            await createNewVariable(filteredVariables, projectId);
            setRefetch((r) => ++r);
            setDeleteModalOpen(false);
        } catch (err) {
            setDeleteErrMsg(err?.body?.detail);
        } finally {
            setIsDeleting(false);
        }
    }, [selectedVariable]);

    const handleVariableDragStart = React.useCallback(() => {}, []);

    const handleVariableDragEnd = React.useCallback(
        async (result) => {
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

            const reorderedVariables = { variables: reorderArray(variables, fromPos, toPos) };
            try {
                await createNewVariable(reorderedVariables, projectId);
                setRefetch((f) => ++f);
            } catch (err) {
                if (err && (err as FetchError).isFetchError && err.body.detail.includes("Unknown token found: ")) {
                    registerError(
                        "VariableWidgetError",
                        t("widget.VariableWidget.errorMessages.orderingVariables"),
                        err
                    );
                }
            }
        },
        [variables]
    );

    const renderDeleteVariable = React.useCallback(() => {
        return (
            <div>
                Are you sure you want to delete variable <strong>{selectedVariable?.name}</strong>?
            </div>
        );
    }, [selectedVariable]);

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
                        <h2>{t("widget.VariableWidget.title", "Variables")}</h2>
                    </CardTitle>
                    <CardOptions>
                        <IconButton
                            data-test-id="variable-add"
                            name={"item-add-artefact"}
                            text={t("widget.VariableWidget.actions.add", "Add")}
                            onClick={() => handleModalOpen()}
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
                                                <Draggable key={variable.name} pos={i} index={i} draggableId={`${i}`}>
                                                    {(provided) => (
                                                        <div
                                                            data-test-id="variable-list-item"
                                                            ref={provided.innerRef}
                                                            {...provided.draggableProps}
                                                            {...provided.dragHandleProps}
                                                        >
                                                            <Toolbar noWrap>
                                                                <ToolbarSection>
                                                                    <Icon small name="item-draggable" />
                                                                    <Spacing size="tiny" vertical />
                                                                </ToolbarSection>
                                                                <ToolbarSection canGrow canShrink>
                                                                    <PropertyValuePair>
                                                                        <PropertyName
                                                                            style={{
                                                                                whiteSpace: "nowrap",
                                                                                overflow: "visible",
                                                                            }}
                                                                            title={variable.name}
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
                                                                                marginLeft: "calc(18.75% + 14px)",
                                                                            }}
                                                                        >
                                                                            <OverflowText>
                                                                                <code>{variable.value}</code>
                                                                            </OverflowText>
                                                                        </PropertyValue>
                                                                    </PropertyValuePair>
                                                                </ToolbarSection>
                                                                {variable.template != null && (
                                                                    <ToolbarSection>
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
                                                                    </ToolbarSection>
                                                                )}
                                                                <ToolbarSection>
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
                                                                        onClick={() => handleDeleteModalOpen(variable)}
                                                                        disruptive
                                                                    />
                                                                </ToolbarSection>
                                                            </Toolbar>
                                                        </div>
                                                    )}
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
