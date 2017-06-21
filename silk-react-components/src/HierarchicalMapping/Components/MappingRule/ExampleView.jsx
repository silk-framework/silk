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
                    console.warn('err MappingRuleOverview: rule.example');
                    this.setState({example: {
                        status: {
                            id: 'error',
                            msg: err.toString(),
                        }
                    }});
                }
            );
    },
    getInitialState() {
        return {
            example: undefined,
        };
    },
    // template rendering
    render () {
        console.warn(JSON.stringify(this.state.example, null, 2));
        if (_.isUndefined(this.state.example)) {
            return <Spinner/>;
        }
        else if (this.state.example.status.id !== 'success') {
            return <Error>{this.state.example.status.msg}</Error>;
        }
        else {
            // FIXME: the table requires some beauty.
            const pathsCount = this.state.example.sourcePaths.length;
            return <div className="di-rule__expanded-example-values-container">
                <table
                    id='myRule'
                    className="mdl-data-table mdl-js-data-table di-rule__expanded-example-values"
                    style={{width: '100%'}}
                >
                    <thead>
                    <tr>
                        <th className="mdl-data-table__cell--non-numeric di-example-values-source-value">Source Path</th>
                        <th className="mdl-data-table__cell--non-numeric di-example-values-source-value">Source Value</th>
                        <th className="mdl-data-table__cell--non-numeric di-example-values-transformed-value">Transformed Value</th>
                    </tr>
                    </thead>
                    <tbody>
                    {_.map(this.state.example.results, (result, index) =>
                        _.map(this.state.example.sourcePaths, (sourcePath, i) =>
                            <tr
                                key={`${index}_${i}`}
                                id={`${index}_${i}`}
                                className={i===0 ? 'di-rule-first-path' : (i===pathsCount-1 ? 'di-rule-last-path' : '')}
                            >
                                <td key='xxx1' className="mdl-data-table__cell--non-numeric">
                                    <Chip>{sourcePath}</Chip>
                                </td>
                                <td key='xxx2' className="mdl-data-table__cell--non-numeric">{result.sourceValues[i].map(t => <Chip>{t}</Chip>)}</td>
                                {
                                    i>0 ? false :
                                        <td key='xxx3' className="mdl-data-table__cell--non-numeric" rowSpan={pathsCount}>
                                            {
                                                this.state.example.results[index].transformedValues.map(transformedValue =>
                                                    <Chip>{transformedValue}</Chip>
                                                )
                                            }
                                        </td>
                                }
                            </tr>
                        )
                    )}
                    </tbody>
                </table>
            </div>
        }
    }
});

export default ExampleView;
