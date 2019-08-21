/*
 Whole overview over a hierarchical Mapping on the right, header may be defined here, loops over MappingRule
 */

import React from 'react';
import _ from 'lodash';
import { Spinner } from '@eccenca/gui-elements';
import UseMessageBus from '../UseMessageBusMixin';
import hierarchicalMappingChannel, { copyRuleAsync, errorChannel, getApiDetails, getRuleAsync } from '../store';
import MappingsHeader from './MappingsHeader';
import MappingsObject from './MappingsObject';
import ObjectMappingRuleForm from './MappingRule/Forms/ObjectMappingRuleForm';
import ValueMappingRuleForm from './MappingRule/Forms/ValueMappingRuleForm';
import MappingsList from './MappingsList';
import SuggestionsList from './SuggestionsList';
import {
    isObjectMappingRule,
    MAPPING_RULE_TYPE_COMPLEX,
    MAPPING_RULE_TYPE_DIRECT,
    MAPPING_RULE_TYPE_OBJECT,
    MAPPING_RULE_TYPE_ROOT,
} from '../helpers';
import { MESSAGES } from '../constants';
import DiscardChangesDialog from '../elements/DiscardChangesDialog';

const MappingsWorkview = React.createClass({
    mixins: [UseMessageBus],

    // define property types
    propTypes: {
        currentRuleId: React.PropTypes.string, // selected rule id
    },
    onRuleCreate({ type }) {
        this.setState({
            ruleEditView: {
                type,
            },
        });
    },
    handleRuleEditOpen({ id }) {
        if (!_.includes(this.state.editing, id)) {
            this.setState({
                editing: _.concat(this.state.editing, [id]),
            });
        }
    },
    handleRuleEditClose({ id }) {
        if (id === 0) {
            this.setState({
                ruleEditView: false,
                editing: _.filter(this.state.editing, e => e !== id),
            });
        } else {
            this.setState({
                editing: _.filter(this.state.editing, e => e !== id),
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
            isCopying: !!sessionStorage.getItem('copyingData'),
            askForDiscard: false,
            showSuggestions: false,
            askForChilds: false,
        };
    },
    componentDidMount() {
        this.loadData({ initialLoad: true });
        this.subscribe(
            hierarchicalMappingChannel.subject(MESSAGES.RELOAD),
            this.loadData
        );
        this.subscribe(
            hierarchicalMappingChannel.subject(MESSAGES.RULE_ID.CREATE),
            this.onRuleCreate
        );
        this.subscribe(
            hierarchicalMappingChannel.subject(MESSAGES.MAPPING.CREATE),
            this.handleCreate
        );
        this.subscribe(
            hierarchicalMappingChannel.subject(MESSAGES.MAPPING.SHOW_SUGGESTIONS),
            this.handleShowSuggestions
        );
        this.subscribe(
            hierarchicalMappingChannel.subject(MESSAGES.TOGGLE_DETAILS),
            this.handleToggleRuleDetails
        );
        this.subscribe(
            hierarchicalMappingChannel.subject(MESSAGES.RULE_VIEW.UNCHANGED),
            this.handleRuleEditClose
        );
        this.subscribe(
            hierarchicalMappingChannel.subject(MESSAGES.RULE_VIEW.CLOSE),
            this.handleRuleEditClose
        );
        this.subscribe(
            hierarchicalMappingChannel.subject(MESSAGES.RULE_VIEW.CHANGE),
            this.handleRuleEditOpen
        );
        this.subscribe(
            hierarchicalMappingChannel.subject(MESSAGES.RULE_VIEW.DISCARD_ALL),
            this.discardAll
        );
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
            hierarchicalMappingChannel
                .subject(MESSAGES.RULE_VIEW.CHANGE)
                .onNext({ id: 0 });
        } else {
            this.setState({
                askForDiscard: {
                    suggestions: true,
                },
            });
        }
    },
    componentDidUpdate(prevProps, prevState) {
        if (prevProps.currentRuleId !== this.props.currentRuleId) {
            this.loadData();
        }
    },
    loadData(params = {}) {
        const { initialLoad = false } = params;

        this.setState({
            loading: true,
        });

        if (__DEBUG__) {
            console.warn('DATA RELOAD');
        }
        getRuleAsync(this.props.currentRuleId, true)
            .subscribe(
                ({ rule }) => {
                    if (
                        initialLoad &&
                        this.props.currentRuleId &&
                        rule.id !== this.props.currentRuleId
                    ) {
                        let toBeOpened;

                        // If the currentRuleId equals the uriRule's id, we want to expand the object mapping
                        if (
                            _.get(rule, 'rules.uriRule.id') ===
                            this.props.currentRuleId
                        ) {
                            toBeOpened = rule.id;
                        } else {
                            // otherwise we want to expand the value mapping
                            toBeOpened = this.props.currentRuleId;
                        }

                        hierarchicalMappingChannel
                            .subject(MESSAGES.RULE_VIEW.TOGGLE)
                            .onNext({
                                expanded: true,
                                id: toBeOpened,
                            });
                    }

                    this.setState({
                        loading: false,
                        ruleData: rule,
                    });
                },
                err => {
                    this.setState({ loading: false });
                }
            );
    },
    handleDiscardChanges(event) {
        event.stopPropagation();
        const type = _.get(this.state.askForDiscard, 'type', false);
        const suggestions = _.get(
            this.state.askForDiscard,
            'suggestions',
            false
        );
        const expanded = _.get(this.state.askForDiscard, 'expanded', false);

        if (type) {
            hierarchicalMappingChannel.subject(MESSAGES.RULE_ID.CREATE).onNext({ type });
        } else if (suggestions) {
            this.setState({
                showSuggestions: true,
            });
        } else {
            hierarchicalMappingChannel.subject(MESSAGES.RULE_VIEW.TOGGLE).onNext({
                expanded,
                id: true,
            });
        }
        hierarchicalMappingChannel.subject(MESSAGES.RULE_VIEW.DISCARD_ALL).onNext();
        this.setState({
            askForDiscard: false,
        });
    },
    handleCancelDiscard(event) {
        event.stopPropagation();
        this.setState({
            askForDiscard: false,
        });
    },
    // sends event to expand / collapse all mapping rules
    handleToggleRuleDetails({ expanded }) {
        if (this.state.editing.length === 0 || expanded) {
            hierarchicalMappingChannel
                .subject(MESSAGES.RULE_VIEW.TOGGLE)
                .onNext({ expanded, id: true });
        } else {
            this.setState({
                askForDiscard: {
                    expanded,
                },
            });
        }
    },

    // jumps to selected rule as new center of view
    handleCreate({ type }) {
        if (this.state.editing.length === 0) {
            hierarchicalMappingChannel.subject(MESSAGES.RULE_ID.CREATE).onNext({
                type,
            });
        } else {
            this.setState({
                askForDiscard: {
                    type,
                },
            });
        }
    },

    handleCloseSuggestions() {
        this.setState({ showSuggestions: false });
        hierarchicalMappingChannel.subject(MESSAGES.RULE_VIEW.CLOSE).onNext({ id: 0 });
    },

    shouldComponentUpdate(nextProps, nextState) {
        // Required to prevent empty redraws while not all data is there.
        // The issue is due to bad use of React ...
        return !_.isEmpty(nextState.ruleData);
    },

    handleCopy(id, type) {
        errorChannel.subject('message.info').onNext({
            message: 'Mapping rule copied. Use "+" button to paste',
        });
        const apiDetails = getApiDetails();
        const copyingData = {
            baseUrl: apiDetails.baseUrl,
            project: apiDetails.project,
            transformTask: apiDetails.transformTask,
            id: id,
            type: type,
            cloning: false,
        };
        sessionStorage.setItem('copyingData',JSON.stringify(copyingData));
        this.setState({
            isCopying: !this.state.isCopying,
        });
    },

    handlePaste(cloning = false) {
        const copyingData = JSON.parse(sessionStorage.getItem('copyingData')),
            { breadcrumbs, id } = this.state.ruleData;
        if (copyingData !== {}) {
            const data = {
                id: breadcrumbs.length > 0 && isObjectMappingRule(copyingData.type) && copyingData.cloning ? breadcrumbs[breadcrumbs.length - 1].id : id,
                queryParameters: {
                    sourceProject: copyingData.project,
                    sourceTask: copyingData.transformTask,
                    sourceRule: copyingData.id,
                    afterRuleId: copyingData.cloning ? copyingData.id : null,
                },
            };
           copyRuleAsync(data)
                .subscribe(
                    (newRuleId) => {
                        if (copyingData.type === MAPPING_RULE_TYPE_DIRECT ||
                            copyingData.type === MAPPING_RULE_TYPE_COMPLEX) {
                            sessionStorage.setItem('pastedId', newRuleId);
                        } else if (copyingData.type === MAPPING_RULE_TYPE_OBJECT || copyingData.type === MAPPING_RULE_TYPE_ROOT) {
                            hierarchicalMappingChannel
                                .subject(MESSAGES.RULE_ID.CHANGE)
                                .onNext({
                                    newRuleId,
                                });
                        }
                        if (cloning) {
                            sessionStorage.removeItem('copyingData');
                        }
                        hierarchicalMappingChannel.subject(MESSAGES.RELOAD).onNext(true);
                    }
                )
        }
    },

    handleClone(id, type, parent = false) {
        const apiDetails = getApiDetails();
        const copyingData = {
            baseUrl: apiDetails.baseUrl,
            project: apiDetails.project,
            transformTask: apiDetails.transformTask,
            id: id,
            type: type,
            cloning: true,
            parentId: parent ? parent : this.props.currentRuleId,
        };
        sessionStorage.setItem('copyingData',JSON.stringify(copyingData));
        this.setState({
            isCopying: !this.state.isCopying,
        });
        this.handlePaste(true);
    },

    // template rendering
    render() {
        const {
            askForDiscard, editing,
        } = this.state;
        const { rules = {}, id } = this.state.ruleData;

        const loading = this.state.loading ? <Spinner /> : false;

        const createType = _.get(this.state, 'ruleEditView.type', false);

        const createRuleForm = createType ? (
            <div className="ecc-silk-mapping__createrule">
                {createType === MAPPING_RULE_TYPE_OBJECT ? (
                    <ObjectMappingRuleForm
                        type={createType}
                        parentId={this.state.ruleData.id}
                        parent={{
                            id: this.state.ruleData.id,
                            property: _.get(this, 'state.ruleData.mappingTarget.uri'),
                            type: _.get(this, 'state.ruleData.rules.typeRules[0].typeUri'),
                        }}
                        ruleData={{ type: MAPPING_RULE_TYPE_OBJECT }}
                    />
                ) : (
                    <ValueMappingRuleForm
                        type={createType}
                        parentId={this.state.ruleData.id}
                        edit
                    />
                )}
            </div>
        ) : (
            false
        );

        const types =
            !createRuleForm &&
            this.state.showSuggestions &&
            _.has(this.state, 'ruleData.rules.typeRules')
                ? _.map(this.state.ruleData.rules.typeRules, v =>
                    v.typeUri.replace('<', '').replace('>', ''))
                : [];

        const listSuggestions =
            !createRuleForm &&
            this.state.showSuggestions &&
            _.has(this.state, 'ruleData.rules.typeRules') ? (
                    <SuggestionsList
                        key={_.join(types, ',')}
                        ruleId={_.get(this, 'state.ruleData.id', 'root')}
                        onClose={this.handleCloseSuggestions}
                        parent={{
                            id: this.state.ruleData.id,
                            property: _.get(
                                this,
                                'state.ruleData.mappingTarget.uri'
                            ),
                            type: _.get(
                                this,
                                'state.ruleData.rules.typeRules[0].typeUri'
                            ),
                        }}
                        targetClassUris={types}
                    />
                ) : (
                    false
                );
        const listMappings =
            !createRuleForm && !listSuggestions ? (
                <MappingsList
                    currentRuleId={_.get(this.props, 'currentRuleId', 'root')}
                    rules={_.get(rules, 'propertyRules', [])}
                    parentRuleId={id}
                    handleCopy={this.handleCopy}
                    handlePaste={this.handlePaste}
                    handleClone={this.handleClone}
                    isCopying={this.state.isCopying}
                />
            ) : (
                false
            );

        return (
            <div className="ecc-silk-mapping__rules">
                {loading}
                {
                    askForDiscard && (
                        <DiscardChangesDialog
                            numberEditingElements={editing.length}
                            handleDiscardCancel={this.handleCancelDiscard}
                            handleDiscardConfirm={this.handleDiscardChanges}
                        />
                    )
                }
                <MappingsHeader
                    rule={this.state.ruleData}
                    key={`navhead_${id}`}
                />
                <div className="mdl-shadow--2dp">
                    <MappingsObject
                        rule={this.state.ruleData}
                        key={`objhead_${id}`}
                        handleCopy={this.handleCopy}
                        handleClone={this.handleClone}
                    />
                    {listSuggestions ? false : listMappings}
                </div>
                {listSuggestions}
                {createRuleForm}
            </div>
        );
    },
});

export default MappingsWorkview;
