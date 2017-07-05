import React from 'react';
import UseMessageBus from '../../UseMessageBusMixin';
import {
    Spinner,
    Error,
    Chip,
} from 'ecc-gui-elements';
import hierarchicalMappingChannel from '../../store';
import _ from 'lodash';


const ExampleView = React.createClass({
    mixins: [UseMessageBus],

    // define property types
    // FIXME: check propTypes
    propTypes: {
        id: React.PropTypes.string,
    },
    componentDidMount() {
        hierarchicalMappingChannel.request(
            {
                topic: 'rule.example',
                data: {
                    id: this.props.id,
                }
            }
        )
            .subscribe(
                ({example}) => {
                    this.setState({example});
                },
                (err) => {
                    if (__DEBUG__) {
                        console.warn('err MappingRuleOverview: rule.example');
                    }
                    this.setState({
                        example: {
                            status: {
                                id: 'error',
                                msg: err.toString(),
                            }
                        }
                    });
                }
            );
    },
    getInitialState() {
        return {
            example: undefined,
            errorExpanded: false,
        };
    },
    // template rendering
    render () {
        if (_.isUndefined(this.state.example)) {
            return <Spinner/>;
        }
        else if (this.state.example.status.id !== 'success') {
            const errorClassName = this.state.errorExpanded ? '' : 'mdl-alert--narrowed';
            if (__DEBUG__) {
                const loremIpsum = require('lorem-ipsum');
                return <Error
                    border
                    className={errorClassName}
                    handlerDismiss={() => {
                        this.setState({errorExpanded: !this.state.errorExpanded})
                    }}
                    labelDismiss={this.state.errorExpanded ? 'Show less' : 'Show more'}
                    iconDismiss={this.state.errorExpanded ? 'expand_less' : 'expand_more'}
                >
                    {loremIpsum({
                        count: _.random(5, 10),
                        units: 'paragraphs'
                    })}
                </Error>;
            }
            return <Error
                border
                className={errorClassName}
                handlerDismiss={() => {
                    this.setState({errorExpanded: !this.state.errorExpanded})
                }}
                labelDismiss={this.state.errorExpanded ? 'Show less' : 'Show more'}
                iconDismiss={this.state.errorExpanded ? 'expand_less' : 'expand_more'}
            >
                {this.state.example.status.msg}
            </Error>;
        }
        else {
            const pathsCount = this.state.example.sourcePaths.length;

            return (
                <table
                    className="mdl-data-table ecc-silk-mapping__rulesviewer__examples-table"
                >
                    <thead>
                    <tr>
                        <th className="ecc-silk-mapping__rulesviewer__examples-table__path">Value path</th>
                        <th className="ecc-silk-mapping__rulesviewer__examples-table__value">Value</th>
                        <th className="ecc-silk-mapping__rulesviewer__examples-table__result">Transformed value</th>
                    </tr>
                    </thead>
                    {
                        _.map(this.state.example.results, (result, index) =>
                            <tbody key={`tbody_${index}`}>
                            {
                                _.map(this.state.example.sourcePaths, (sourcePath, i) =>
                                    <tr
                                        key={`${index}_${sourcePath}_${i}`}
                                        id={`${index}_${sourcePath}_${i}`}
                                    >
                                        <td key='path' className='ecc-silk-mapping__rulesviewer__examples-table__path'>
                                            <Chip>{sourcePath}</Chip>
                                        </td>
                                        <td key='value'
                                            className='ecc-silk-mapping__rulesviewer__examples-table__value'>
                                            {
                                            _.map(result.sourceValues[i], (value, valueIndex) =>
                                                <Chip
                                                    key={`${index}_${sourcePath}_${i}_${valueIndex}`}>{value}</Chip>
                                            )}
                                            </td>
                                        {
                                            i > 0 ? false :
                                                <td key='result'
                                                    className='ecc-silk-mapping__rulesviewer__examples-table__result'
                                                    rowSpan={pathsCount}>
                                                    {
                                                        _.map(this.state.example.results[index].transformedValues, (transformedValue, row) =>
                                                            <Chip key={`value_${index}_${i}_${row}`}
                                                                  id={`value_${index}_${i}_${row}`}>{transformedValue}</Chip>
                                                        )
                                                    }
                                                </td>
                                        }
                                    </tr>
                                )}
                            </tbody>
                        )}
                </table>
            );
        }
    }
});

export default ExampleView;
