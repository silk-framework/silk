import React from "react";

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
    OverflowText,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDescription,
    OverviewItemLine,
    PropertyName,
    PropertyValue,
    PropertyValueList,
    PropertyValuePair,
    Spacing,
} from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { getVariables } from "./requests";
import useErrorHandler from "../../../hooks/useErrorHandler";
import Loading from "../Loading";
import NewVariableModal from "./modals/NewVariableModal";
import DeleteVariablePrompt from "./modals/DeleteVariablePrompt";
import { createNewVariable } from "./requests";

const VariablesWidget: React.FC<VariableWidgetProps> = ({ projectId, taskId }) => {
    const { registerError } = useErrorHandler();
    const [loadingVariables, setLoadingVariables] = React.useState<boolean>(false);
    const [variables, setVariables] = React.useState<Array<Variable>>([]);
    const [selectedVariable, setSelectedVariable] = React.useState<Variable>();
    const [modalOpen, setModalOpen] = React.useState<boolean>(false);
    const [refetch, setRefetch] = React.useState<number>(0);
    const [isDeleting, setIsDeleting] = React.useState<boolean>(false);
    const [deleteModalOpen, setDeleteModalOpen] = React.useState<boolean>(false);
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
    }, []);

    const handleDeleteVariable = React.useCallback(async () => {
        setIsDeleting(true);
        try {
            const filteredVariables = {
                variables: variables.filter((variable) => variable.name !== selectedVariable?.name),
            };
            await createNewVariable(filteredVariables, projectId);
            setRefetch((r) => ++r);
            setDeleteModalOpen(false);
        } catch (err) {
        } finally {
            setIsDeleting(false);
        }
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
            <DeleteVariablePrompt
                isOpen={deleteModalOpen}
                closeModal={() => setDeleteModalOpen(false)}
                isDeletingVariable={isDeleting}
                deleteVariable={handleDeleteVariable}
            />
            <Card>
                <CardHeader>
                    <CardTitle>
                        <h3>{t("widget.VariableWidget.title", "Variables")}</h3>
                    </CardTitle>
                    <CardOptions>
                        <IconButton
                            name={"item-add-artefact"}
                            text={t("widget.VariableWidget.actions.add", "Add")}
                            onClick={() => handleModalOpen()}
                        />
                    </CardOptions>
                </CardHeader>
                <Divider />
                <CardContent>
                    {loadingVariables ? (
                        <Loading />
                    ) : !variables.length ? (
                        <p>No Variables set</p>
                    ) : (
                        <PropertyValueList>
                            {variables.map((variable, i) => (
                                <PropertyValuePair hasDivider key={variable.name}>
                                    <PropertyName>
                                        <OverviewItem>
                                            <OverviewItemDescription>
                                                <OverviewItemLine>
                                                    <OverflowText>{variable.name}</OverflowText>
                                                </OverviewItemLine>
                                            </OverviewItemDescription>
                                        </OverviewItem>
                                    </PropertyName>
                                    <PropertyValue>
                                        <OverviewItem>
                                            <OverviewItemDescription>
                                                <OverviewItemLine>
                                                    <code>{variable.value}</code>
                                                    {variable.description && (
                                                        <>
                                                            <Spacing vertical size="large" hasDivider />
                                                            <Icon
                                                                name="item-info"
                                                                small
                                                                tooltipText={variable.description}
                                                            />
                                                        </>
                                                    )}
                                                </OverviewItemLine>
                                            </OverviewItemDescription>
                                            <OverviewItemActions>
                                                <IconButton
                                                    small
                                                    name="item-edit"
                                                    onClick={() => handleModalOpen(variable)}
                                                />
                                                <Spacing vertical hasDivider />
                                                <IconButton
                                                    small
                                                    name="item-remove"
                                                    onClick={() => handleDeleteModalOpen(variable)}
                                                    disruptive
                                                />
                                            </OverviewItemActions>
                                        </OverviewItem>
                                    </PropertyValue>
                                </PropertyValuePair>
                            ))}
                        </PropertyValueList>
                    )}
                </CardContent>
            </Card>
        </>
    );
};

export default VariablesWidget;
