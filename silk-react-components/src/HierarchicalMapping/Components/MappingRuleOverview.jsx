
/*
 Whole overview over a hierarchical Mapping on the right, header may be defined here, loops over MappingRule
 */

import React from 'react';
import {UseMessageBus} from 'ecc-mixins';
import MappingRule from './MappingRule';
import {Spinner} from 'ecc-gui-elements';
import hierarchicalMappingChannel from '../store';
import _ from 'lodash';

const MappingRuleOverview = React.createClass({

    mixins: [UseMessageBus],

    // define property types
    propTypes: {
        //apiBase: React.PropTypes.string.isRequired, // used restApi url
        //project: React.PropTypes.string.isRequired, // used project name
        //transformationTask: React.PropTypes.string, // used transformation
        currentRuleId: React.PropTypes.string, // selected rule id
    },

    // initilize state
    getInitialState() {
        return {
            loading: true,
            ruleData: {},
        };
    },

    componentDidMount() {
        hierarchicalMappingChannel.request(
            {
                topic: 'rule.get',
                data: {
                    id: this.props.currentRuleId,
                }
            }
        )
        .subscribe(
            ({rule}) => {
                this.setState({
                    loading: false,
                    ruleData: rule,
                });
            },
            (err) => {
                console.warn('err MappingRuleOverview: rule.get');
                this.setState({loading: false});
            }
        );
    },

    // template rendering
    render () {
        const {
            name,
            id,
            typeRules,
            uriRule,
        } = this.state.ruleData;

        const loading = this.state.loading ? <Spinner /> : false;

        const mappingRulesHead = (
            !_.isEmpty(this.state.ruleData.typeRules) ? (
                <div
                    className="ecc-component-hierarchicalMapping__content-mappingRuleOverview__header"
                >
                    Configuration: {name}
                    <br/>
                    Entity types: {_.map(typeRules, (rule = {}) => (rule.name))}
                    <br/>
                    URI template: {uriRule.pattern}
                </div>

            ) : (
                false
            )
        );

        const mappingRulesTable = (
            !_.isEmpty(this.state.ruleData) ? (
                <table
                    className="mdl-data-table ecc-component-hierarchicalMapping__content-mappingRuleOverview__body"
                >
                    <thead>
                        <tr>
                            <th>Rule type</th>
                            <th>Source</th>
                            <th>Target property</th>
                            <th>Target type</th>
                            <th>Actions</th>
                        </tr>
                    </thead>

                        {
                            _.map(this.state.ruleData.rules, (rule, idx) =>
                                (
                                    <MappingRule key={'MappingRule_' + idx} {...rule}/>
                                )
                            )
                        }

                </table>
            ) : false
        );

        return (
            <div
                className="ecc-component-hierarchicalMapping__content-mappingRuleOverview"
            >
                <div className="mdl-card mdl-card--stretch mdl-shadow--2dp">
                    {loading}
                    {mappingRulesHead}
                    <div className="mdl-card__content">
                        {mappingRulesTable}
                    </div>
                </div>
            </div>
        );
    },
});

export default MappingRuleOverview;