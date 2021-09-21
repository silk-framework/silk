import React, {useEffect, useState} from 'react';
import PropTypes from 'prop-types';

import { Chip, Spinner } from '@eccenca/gui-elements';
import ErrorView from '../../components/ErrorView';
import _ from 'lodash';

import { childExampleAsync, ruleExampleAsync } from '../../store';
import { InfoBox } from '../../components/InfoBox';
import { isDebugMode } from '../../utils/isDebugMode';

interface IProps {
    id: string
    rawRule?: object
    ruleType: string
}
/** Shows example input and output values for a mapping rule. */
export const ExampleView = ({id, rawRule, ruleType}: IProps) => {
    const [example, setExample] = useState<any>(undefined)
    const [error, setError] = useState<any>(undefined)

    useEffect(() => {
        const ruleExampleFunc = rawRule ? childExampleAsync : ruleExampleAsync;
        ruleExampleFunc({
            id: id,
            rawRule: rawRule,
            ruleType: ruleType,
        }).subscribe(
            ({ example }) => {
                setExample(example);
            },
            error => {
                isDebugMode('err MappingRuleOverview: rule.example');
                setError(error);
            }
        );
    }, [])

    if (error) {
        return <ErrorView {...error} titlePrefix={"There has been an error loading the examples: "}/>;
    }

    if (_.isUndefined(example)) {
        return <div/>;
    }

    const pathsCount = _.size(example.sourcePaths);
    const resultsCount = _.size(example.results);

    if (pathsCount === 0 && resultsCount === 0) {
        return null;
    }

    const sourcePaths =
        pathsCount === 0 ? [''] : example.sourcePaths;

    return (
        <InfoBox>
            <table className="mdl-data-table ecc-silk-mapping__rulesviewer__examples-table">
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
                                    <Chip>&lrm;{sourcePath}</Chip>
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
