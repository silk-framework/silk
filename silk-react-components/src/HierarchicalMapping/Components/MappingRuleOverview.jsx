/*
 Whole overview over a hierarchical Mapping on the right, header may be defined here, loops over MappingRule
 */

import React from 'react';
import UseMessageBus from '../UseMessageBusMixin';
import hierarchicalMappingChannel from '../store';
import _ from 'lodash';
import MappingRuleOverviewHeader from './MappingRuleOverviewHeader';
import MappingRule from './MappingRule';
import {Spinner, Info, ContextMenu, MenuItem} from 'ecc-gui-elements';

const MappingRuleOverview = React.createClass({

    mixins: [UseMessageBus],

    // define property types
    propTypes: {
        //apiBase: React.PropTypes.string.isRequired, // used restApi url
        //project: React.PropTypes.string.isRequired, // used project name
        //transformationTask: React.PropTypes.string, // used transformation
        currentRuleId: React.PropTypes.string, // selected rule id
        //createRuleForm,
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
        const createRuleForm = this.props.createRuleForm;
        const childRules = rules.propertyRules || [];

        const loading = this.state.loading ? <Spinner /> : false;

        const mappingRulesListHead = (
            <div className="mdl-card__title mdl-card--border">
                <div className="mdl-card__title-text">
                    Mapping rules {`(${childRules.length})`}
                </div>
                <ContextMenu
                    className="ecc-silk-mapping__ruleslistmenu"
                >
                    <MenuItem
                        className="ecc-silk-mapping__ruleslistmenu__item-add-value"
                        onClick={() => {
                            this.handleCreate({type: 'direct'});
                        }}
                    >
                        Add value mapping
                    </MenuItem>
                    <MenuItem
                        className="ecc-silk-mapping__ruleslistmenu__item-add-object"
                        onClick={() => {
                            this.handleCreate({type: 'object'});
                        }}
                    >
                        Add object mapping
                    </MenuItem>
                    <MenuItem
                        className="ecc-silk-mapping__ruleslistmenu__item-autosuggest"
                    >
                        Suggest rules (0) (TODO)
                    </MenuItem>
                    <MenuItem
                        className="ecc-silk-mapping__ruleslistmenu__item-expand"
                        onClick={() => {
                            this.handleToggleRuleDetails({expanded: true})
                        }}
                    >
                        Expand all
                    </MenuItem>
                    <MenuItem
                        className="ecc-silk-mapping__ruleslistmenu__item-reduce"
                        onClick={() => {
                            this.handleToggleRuleDetails({expanded: false})
                        }}
                    >
                        Reduce all
                    </MenuItem>
                </ContextMenu>
            </div>

        );
        const form = createRuleForm
            ?   <li>{createRuleForm}</li>
            :   false;

        const mappingRulesList = (
            _.isEmpty(childRules) ? (
                <div className="mdl-card__content">
                    <Info vertSpacing border>
                        No existing mapping rules.
                    </Info>
                    {
                        /* TODO: we should provide options like adding rules or suggestions here,
                           even a help text would be a good support for the user.
                        */
                     }
                </div>
            ) : (
                <ol className="mdl-list">
                    {form}
                    {
                        _.map(childRules, (rule, idx) =>
                            (
                                <MappingRule
                                    pos={idx}
                                    count={childRules.length}
                                    key={`MappingRule_${id}_${idx}`}
                                    {...rule}
                                />
                            )
                        )
                    }
                </ol>
            )
        );

        return (
            <div className="ecc-silk-mapping__rules">
                {loading}
                <MappingRuleOverviewHeader rule={this.state.ruleData} key={id}/>
                <div className="ecc-silk-mapping__ruleslist">
                    <div className="mdl-card mdl-card--stretch mdl-shadow--2dp">
                        {mappingRulesListHead}
                        {mappingRulesList}
                    </div>
                </div>
            </div>
        );
    },
});

export default MappingRuleOverview;
