/*
 Whole overview over a hierarchical Mapping on the right, header may be defined here, loops over MappingRule
 */

import React from 'react';
import UseMessageBus from '../UseMessageBusMixin';
import MappingRule from './MappingRule';
import {Spinner, Info, ContextMenu, MenuItem} from 'ecc-gui-elements';
import hierarchicalMappingChannel from '../store';
import _ from 'lodash';
import MappingRuleOverviewHeader from './MappingRuleOverviewHeader';

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
        this.subscribe(hierarchicalMappingChannel.subject('reload'), this.loadData);
        return {
            loading: true,
            ruleData: {},
        };
    },
    componentDidMount() {
        this.loadData();
    },
    componentDidUpdate(prevProps) {
        if (prevProps.currentRuleId !== this.props.currentRuleId) {
            this.loadData();
        }
    },
    loadData() {
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
            rules = {},
            id,
        } = this.state.ruleData;

        const childRules = rules.propertyRules || [];

        const loading = this.state.loading ? <Spinner /> : false;

        const mappingRulesListHead = (
            <div>
                Mapping rules {`(${childRules.length})`}
                <ContextMenu
                >
                    <MenuItem
                        className="ecc-component-hierarchicalMapping__content-mappingRuleOverview__head__context-add-value-mapping"
                        onClick={() => {
                            this.handleCreate({type: 'direct'});
                        }}
                    >
                        Add value mapping
                    </MenuItem>
                    <MenuItem
                        className="ecc-component-hierarchicalMapping__content-mappingRuleOverview__head__context-add-object-mapping"
                        onClick={() => {
                            this.handleCreate({type: 'object'});
                        }}
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
                        onClick={() => {
                            this.handleToggleRuleDetails({expanded: true})
                        }}
                    >
                        Expand all
                    </MenuItem>
                    <MenuItem
                        className="ecc-component-hierarchicalMapping__content-mappingRuleOverview__head__context-reduce-mappings"
                        onClick={() => {
                            this.handleToggleRuleDetails({expanded: false})
                        }}
                    >
                        Reduce all
                    </MenuItem>
                </ContextMenu>
            </div>

        );

        const mappingRulesList = (
            _.isEmpty(childRules) ? (
                <Info vertSpacing border>
                    No existing mapping rules.
                </Info>
            ) : (
                _.map(childRules, (rule, idx) =>
                    (
                        <MappingRule key={`MappingRule_${id}_${idx}`} {...rule}/>
                    )
                )
            )
        );

        return (
            <div
                className="ecc-component-hierarchicalMapping__content-mappingRuleOverview"
            >
                {loading}
                <MappingRuleOverviewHeader rule={this.state.ruleData} key={id}/>
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
