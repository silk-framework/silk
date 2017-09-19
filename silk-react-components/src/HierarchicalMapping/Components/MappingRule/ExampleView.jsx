import React from 'react';
import {Spinner, Chip} from 'ecc-gui-elements';
import ErrorView from './ErrorView'
import _ from 'lodash';

import UseMessageBus from '../../UseMessageBusMixin';
import hierarchicalMappingChannel from '../../store';

const ExampleView = React.createClass({
    mixins: [UseMessageBus],

    // define property types
    // FIXME: check propTypes
    propTypes: {
        id: React.PropTypes.string,
        rawRule: React.PropTypes.object,
        ruleType: React.PropTypes.object,
    },
    componentDidMount() {
        if (!this.props.rawRule) {
            hierarchicalMappingChannel
                .request({
                    topic: 'rule.example',
                    data: {
                        id: this.props.id,
                    },
                })
                .subscribe(
                    ({example}) => {
                        this.setState({example});
                    },
                    error => {
                        if (__DEBUG__) {
                            console.warn('err MappingRuleOverview: rule.example');
                        }
                        this.setState({
                            error,
                        });
                    }
                );
        }
        else {
            hierarchicalMappingChannel
                .request({
                    topic: 'rule.child.example',
                    data: {
                        id: this.props.id,
                        rawRule: this.props.rawRule,
                        ruleType: this.props.ruleType,
                    },
                })
                .subscribe(
                    ({example}) => {
                        this.setState({example});
                    },
                    error => {
                        if (__DEBUG__) {
                            console.warn('err MappingRuleOverview: rule.example');
                        }
                        this.setState({
                            error,
                        });
                    }
                );
        }
    },
    getInitialState() {
        return {
            example: undefined,
            errorExpanded: false,
        };
    },
    // template rendering
    render() {
        if (_.isUndefined(this.state.example)) {
            return <Spinner />;
        } else if (this.state.error) {
            return <ErrorView
                {...this.state.error}
                />
        }

        const pathsCount = this.state.example.sourcePaths.length;

        return (
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
                {_.map(this.state.example.results, (result, index) =>
                    <tbody key={`tbody_${index}`}>
                        {_.map(
                            this.state.example.sourcePaths,
                            (sourcePath, i) =>
                                <tr
                                    key={`${index}_${sourcePath}_${i}`}
                                    id={`${index}_${sourcePath}_${i}`}>
                                    <td
                                        key="path"
                                        className="ecc-silk-mapping__rulesviewer__examples-table__path">
                                        <Chip>
                                            {sourcePath}
                                        </Chip>
                                    </td>
                                    <td
                                        key="value"
                                        className="ecc-silk-mapping__rulesviewer__examples-table__value">
                                        {_.map(
                                            result.sourceValues[i],
                                            (value, valueIndex) =>
                                                <Chip
                                                    key={`${index}_${sourcePath}_${i}_${valueIndex}`}>
                                                    {value}
                                                </Chip>
                                        )}
                                    </td>
                                    {i > 0
                                        ? false
                                        : <td
                                              key="result"
                                              className="ecc-silk-mapping__rulesviewer__examples-table__result"
                                              rowSpan={pathsCount}>
                                              {_.map(
                                                  this.state.example.results[
                                                      index
                                                  ].transformedValues,
                                                  (transformedValue, row) =>
                                                      <Chip
                                                          key={`value_${index}_${i}_${row}`}
                                                          id={`value_${index}_${i}_${row}`}>
                                                          {transformedValue}
                                                      </Chip>
                                              )}
                                          </td>}
                                </tr>
                        )}
                    </tbody>
                )}
            </table>
        );
    },
});

export default ExampleView;
