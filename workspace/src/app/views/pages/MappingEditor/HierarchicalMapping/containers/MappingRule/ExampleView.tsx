import React, { useEffect, useState } from "react";

import ErrorView from "../../components/ErrorView";
import _ from "lodash";

import { childExampleAsync, ruleExampleAsync } from "../../store";
import { InfoBox } from "../../components/InfoBox";
import {
    IconButton,
    Markdown,
    Notification,
    Spinner,
    Toolbar,
    ToolbarSection,
    Tag,
    Table,
    TableContainer,
    TableBody,
    TableHeader,
    TableHead,
    TableRow,
    TableCell,
} from "@eccenca/gui-elements";
import EventEmitter from "../../utils/EventEmitter";
import { MESSAGES } from "../../utils/constants";
import { useTranslation } from "react-i18next";

interface IProps {
    id: string;
    rawRule?: object;
    ruleType: string;
    // An additional path in which context the examples should be generated, e.g. needed in the creation of an object rule when a source path is specified.
    objectSourcePathContext?: string;
    // The number of milliseconds to wait before updating the example view on changes. Changes are aggregated and only one request will be send if changes happen below this delay.
    updateDelay?: number;
}

interface RuleExamples {
    sourcePaths: string[][];
    results: RuleExample[];
    status: {
        id: "success" | "with exceptions" | "not supported" | "empty with exceptions" | "empty" | string;
        msg?: string;
    };
}

interface RuleExample {
    sourceValues: string[][];
    transformedValues: string[];
}

