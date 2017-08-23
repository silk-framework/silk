import React from 'react';
import UseMessageBus from '../../../UseMessageBusMixin';
import {
    Radio,
    RadioGroup,
    TextField,
    AffirmativeButton,
    Spinner,
    DismissiveButton,
} from 'ecc-gui-elements';
import {ParentElement} from '../SharedComponents';
import hierarchicalMappingChannel from '../../../store';
import {newValueIsIRI, wasTouched} from './helpers';
import _ from 'lodash';
import FormSaveError from './FormSaveError';
import AutoComplete from './AutoComplete';

const ObjectMappingRuleForm = React.createClass({
    mixins: [UseMessageBus],

    // define property types
    propTypes: {
        id: React.PropTypes.string,
    },
    getInitialState() {
        return {
            loading: true,
            changed: false,
        };
    },
    componentDidMount() {
        this.loadData();
    },
    loadData() {
        if (this.props.id) {
            hierarchicalMappingChannel
                .request({
                    topic: 'rule.get',
                    data: {
                        id: this.props.id,
                    },
                })
                .subscribe(
                    ({rule}) => {
                        const initialValues = {
                            targetProperty: _.get(
                                rule,
                                'mappingTarget.uri',
                                undefined
                            ),
                            sourceProperty: _.get(
                                rule,
                                'sourcePath',
                                undefined
                            ),
                            comment: _.get(rule, 'metadata.description', ''),
                            targetEntityType: _.chain(rule)
                                .get('rules.typeRules', [])
                                .map('typeUri')
                                .value(),
                            entityConnection: _.get(
                                rule,
                                'mappingTarget.isBackwardProperty',
                                false
                            )
                                ? 'to'
                                : 'from',
                            pattern: _.get(rule, 'rules.uriRule.pattern', ''),
                            type: _.get(rule, 'type'),
                        };

                        this.setState({
                            loading: false,
                            initialValues,
                            ...initialValues,
                        });
                    },
                    err => {
                        console.warn('err MappingRuleOverview: rule.get');
                        this.setState({
                            loading: false,
                            initialValues: {},
                        });
                    }
                );
        } else {
            hierarchicalMappingChannel
                .subject('ruleView.change')
                .onNext({id: 0});
            this.setState({
                create: true,
                loading: false,
                type: 'object',
            });
        }
    },
    handleConfirm(event) {
        event.stopPropagation();
        event.persist();
        this.setState({
            loading: true,
        });
        hierarchicalMappingChannel
            .request({
                topic: 'rule.createObjectMapping',
                data: {
                    id: this.props.id,
                    parentId: this.props.parentId,
                    type: this.state.type,
                    comment: this.state.comment,
                    sourceProperty: this.state.sourceProperty,
                    targetProperty: this.state.targetProperty,
                    targetEntityType: this.state.targetEntityType,
                    pattern: this.state.pattern,
                    entityConnection: this.state.entityConnection === 'to',
                },
            })
            .subscribe(
                () => {
                    this.handleClose(event);
                    hierarchicalMappingChannel.subject('reload').onNext(true);
                },
                err => {
                    this.setState({
                        error: err,
                        loading: false,
                    });
                }
            );
    },
    handleChangeSelectBox(state, value) {
        this.handleChangeValue(state, value);
    },
    handleChangeTextfield(state, {value}) {
        this.handleChangeValue(state, value);
    },
    handleChangeRadio(state, {value}) {
        this.handleChangeValue(state, value);
    },
    handleChangeValue(name, value) {
        const {initialValues, create, ...currValues} = this.state;

        currValues[name] = value;

        const touched = create || wasTouched(initialValues, currValues);
        const id = _.get(this.props, 'id', 0);

        if (id !== 0) {
            if (touched) {
                hierarchicalMappingChannel
                    .subject('ruleView.change')
                    .onNext({id});
            } else {
                hierarchicalMappingChannel
                    .subject('ruleView.unchanged')
                    .onNext({id});
            }
        }

        this.setState({
            [name]: value,
            changed: touched,
        });
    },
    handleClose(event) {
        event.stopPropagation();
        const id = _.get(this.props, 'id', 0);
        hierarchicalMappingChannel.subject('ruleView.unchanged').onNext({id});
        hierarchicalMappingChannel.subject('ruleView.close').onNext({id});
    },
    // template rendering
    render() {
        const {id} = this.props;

        const {error} = this.state;

        const type = this.state.type;

        if (this.state.loading) {
            return <Spinner />;
        }

        // FIXME: also check if data really has changed before allow saving
        const allowConfirm = type === 'root' ? true : this.state.targetProperty;

        const errorMessage = error ? <FormSaveError error={error} /> : false;

        const title =
            // TODO: add source path if: parent, not edit, not root element
            !id
                ? <div className="mdl-card__title mdl-card--border">
                      Add object mapping
                  </div>
                : false;

        let targetPropertyInput = false;
        let entityRelationInput = false;
        let sourcePropertyInput = false;

        if (type !== 'root') {
            // TODO: where to get get list of target properties
            targetPropertyInput = (
                <AutoComplete
                    placeholder={'Target property'}
                    className="ecc-silk-mapping__ruleseditor__targetProperty"
                    entity="targetProperty"
                    isValidNewOption={newValueIsIRI}
                    creatable
                    ruleId={this.props.parentId}
                    value={this.state.targetProperty}
                    onChange={this.handleChangeSelectBox.bind(
                        null,
                        'targetProperty'
                    )}
                />
            );
            entityRelationInput = (
                <RadioGroup
                    onChange={this.handleChangeRadio.bind(
                        null,
                        'entityConnection'
                    )}
                    value={
                        !_.isEmpty(this.state.entityConnection)
                            ? this.state.entityConnection
                            : 'from'
                    }
                    name=""
                    disabled={false}>
                    <Radio
                        value="from"
                        label={
                            <div>
                                Connect from{' '}
                                <ParentElement parent={this.props.parent} />
                            </div>
                        }
                    />
                    <Radio
                        value="to"
                        label={
                            <div>
                                Connect to{' '}
                                <ParentElement parent={this.props.parent} />
                            </div>
                        }
                    />
                </RadioGroup>
            );

            sourcePropertyInput = (
                <AutoComplete
                    placeholder={'Value path'}
                    className="ecc-silk-mapping__ruleseditor__sourcePath"
                    entity="sourcePath"
                    creatable
                    value={this.state.sourceProperty}
                    ruleId={this.props.parentId}
                    onChange={this.handleChangeSelectBox.bind(
                        null,
                        'sourceProperty'
                    )}
                />
            );
        }

        let patternInput = false;

        if (id) {
            patternInput = (
                <TextField
                    label="URI pattern"
                    className="ecc-silk-mapping__ruleseditor__pattern"
                    value={this.state.pattern}
                    onChange={this.handleChangeTextfield.bind(null, 'pattern')}
                />
            );
        }

        return (
            <div>
                <div className="ecc-silk-mapping__ruleseditor">
                    <div
                        className={`mdl-card mdl-card--stretch${!id
                            ? ' mdl-shadow--2dp'
                            : ''}`}>
                        {title}
                        <div className="mdl-card__content">
                            {errorMessage}
                            {targetPropertyInput}
                            {entityRelationInput}
                            <AutoComplete
                                placeholder={'Target entity type'}
                                className={
                                    'ecc-silk-mapping__ruleseditor__targetEntityType'
                                }
                                entity="targetEntityType"
                                isValidNewOption={newValueIsIRI}
                                ruleId={
                                    type === 'root'
                                        ? this.props.id
                                        : this.props.parentId
                                }
                                value={this.state.targetEntityType}
                                multi // allow multi selection
                                creatable
                                onChange={this.handleChangeSelectBox.bind(
                                    null,
                                    'targetEntityType'
                                )}
                            />
                            {sourcePropertyInput}
                            {patternInput}
                            <TextField
                                multiline
                                label="Description"
                                className="ecc-silk-mapping__ruleseditor__comment"
                                value={this.state.comment}
                                onChange={this.handleChangeTextfield.bind(
                                    null,
                                    'comment'
                                )}
                            />
                        </div>
                        <div className="ecc-silk-mapping__ruleseditor__actionrow mdl-card__actions mdl-card--border">
                            <AffirmativeButton
                                className="ecc-silk-mapping__ruleseditor__actionrow-save"
                                onClick={this.handleConfirm}
                                disabled={!allowConfirm || !this.state.changed}>
                                Save
                            </AffirmativeButton>
                            <DismissiveButton
                                className="ecc-silk-mapping__ruleseditor__actionrow-cancel"
                                onClick={this.handleClose}>
                                Cancel
                            </DismissiveButton>
                        </div>
                    </div>
                </div>
            </div>
        );
    },
});

export default ObjectMappingRuleForm;
