import React, {useEffect, useState} from 'react';

import {Chip} from 'gui-elements-deprecated';
import ErrorView from '../../components/ErrorView';
import _ from 'lodash';

import {childExampleAsync, ruleExampleAsync} from '../../store';
import {InfoBox} from '../../components/InfoBox';
import {isDebugMode} from '../../utils/isDebugMode';
import {Notification} from "@eccenca/gui-elements";

interface IProps {
    id: string
    rawRule?: object
    ruleType: string
    // An additional path in which context the examples should be generated, e.g. needed in the creation of an object rule when a source path is specified.
    objectSourcePathContext?: string
    // The number of milliseconds to wait before updating the example view on changes. Changes are aggregated and only one request will be send if changes happen below this delay.
    updateDelay?: number
}
/** Shows example input and output values for a mapping rule. */
export const ExampleView = ({id, rawRule, ruleType, objectSourcePathContext, updateDelay = 500}: IProps) => {
    const [example, setExample] = useState<any>(undefined)
    const [error, setError] = useState<any>(undefined)

    useEffect(() => {
        const ruleExampleFunc = rawRule ? childExampleAsync : ruleExampleAsync;
        const updateFn = () => ruleExampleFunc({
            id: id,
            rawRule: rawRule,
            ruleType: ruleType,
            objectPath: objectSourcePathContext
        }).subscribe(
            ({example}) => {
                setExample(example);
            },
            error => {
                isDebugMode('err MappingRuleOverview: rule.example');
                setError(error);
            }
        )
        if(updateDelay > 0) {
            const timeoutId = setTimeout(updateFn, updateDelay)
            return () => clearTimeout(timeoutId)
        } else {
            updateFn()
        }
    }, [id, objectSourcePathContext, ruleType, rawRule])

    if (error) {
        return <ErrorView {...error} titlePrefix={"There has been an error loading the examples: "}/>;
    }

    if (_.isUndefined(example)) {
        return <div/>;
    }

    const pathsCount = _.size(example.sourcePaths);
    const resultsCount = _.size(example.results);

    if (resultsCount === 0) {
        return <Notification>Preview has returned no results.</Notification>
    }

    const sourcePaths =
        pathsCount === 0 ? [''] : example.sourcePaths;

    return (
        <InfoBox>
            <table data-test-id={"example-preview-table"} className="mdl-data-table ecc-silk-mapping__rulesviewer__examples-table">
                <thead>
                <tr>
                    <th className="ecc-silk-mapping__rulesviewer__examples-table__path">
                        Value path
                    </th>
                    <th className="ecc-silk-mapping__rulesviewer__examples-table__value">
                        Value
                    </th>
                    <th className="ecc-silk-mapping__rulesviewer__examples-table__result">
                        Transformed value
                    </th>
                </tr>
                </thead>
                {_.map(example.results, (result, index) => (
                    <tbody key={`tbody_${index}`}>
                    {sourcePaths.map((sourcePath, i) => (
                        <tr
                            key={`${index}_${sourcePath}_${i}`}
                            id={`${index}_${sourcePath}_${i}`}
                        >
                            <td
                                key="path"
                                className="ecc-silk-mapping__rulesviewer__examples-table__path"
                            >
                                {sourcePath ? (
                                    <Chip>&lrm;{sourcePath}&lrm;</Chip>
                                ) : (
                                    false
                                )}
                            </td>
                            <td
                                key="value"
                                className="ecc-silk-mapping__rulesviewer__examples-table__value"
                            >
                                {_.map(
                                    result.sourceValues[i],
                                    (value, valueIndex) => (
                                        <Chip
                                            key={`${index}_${sourcePath}_${i}_${valueIndex}`}
                                        >
                                            {value}
                                        </Chip>
                                    )
                                )}
                            </td>
                            {i > 0 ? (
                                false
                            ) : (
                                <td
                                    key="result"
                                    className="ecc-silk-mapping__rulesviewer__examples-table__result"
                                    rowSpan={pathsCount}
                                >
                                    {_.map(
                                        example.results[
                                            index
                                            ].transformedValues,
                                        (transformedValue, row) => (
                                            <Chip
                                                key={`value_${index}_${i}_${row}`}
                                                id={`value_${index}_${i}_${row}`}
                                            >
                                                {transformedValue}
                                            </Chip>
                                        )
                                    )}
                                </td>
                            )}
                        </tr>
                    ))}
                    </tbody>
                ))}
            </table>
        </InfoBox>
    );
}

export default ExampleView;
