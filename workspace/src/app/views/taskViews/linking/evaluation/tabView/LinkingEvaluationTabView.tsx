import {
    Button,
    Grid,
    GridRow,
    SearchField,
    Spacing,
    Table,
    TableBody,
    TableHead,
    TableHeader,
    TableRow,
    TableCell,
    GridColumn,
} from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";
import Pagination from "../../../../../views/shared/Pagination";
import { getLinkingEvaluations } from "./LinkingEvaluationViewUtils";
import { LinkingEvaluationResult } from "./typings";

interface LinkingEvaluationTabViewProps {
    projectId: string;
    linkingTaskId: string;
}

const LinkingEvaluationTabView: React.FC<LinkingEvaluationTabViewProps> = ({ projectId, linkingTaskId }) => {
    const [t] = useTranslation();
    const [evaluationResults, setEvaluationResults] = React.useState<Array<LinkingEvaluationResult>>([]);

    React.useEffect(() => {
        (async () => {
            setEvaluationResults((await getLinkingEvaluations(projectId, linkingTaskId))?.data.links ?? []);
        })();
    }, []);

    return (
        <Grid>
            <GridRow>
                <GridColumn small>
                    <SearchField />
                </GridColumn>
                <GridColumn small>
                    <Pagination
                        pagination={{ current: 1, total: 25, limit: 10 }}
                        pageSizes={[10, 25, 50, 100]}
                        onChangeSelect={() => {}}
                    />
                </GridColumn>
            </GridRow>
            <Spacing />
            <GridRow>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableHeader>{t("linkingEvaluationTabView.table.header.source")}</TableHeader>
                            <TableHeader>{t("linkingEvaluationTabView.table.header.target")}</TableHeader>
                            <TableHeader>{t("linkingEvaluationTabView.table.header.score")}</TableHeader>
                            <TableHeader>{t("linkingEvaluationTabView.table.header.correct")}</TableHeader>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {evaluationResults.map((evaluation) => {
                            return (
                                <TableRow>
                                    <TableCell>{evaluation.source}</TableCell>
                                    <TableCell>{evaluation.target}</TableCell>
                                    <TableCell>{Math.round(evaluation.confidence * 1000) / 10}%</TableCell>
                                    <TableCell>
                                        <Button hasStatePrimary>Accept</Button>
                                        <Spacing vertical size="tiny" />
                                        <Button hasStateWarning>Unsure</Button>
                                        <Spacing vertical size="tiny" />
                                        <Button hasStateDanger>Decline</Button>
                                    </TableCell>
                                </TableRow>
                            );
                        })}
                    </TableBody>
                </Table>
            </GridRow>
        </Grid>
    );
};

export default LinkingEvaluationTabView;
