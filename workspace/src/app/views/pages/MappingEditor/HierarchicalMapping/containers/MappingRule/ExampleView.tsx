import { IconButton, Markdown, Notification } from "@eccenca/gui-elements";
import { Chip } from "gui-elements-deprecated";
import _ from "lodash";
import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";

import ErrorView from "../../components/ErrorView";
import { InfoBox } from "../../components/InfoBox";
import { childExampleAsync, ruleExampleAsync } from "../../store";
import { MESSAGES } from "../../utils/constants";
import EventEmitter from "../../utils/EventEmitter";

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
        id: "success" | "not supported" | "empty with exceptions" | "empty" | string;
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
    const [t] = useTranslation();

    const ruleExampleFunc = rawRule ? childExampleAsync : ruleExampleAsync;
    const updateFn = () =>
        ruleExampleFunc({
            id: id,
            rawRule: rawRule,
            ruleType: ruleType,
            objectPath: objectSourcePathContext,
        }).subscribe(
            ({ example }) => {
                setExamples(example);
            },
            (error) => {
                setError(error);
            }
        );

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
            <table
                data-test-id={"example-preview-table"}
                className="mdl-data-table ecc-silk-mapping__rulesviewer__examples-table"
            >
                <thead>
                    <tr>
                        <th className="ecc-silk-mapping__rulesviewer__examples-table__path">Value path</th>
                        <th className="ecc-silk-mapping__rulesviewer__examples-table__value">Value</th>
                        <th className="ecc-silk-mapping__rulesviewer__examples-table__result">Transformed value</th>
                    </tr>
                </thead>
                {_.map(examples.results, (result, index) => (
                    <tbody key={`tbody_${index}`}>
                        {sourcePaths.map((sourcePath, i) => (
                            <tr key={`${index}_${sourcePath}_${i}`} id={`${index}_${sourcePath}_${i}`}>
                                <td key="path" className="ecc-silk-mapping__rulesviewer__examples-table__path">
                                    {sourcePath ? <Chip>&lrm;{sourcePath}&lrm;</Chip> : false}
                                </td>
                                <td key="value" className="ecc-silk-mapping__rulesviewer__examples-table__value">
                                    {_.map(result.sourceValues[i], (value, valueIndex) => (
                                        <Chip key={`${index}_${sourcePath}_${i}_${valueIndex}`}>{value}</Chip>
                                    ))}
                                </td>
                                {i > 0 ? (
                                    false
                                ) : (
                                    <td
                                        key="result"
                                        className="ecc-silk-mapping__rulesviewer__examples-table__result"
                                        rowSpan={pathsCount}
                                    >
                                        {_.map(examples.results[index].transformedValues, (transformedValue, row) => (
                                            <Chip key={`value_${index}_${i}_${row}`} id={`value_${index}_${i}_${row}`}>
                                                {transformedValue}
                                            </Chip>
                                        ))}
                                    </td>
                                )}
                            </tr>
                        ))}
                    </tbody>
                ))}
            </table>
        </InfoBox>
    );
};

export default ExampleView;
