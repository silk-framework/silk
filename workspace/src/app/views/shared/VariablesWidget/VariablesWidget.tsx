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
    IconButton,
    OverflowText,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDescription,
    OverviewItemLine,
    OverviewItemList,
} from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { getVariables } from "./requests";
import useErrorHandler from "../../../hooks/useErrorHandler";
import Loading from "../Loading";
import VariableModal from "./VariableModal";

const VariablesWidget: React.FC<VariableWidgetProps> = ({ projectId, taskId }) => {
    const { registerError } = useErrorHandler();
    const [loadingVariables, setLoadingVariables] = React.useState<boolean>(false);
    const [variables, setVariables] = React.useState<Array<Variable>>([]);
    const [selectedVariable, setSelectedVariable] = React.useState<Variable>();
    const [modalOpen, setModalOpen] = React.useState<boolean>(false);
    const [refetch, setRefetch] = React.useState<number>(0);
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

    return (
        <>
            <VariableModal
                modalOpen={modalOpen}
                closeModal={() => setModalOpen(false)}
                variables={variables}
                targetVariable={selectedVariable}
                projectId={projectId}
                taskId={taskId}
                refresh={() => setRefetch((r) => ++r)}
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
                        <OverviewItemList hasDivider>
                            {variables.map((variable, i) => (
                                <OverviewItem key={i}>
                                    <OverviewItemLine>
                                        <OverflowText>{variable.name}</OverflowText>
                                    </OverviewItemLine>
                                    <OverviewItemDescription>
                                        <OverviewItemLine>
                                            <code>{variable.value}</code>
                                        </OverviewItemLine>
                                        <OverviewItemLine>
                                            <OverflowText>{variable.description}</OverflowText>
                                        </OverviewItemLine>
                                    </OverviewItemDescription>
                                    <OverviewItemActions>
                                        <IconButton
                                            name="item-edit"
                                            text={t("widget.VariableWidget.actions.add", "Edit")}
                                            onClick={() => handleModalOpen(variable)}
                                        />
                                    </OverviewItemActions>
                                </OverviewItem>
                            ))}
                        </OverviewItemList>
                    )}
                </CardContent>
            </Card>
        </>
    );
};

export default VariablesWidget;
