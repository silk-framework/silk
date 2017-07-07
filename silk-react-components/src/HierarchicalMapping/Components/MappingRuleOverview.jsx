/*
 Whole overview over a hierarchical Mapping on the right, header may be defined here, loops over MappingRule
 */

import React from 'react';
import UseMessageBus from '../UseMessageBusMixin';
import hierarchicalMappingChannel from '../store';
import _ from 'lodash';
import ObjectMappingRuleForm from './MappingRule/Forms/ObjectMappingRuleForm'
import ValueMappingRuleForm from './MappingRule/Forms/ValueMappingRuleForm'
import MappingRuleOverviewHeader from './MappingRuleOverviewHeader';
import MappingRule from './MappingRule/MappingRule';
import SuggestionOverview from './MappingRule/SuggestionOverview';
import {
    Spinner,
    Info,
    Icon,
    ContextMenu,
    MenuItem,
    ConfirmationDialog,
    DisruptiveButton,
    DismissiveButton,
    Button,
} from 'ecc-gui-elements';
import {
    FloatingListActions,
} from './MappingRule/SharedComponents';

const MappingRuleOverview = React.createClass({

    mixins: [UseMessageBus],

    // define property types
    propTypes: {
        currentRuleId: React.PropTypes.string, // selected rule id
    },
    onRuleCreate({type}) {
        this.setState({
            ruleEditView: {
                type,
            },
        });
    },
    handleRuleEditOpen({id}) {
        if (!_.includes(this.state.editing, id)) {
            this.setState({
                editing: _.concat(this.state.editing, [id]),
            });
        }
    },
    handleRuleEditClose({id}) {
        if (id === 0) {
            this.setState({
                ruleEditView: false,
                editing: _.filter(this.state.editing, (e) => e !== id),
            });
        }
        else {
            this.setState({
                editing: _.filter(this.state.editing, (e) => e !== id),
            });
        }
    },

    // initilize state
    getInitialState() {
        return {
            loading: true,
            ruleData: {},
            ruleEditView: false,
            editing: [],
            askForDiscard: false,
            showSuggestions: false,
        };
    },
    componentDidMount() {
        this.loadData();
        this.subscribe(hierarchicalMappingChannel.subject('reload'), this.loadData);
        this.subscribe(hierarchicalMappingChannel.subject('ruleId.create'), this.onRuleCreate);
        this.subscribe(hierarchicalMappingChannel.subject('ruleView.unchanged'), this.handleRuleEditClose);
        this.subscribe(hierarchicalMappingChannel.subject('ruleView.close'), this.handleRuleEditClose);
        this.subscribe(hierarchicalMappingChannel.subject('ruleView.change'), this.handleRuleEditOpen);
        this.subscribe(hierarchicalMappingChannel.subject('ruleView.discardAll'), this.discardAll);
    },
    discardAll() {
        this.setState({
            editing: [],
            showSuggestions: false,
        });
    },
    handleShowSuggestions(event) {
        event.stopPropagation();
        if (this.state.editing.length === 0) {
            this.setState({
                showSuggestions: true,

            });
            hierarchicalMappingChannel.subject('ruleView.change').onNext({id: 0});
        }
        else {
            this.setState({
                askForDiscard: {
                    suggestions: true,
                }
            })
        }
    },
    componentDidUpdate(prevProps) {
        if (prevProps.currentRuleId !== this.props.currentRuleId) {
            this.loadData();
        }
    },
    loadData() {
        this.setState({
            loading: true,
        });

        console.warn('DATA RELOAD');

        hierarchicalMappingChannel.request(
            {
                topic: 'rule.get',
                data: {
                    id: this.props.currentRuleId,
                    isObjectMapping: true,
                }
            }
        )
            .subscribe(
                ({rule}) => {

                    if(rule.id !== this.props.currentRuleId){
                        hierarchicalMappingChannel.subject('rulesView.toggle').onNext({
                            expanded:true,
                            id: this.props.currentRuleId,
                        });
                    }

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
    handleDiscardChanges(event){
        event.stopPropagation();
        const type = _.get(this.state.askForDiscard, 'type', false);
        const suggestions = _.get(this.state.askForDiscard, 'suggestions', false);
        const expanded = _.get(this.state.askForDiscard, 'expanded', false);

        if (type) {
            hierarchicalMappingChannel.subject('ruleId.create').onNext({type});
        }
        else if (suggestions) {
            this.setState({
                showSuggestions: true,
            })
        }
        else {
            hierarchicalMappingChannel.subject('rulesView.toggle').onNext({
                expanded,
                id: true,
            });
        }
        hierarchicalMappingChannel.subject('ruleView.discardAll').onNext();
        this.setState({
            askForDiscard: false,
        });

    },
    handleCancelDiscard(event) {
        event.stopPropagation();
        this.setState({
            askForDiscard: false,
        })
    },
    // sends event to expand / collapse all mapping rules
    handleToggleRuleDetails({expanded}) {
        if (this.state.editing.length === 0 || expanded) {
            hierarchicalMappingChannel.subject('rulesView.toggle').onNext({expanded, id: true});
        }
        else {
            this.setState({
                askForDiscard: {
                    expanded
                },
            });
        }
    },
    // jumps to selected rule as new center of view
    handleCreate({type}) {
        if (this.state.editing.length === 0) {
            hierarchicalMappingChannel.subject('ruleId.create').onNext({
                type,
            });
        }
        else {
            this.setState({
                askForDiscard: {
                    type
                },
            });
        }
    },
    handleCloseSuggestions() {
        this.setState({showSuggestions: false});
        hierarchicalMappingChannel.subject('ruleView.close').onNext({id:0});
    },
    shouldComponentUpdate(nextProps, nextState) {
        // Required to prevent empty redraws while not all data is there.
        // The issue is due to bad use of React ...
        return !_.isEmpty(nextState.ruleData);
    },
    // template rendering
    render () {
        const {
            rules = {},
            id,
        } = this.state.ruleData;

        const discardView = this.state.askForDiscard !== false
            ? <ConfirmationDialog
                active={true}
                modal={true}
                title="Discard changes?"
                confirmButton={
                    <DisruptiveButton disabled={false} onClick={this.handleDiscardChanges}>
                        Discard
                    </DisruptiveButton>
                }
                cancelButton={
                    <DismissiveButton onClick={this.handleCancelDiscard}>
                        Cancel
                    </DismissiveButton>
                }>
                <p>
                    You currently have unsaved changes{this.state.editing.length === 1 ? '' :
                    ` in ${this.state.editing.length} mapping rules`}.
                </p>
            </ConfirmationDialog>
            : false;

        const createType = _.get(this.state, 'ruleEditView.type', false);

        const createRuleForm = createType ? (
            <div className="ecc-silk-mapping__createrule">
                {
                    createType === 'object' ? (
                        <ObjectMappingRuleForm
                            type={createType}
                            parentId={this.state.ruleData.id}
                            parent={{
                                id: this.state.ruleData.id,
                                property: _.get(this, 'state.ruleData.mappingTarget.uri'),
                                type: _.get(this, 'state.ruleData.rules.typeRules[0].typeUri'),
                            }}
                            edit={true}
                        />
                    ) : (
                        <ValueMappingRuleForm
                            type={createType}
                            parentId={this.state.ruleData.id}
                            edit={true}
                        />
                    )
                }
            </div>
        ) : false;

        const childRules = rules.propertyRules || [];

        const loading = this.state.loading ? <Spinner /> : false;

        let mappingRulesListHead = false;
        let mappingRulesList = false;

        if (!createRuleForm) {
            mappingRulesListHead = (
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
                            onClick={this.handleShowSuggestions}
                        >
                            Suggest mappings
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

            mappingRulesList = (
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
                        {
                            _.map(childRules, (rule, idx) =>
                                (
                                    <MappingRule
                                        pos={idx}
                                        parentId={this.props.currentRuleId}
                                        count={childRules.length}
                                        key={`MappingRule_${rule.id}`}
                                        {...rule}
                                    />
                                )
                            )
                        }
                    </ol>
                )
            );
        }

        const rulesList = !createRuleForm && !suggestions ? <div className="ecc-silk-mapping__ruleslist">
            <div className="mdl-card mdl-card--stretch">
                {mappingRulesListHead}
                {mappingRulesList}
                <div className="mdl-card__actions--fixed">
                    <FloatingListActions
                        iconName="add"
                        actions={
                            [
                                {
                                    icon: 'insert_drive_file',
                                    label: 'Add value mapping',
                                    handler: () => {
                                        this.handleCreate({type: 'direct'});
                                    },
                                },
                                {
                                    icon: 'folder',
                                    label: 'Add object mapping',
                                    handler: () => {
                                        this.handleCreate({type: 'object'});
                                    },
                                },
                            ]
                        }
                    />
                </div>
            </div>
        </div> : false;


        const types = !createRuleForm && this.state.showSuggestions && _.has(this.state, 'ruleData.rules.typeRules')
            ? _.map(
            this.state.ruleData.rules.typeRules,
            v => v.typeUri.replace('<', '').replace('>', ''))
            : [];

        const suggestions = !createRuleForm && this.state.showSuggestions && _.has(this.state, 'ruleData.rules.typeRules')
            ? <SuggestionOverview
                key={_.join(types, ',')}
                ruleId={this.props.currentRuleId}
                onClose={this.handleCloseSuggestions}
                parent={{
                    id: this.state.ruleData.id,
                    property: _.get(this, 'state.ruleData.mappingTarget.uri'),
                    type: _.get(this, 'state.ruleData.rules.typeRules[0].typeUri'),
                }}
                targetClassUris={types}/> : false;


        return (
            <div className="ecc-silk-mapping__rules">
                {loading}
                {discardView}
                <div className="mdl-shadow--2dp">
                    <MappingRuleOverviewHeader rule={this.state.ruleData} key={id}/>
                    {suggestions || rulesList}
                </div>
                {createRuleForm}
            </div>
        );
    },
});

export default MappingRuleOverview;
