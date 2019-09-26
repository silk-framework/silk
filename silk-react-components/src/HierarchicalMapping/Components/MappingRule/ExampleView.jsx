import React from 'react';
import PropTypes from 'prop-types';

import { Chip, Spinner } from '@eccenca/gui-elements';
import ErrorView from './ErrorView';
import _ from 'lodash';

import { childExampleAsync, ruleExampleAsync } from '../../store';
import { InfoBox } from './SharedComponents';

export class ExampleView extends React.Component {
    static propTypes = {
        id: PropTypes.string,
        rawRule: PropTypes.object,
        ruleType: PropTypes.object,
    };

    state = {
        example: undefined,
        errorExpanded: false,
    };

    componentDidMount() {
        const ruleExampleFunc = this.props.rawRule ? childExampleAsync : ruleExampleAsync;
        ruleExampleFunc({
            id: this.props.id,
            rawRule: this.props.rawRule,
            ruleType: this.props.ruleType,
        }).subscribe(
            ({ example }) => {
                this.setState({ example });
            },
            error => {
                if (__DEBUG__) {
                    console.warn('err MappingRuleOverview: rule.example');
                }
                this.setState({ error });
            }
        );
    }

    // template rendering
    render() {
        if (this.state.error) {
            return <ErrorView {...this.state.error} />;
        }

        if (_.isUndefined(this.state.example)) {
            return <div />;
        }

        const pathsCount = _.size(this.state.example.sourcePaths);
        const resultsCount = _.size(this.state.example.results);

        if (pathsCount === 0 && resultsCount === 0) {
            return false;
        }

        const sourcePaths =
            pathsCount === 0 ? [''] : this.state.example.sourcePaths;

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
                    {_.map(this.state.example.results, (result, index) => (
                        <tbody key={`tbody_${index}`}>
                            {_.map(sourcePaths, (sourcePath, i) => (
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
                                                this.state.example.results[
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
}

export default ExampleView;