/** Shows example input and output values for a mapping rule. */
export const ExampleView = ({ id, rawRule, ruleType, objectSourcePathContext, updateDelay = 500 }: IProps) => {
    const [examples, setExamples] = useState<RuleExamples | undefined>(undefined);
    // Show message details
    const [showDetails, setShowDetails] = useState(false);
    const [error, setError] = useState<any>(undefined);
    const [loading, setLoading] = useState(true);
    const [t] = useTranslation();

    const ruleExampleFunc = rawRule ? childExampleAsync : ruleExampleAsync;
    const updateFn = () => {
        setLoading(true);
        ruleExampleFunc({
            id: id,
            rawRule: rawRule,
            ruleType: ruleType,
            objectPath: objectSourcePathContext,
        }).subscribe(
            ({ example }) => {
                setExamples(example);
                setLoading(false);
            },
            (error) => {
                setError(error);
                setLoading(false);
            }
        );
    };

    useEffect(() => {
        EventEmitter.on(MESSAGES.RELOAD, updateFn);
        return () => {
            EventEmitter.off(MESSAGES.RELOAD, updateFn);
        };
    });

    useEffect(() => {
        if (updateDelay > 0) {
            const timeoutId = setTimeout(updateFn, updateDelay);
            return () => clearTimeout(timeoutId);
        } else {
            updateFn();
        }
    }, [id, objectSourcePathContext, ruleType, rawRule]);

    if (loading) {
        return (
            <Toolbar>
                <ToolbarSection>
                    <Spinner position={"local"} stroke={"thin"} delay={10} />
                </ToolbarSection>
                <ToolbarSection canGrow={true}></ToolbarSection>
            </Toolbar>
        );
    }

    if (error) {
        return <ErrorView {...error} titlePrefix={"There has been an error loading the examples: "} />;
    }

    if (_.isUndefined(examples)) {
        return <div />;
    }

    const pathsCount = _.size(examples.sourcePaths);
    const resultsCount = _.size(examples.results);
    const handleToggleDetails = () => {
        setShowDetails((current) => !current);
    };
    const ProblemNotification = ({ message, details }: { message: string; details?: string }) => {
        const detailMessage = showDetails && details ? details : undefined;
        return (
            <Notification
                warning={true}
                actions={
                    details ? (
                        <IconButton
                            name={showDetails ? "toggler-showless" : "toggler-showmore"}
                            onClick={details ? handleToggleDetails : undefined}
                        />
                    ) : undefined
                }
                onClick={details ? handleToggleDetails : undefined}
                message={detailMessage ? <Markdown>{message + "\n\n`" + detailMessage + "`"}</Markdown> : undefined}
            >
                {detailMessage ? "" : message}
            </Notification>
        );
    };

    if (examples.status.id === "not supported") {
        return (
            <ProblemNotification
                message={t("HierarchicalMapping.ExampleView.errors.notSupported")}
                details={examples.status.msg}
            />
        );
    } else if (resultsCount === 0 && examples.status.id === "empty with exceptions") {
        return (
            <ProblemNotification
                message={t("HierarchicalMapping.ExampleView.errors.emptyWithExceptions")}
                details={examples.status.msg}
            />
        );
    } else if (resultsCount === 0) {
        return (
            <ProblemNotification
                message={t("HierarchicalMapping.ExampleView.errors.emptyResult")}
                details={examples.status.msg}
            />
        );
    }

    const sourcePaths = pathsCount === 0 ? [""] : examples.sourcePaths;

    return (
        <InfoBox>
            <TableContainer className="ecc-silk-mapping__rulesviewer__examples-table">
                <Table data-test-id={"example-preview-table"}>
                    <TableHead>
                        <TableRow>
                            <TableHeader className="ecc-silk-mapping__rulesviewer__examples-table__path">
                                Value path
                            </TableHeader>
                            <TableHeader className="ecc-silk-mapping__rulesviewer__examples-table__value">
                                Value
                            </TableHeader>
                            <TableHeader className="ecc-silk-mapping__rulesviewer__examples-table__result">
                                Transformed value
                            </TableHeader>
                        </TableRow>
                    </TableHead>
                    {resultsCount > 0 && examples.status.id === "with exceptions" && examples.status.msg ?
                        <TableBody>
                            <TableRow key={"errorRow"}>
                                <TableCell colSpan={3}>
                                    <ProblemNotification
                                        message={t("HierarchicalMapping.ExampleView.errors.withExceptions", {error: examples.status.msg})}
                                        details={examples.status.msg}
                                    />
                                </TableCell>
                            </TableRow>
                        </TableBody> :
                        null
                    }
                    {_.map(examples.results, (result, index) => (
                        <TableBody key={`tbody_${index}`}>
                            {sourcePaths.map((sourcePath, i) => (
                                <TableRow key={`${index}_${sourcePath}_${i}`} id={`${index}_${sourcePath}_${i}`}>
                                    <TableCell
                                        key="path"
                                        className="ecc-silk-mapping__rulesviewer__examples-table__path"
                                    >
                                        {sourcePath ? <Tag round>&lrm;{sourcePath}&lrm;</Tag> : false}
                                    </TableCell>
                                    <TableCell
                                        key="value"
                                        className="ecc-silk-mapping__rulesviewer__examples-table__value"
                                    >
                                        {_.map(result.sourceValues[i], (value, valueIndex) => (
                                            <Tag round key={`${index}_${sourcePath}_${i}_${valueIndex}`}>
                                                {value}
                                            </Tag>
                                        ))}
                                    </TableCell>
                                    {i > 0 ? (
                                        false
                                    ) : (
                                        <TableCell
                                            key="result"
                                            className="ecc-silk-mapping__rulesviewer__examples-table__result"
                                            rowSpan={pathsCount}
                                        >
                                            {_.map(
                                                examples.results[index].transformedValues,
                                                (transformedValue, row) => (
                                                    <Tag
                                                        round
                                                        key={`value_${index}_${i}_${row}`}
                                                        id={`value_${index}_${i}_${row}`}
                                                    >
                                                        {transformedValue}
                                                    </Tag>
                                                )
                                            )}
                                        </TableCell>
                                    )}
                                </TableRow>
                            ))}
                        </TableBody>
                    ))}
                </Table>
            </TableContainer>
        </InfoBox>
    );
};

export default ExampleView;
