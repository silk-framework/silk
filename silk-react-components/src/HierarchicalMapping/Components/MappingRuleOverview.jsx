
/*
 Whole overview over a hierarchical Mapping on the right, header may be defined here, loops over MappingRule
 */

import React from 'react';
import {UseMessageBus} from 'ecc-mixins';
import MappingRule from './MappingRule';
import {Spinner, Info, ContextMenu, MenuItem} from 'ecc-gui-elements';
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
    // sends event to expand / collapse all mapping rules
    handleToggleRuleDetails({expanded}) {
        hierarchicalMappingChannel.subject('rulesView.toggle').onNext({expanded});
    },
    // jumps to selected rule as new center of view
    handleCreate({type}) {
        hierarchicalMappingChannel.subject('ruleId.create').onNext({
            type,
            //FIXME: do we need more data like id of parent as source?
        });
    },

    // template rendering
    render () {

        const {
            name,
            id,
            typeRules,
            uriRule,
            rules = [],
        } = this.state.ruleData;

        const loading = this.state.loading ? <Spinner /> : false;

        const mappingRulesOverview = (
            !_.isEmpty(this.state.ruleData.typeRules) ? (
                <div
                    className="ecc-component-hierarchicalMapping__content-mappingRuleOverview__header"
                >
                    <b>Configuration: {name}</b>
                    <br/>
                    Entity types: {_.map(typeRules, (rule = {}) => (rule.name))}
                    <br/>
                    URI template: {uriRule.pattern}
                </div>

            ) : (
                false
            )
        );

        const mappingRulesListHead = (
            <div>
                Mapping rules {`(${rules.length})`}
                <ContextMenu
                >
                    <MenuItem
                        className="ecc-component-hierarchicalMapping__content-mappingRuleOverview__head__context-add-value-mapping"
                        onClick={() => {this.handleCreate({type: 'value'}); }}
                    >
                        Add value mapping
                    </MenuItem>
                    <MenuItem
                        className="ecc-component-hierarchicalMapping__content-mappingRuleOverview__head__context-add-object-mapping"
                        onClick={() => {this.handleCreate({type: 'object'}); }}
                    >
                        Add object mapping
                    </MenuItem>
                    <MenuItem
                        className="ecc-component-hierarchicalMapping__content-mappingRuleOverview__head__context-suggest-mappings"
                    >
                        Suggest rules (0) (TODO)
                    </MenuItem>
                    <MenuItem
                        className="ecc-component-hierarchicalMapping__content-mappingRuleOverview__head__context-expand-mappings"
                        onClick={() => {this.handleToggleRuleDetails({expanded: true})}}
                    >
                        Expand all
                    </MenuItem>
                    <MenuItem
                        className="ecc-component-hierarchicalMapping__content-mappingRuleOverview__head__context-reduce-mappings"
                        onClick={() => {this.handleToggleRuleDetails({expanded: false})}}
                    >
                        Reduce all
                    </MenuItem>
                </ContextMenu>
            </div>

        );

        const mappingRulesList = (
            _.isEmpty(rules) ? (
                <Info vertSpacing border>
                    No existing mapping rules.
                </Info>
            ) : (
                _.map(rules, (rule, idx) =>
                    (
                        <MappingRule key={'MappingRule_' + idx} {...rule}/>
                    )
                )
            )
        );

        return (
            <div
                className="ecc-component-hierarchicalMapping__content-mappingRuleOverview"
            >
                {loading}
                <div className="mdl-card mdl-card--stretch mdl-shadow--2dp">
                    <div className="mdl-card__content">
                        {mappingRulesOverview}
                    </div>
                </div>
                <br/>
                <div className="mdl-card mdl-card--stretch mdl-shadow--2dp">
                    <div className="mdl-card__content">
                        <div className="ecc-component-hierarchicalMapping__content-mappingRuleOverview__head">
                            {mappingRulesListHead}
                        </div>
                        <div className="ecc-component-hierarchicalMapping__content-mappingRuleOverview__body">
                            {mappingRulesList}
                        </div>
                    </div>
                </div>
            </div>
        );
    },
});

export default MappingRuleOverview;