/*
 Whole overview over a hierarchical Mapping on the right, header may be defined here, loops over MappingRule
 */

import React from 'react';
import _ from 'lodash';
import {
    DisruptiveButton,
    DismissiveButton,
    Card,
    CardTitle,
    CardContent,
    CardMenu,
    FloatingActionList,
    ContextMenu,
    MenuItem,
    ConfirmationDialog,
    Info,
    Spinner,
} from '@eccenca/gui-elements';
import UseMessageBus from '../UseMessageBusMixin';
import hierarchicalMappingChannel from '../store';
import MappingsHeader from './MappingsHeader';
import MappingsObject from './MappingsObject';
import ObjectMappingRuleForm from './MappingRule/Forms/ObjectMappingRuleForm';
import ValueMappingRuleForm from './MappingRule/Forms/ValueMappingRuleForm';
import MappingsList from './MappingsList';
import SuggestionsList from './SuggestionsList';
import {MAPPING_RULE_TYPE_DIRECT, MAPPING_RULE_TYPE_OBJECT} from '../helpers';

const MappingsWorkview = React.createClass({
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
            askForDiscard: false,
            showSuggestions: false,
            askForChilds: false,
        };
    },
    componentDidMount() {
        this.loadData({initialLoad: true});
        this.subscribe(
            hierarchicalMappingChannel.subject('reload'),
            this.loadData
        );
        this.subscribe(
            hierarchicalMappingChannel.subject('ruleId.create'),
            this.onRuleCreate
        );
        this.subscribe(
            hierarchicalMappingChannel.subject('mapping.create'),
            this.handleCreate
        );
        this.subscribe(
            hierarchicalMappingChannel.subject('mapping.showSuggestions'),
            this.handleShowSuggestions
        );
        this.subscribe(
            hierarchicalMappingChannel.subject('list.toggleDetails'),
            this.handleToggleRuleDetails
        );
        this.subscribe(
            hierarchicalMappingChannel.subject('ruleView.unchanged'),
            this.handleRuleEditClose
        );
        this.subscribe(
            hierarchicalMappingChannel.subject('ruleView.close'),
            this.handleRuleEditClose
        );
        this.subscribe(
            hierarchicalMappingChannel.subject('ruleView.change'),
            this.handleRuleEditOpen
        );
        this.subscribe(
            hierarchicalMappingChannel.subject('ruleView.discardAll'),
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
                .subject('ruleView.change')
                .onNext({id: 0});
        } else {
            this.setState({
                askForDiscard: {
                    suggestions: true,
                },
            });
        }
    },
    componentDidUpdate(prevProps) {
        if (prevProps.currentRuleId !== this.props.currentRuleId) {
            this.loadData();
        }
    },
    loadData(params = {}) {
        const {initialLoad = false} = params;

        this.setState({
            loading: true,
        });

        if (__DEBUG__) {
            console.warn('DATA RELOAD');
        }

        hierarchicalMappingChannel
            .request({
                topic: 'rule.get',
                data: {
                    id: this.props.currentRuleId,
                    isObjectMapping: true,
                },
            })
            .subscribe(
                ({rule}) => {
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
                            .subject('rulesView.toggle')
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
                    this.setState({loading: false});
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
            hierarchicalMappingChannel.subject('ruleId.create').onNext({type});
        } else if (suggestions) {
            this.setState({
                showSuggestions: true,
            });
        } else {
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
        });
    },
    // sends event to expand / collapse all mapping rules
    handleToggleRuleDetails({expanded}) {
        if (this.state.editing.length === 0 || expanded) {
            hierarchicalMappingChannel
                .subject('rulesView.toggle')
                .onNext({expanded, id: true});
        } else {
            this.setState({
                askForDiscard: {
                    expanded,
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
        } else {
            this.setState({
                askForDiscard: {
                    type,
                },
            });
        }
    },

    handleCloseSuggestions() {
        this.setState({showSuggestions: false});
        hierarchicalMappingChannel.subject('ruleView.close').onNext({id: 0});
    },

    shouldComponentUpdate(nextProps, nextState) {
        // Required to prevent empty redraws while not all data is there.
        // The issue is due to bad use of React ...
        return !_.isEmpty(nextState.ruleData);
    },

    handleCopy(id) {
        hierarchicalMappingChannel
            .request({
                topic: 'rule.getDataToCopyRule',
                data: {
                    id: id,
                },
            })
            .subscribe(
                ({data}) => {
                    sessionStorage.setItem('copyingData',JSON.stringify(data));
                    this.setState({
                        isCopying: !this.state.isCopying,
                    });
                }
            );
    },

    createCopiedMappingRule(data, copyChilds) {
        this.setState({
            loading: true,
        });
        const topic = data.type === MAPPING_RULE_TYPE_DIRECT
            ? 'rule.createValueMapping'
            : copyChilds
                    ? 'rule.copyObjectMapping'
                    : 'rule.createObjectMapping';
        hierarchicalMappingChannel
            .request({
                topic: topic,
                data: data,
            })
            .subscribe(
                (replySubject) => {
                    this.setState({
                        loading: false,
                    });
                    if (data.type === MAPPING_RULE_TYPE_DIRECT) {
                        sessionStorage.setItem('pastedId', replySubject.body.id);
                    } else {
                        hierarchicalMappingChannel
                        .subject('ruleId.change')
                            .onNext({
                                newRuleId: replySubject.body.id,
                                parentId: data.parentId,
                            });
                    }

                    hierarchicalMappingChannel.subject('reload').onNext(true);
                }
            )
    },

    handlePaste(askForChilds = true, copyChilds = false) {
        let data = JSON.parse(sessionStorage.getItem('copyingData'));
        if (data !== {}) {
            data.parentId = this.props.currentRuleId;
            _.isEmpty(_.trim(data.label))
                ? data.label = 'Copy of ' + data.targetProperty
                : data.label = 'Copy of ' + data.label;
            if (data.type === MAPPING_RULE_TYPE_DIRECT) {
                this.createCopiedMappingRule(data);
                sessionStorage.removeItem('copyingData');
            } else {
                if (askForChilds) {
                    this.setState({
                        askForChilds: true,
                    });
                } else {
                    this.createCopiedMappingRule(data, copyChilds);
                    sessionStorage.removeItem('copyingData');
                }
            };
        }
    },

    handleClone(id) {
        hierarchicalMappingChannel
            .request({
                topic: 'rule.getDataToCopyRule',
                data: {
                    id: id,
                },
            })
            .subscribe(
                ({data}) => {
                    sessionStorage.setItem('copyingData',JSON.stringify(data));
                    this.handlePaste();
                }
            );
    },

    // template rendering
    render() {
        const {rules = {}, id} = this.state.ruleData;

        const loading = this.state.loading ? <Spinner /> : false;

        const discardView =
            this.state.askForDiscard !== false ? (
                <ConfirmationDialog
                    active
                    modal
                    title="Discard changes?"
                    confirmButton={
                        <DisruptiveButton
                            disabled={false}
                            onClick={this.handleDiscardChanges}>
                            Discard
                        </DisruptiveButton>
                    }
                    cancelButton={
                        <DismissiveButton onClick={this.handleCancelDiscard}>
                            Cancel
                        </DismissiveButton>
                    }>
                    <p>
                        You currently have unsaved changes{this.state.editing
                            .length === 1
                            ? ''
                            : ` in ${this.state.editing.length} mapping rules`}.
                    </p>
                </ConfirmationDialog>
            ) : (
                false
            );

        const createType = _.get(this.state, 'ruleEditView.type', false);

        const createRuleForm = createType ? (
            <div className="ecc-silk-mapping__createrule">
                {createType === MAPPING_RULE_TYPE_OBJECT ? (
                    <ObjectMappingRuleForm
                        type={createType}
                        parentId={this.state.ruleData.id}
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
                        edit
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
                      v.typeUri.replace('<', '').replace('>', '')
                  )
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
                    handleCopy={this.handleCopy}
                    handlePaste={this.handlePaste}
                    handleClone={this.handleClone}
                    isCopying={this.state.isCopying}
                />
            ) : (
                false
            );

        const askForChildsDialog = this.state.askForChilds ? (
            <ConfirmationDialog
                active
                modal
                title="Copying Object"
                confirmButton={
                    <DisruptiveButton
                        onClick={() => {
                            this.setState({
                                askForChilds: false,
                            });
                            this.handlePaste(false, true);
                        }}>
                        Yes
                    </DisruptiveButton>
                }
                cancelButton={
                    <DismissiveButton
                        onClick={() => {
                            this.setState({
                                askForChilds: false,
                            });
                            this.handlePaste(false);
                        }}>
                        No
                    </DismissiveButton>
                }>
                <p>Do you want to copy all the child Mapping Rules of this object?</p>
            </ConfirmationDialog>
        ) : (
            false
        );

        return (
            <div className="ecc-silk-mapping__rules">
                {loading}
                {discardView}
                {askForChildsDialog}
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
