import React from 'react';
import UseMessageBus from '../../../UseMessageBusMixin';
import {
    TextField,
    AffirmativeButton,
    DismissiveButton,
    Spinner,
} from 'ecc-gui-elements';
import hierarchicalMappingChannel from '../../../store';
import {newValueIsIRI, wasTouched} from './helpers';
import _ from 'lodash';
import FormSaveError from './FormSaveError';
import AutoComplete from './AutoComplete';

const ValueMappingRuleForm = React.createClass({
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
                            type: _.get(rule, 'type', 'direct'),
                            comment: _.get(rule, 'metadata.description', ''),
                            targetProperty: _.get(
                                rule,
                                'mappingTarget.uri',
                                '',
                            ),
                            propertyType: _.get(
                                rule,
                                'mappingTarget.valueType.nodeType',
                                'AutoDetectValueType',
                            ),
                            sourceProperty: rule.sourcePath,
                        };

                        this.setState({
                            loading: false,
                            ...initialValues,
                            initialValues,
                        });
                    },
                    err => {
                        console.warn('err MappingRuleOverview: rule.get');
                        this.setState({loading: false});
                    },
                );
        } else {
            hierarchicalMappingChannel
                .subject('ruleView.change')
                .onNext({id: 0});
            this.setState({
                create: true,
                loading: false,
                type: 'direct',
                propertyType: 'AutoDetectValueType',
                sourceProperty: '',
                initialValues: {},
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
                topic: 'rule.createValueMapping',
                data: {
                    id: this.props.id,
                    parentId: this.props.parentId,
                    type: this.state.type,
                    comment: this.state.comment,
                    targetProperty: this.state.targetProperty,
                    propertyType: this.state.propertyType,
                    sourceProperty: this.state.sourceProperty,
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
                },
            );
    },
    // remove rule
    handleChangeTextfield(state, {value}) {
        this.handleChangeValue(state, value);
    },
    handleChangeSelectBox(state, value) {
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

        const {type, error} = this.state;

        if (this.state.loading) {
            return <Spinner />;
        }

        const errorMessage = error ? <FormSaveError error={error} /> : false;

        const allowConfirm = this.state.targetProperty;

        const title = !id
            ? <div className="mdl-card__title mdl-card--border">
                  Add value mapping
              </div>
            : false;

        // TODO: Unfold complex mapping
        let sourcePropertyInput = false;

        if (type === 'direct') {
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
                        'sourceProperty',
                    )}
                />
            );
        } else if (type === 'complex') {
            sourcePropertyInput = (
                <TextField
                    disabled
                    label="Value formula"
                    value="The value formula cannot be modified in the edit form."
                />
            );
        }
        // TODO: Where to get the list of target Properties?
        // TODO: Where to get the list of target property types?
        return (
            <div className="ecc-silk-mapping__ruleseditor">
                <div
                    className={`mdl-card mdl-card--stretch${!id
                        ? ' mdl-shadow--2dp'
                        : ''}`}>
                    {title}
                    <div className="mdl-card__content">
                        {errorMessage}
                        <AutoComplete
                            placeholder={'Target property'}
                            className="ecc-silk-mapping__ruleseditor__targetProperty"
                            entity="targetProperty"
                            isValidNewOption={newValueIsIRI}
                            creatable
                            value={this.state.targetProperty}
                            ruleId={this.props.parentId}
                            onChange={this.handleChangeSelectBox.bind(
                                null,
                                'targetProperty',
                            )}
                        />
                        <AutoComplete
                            placeholder={'Data type'}
                            className="ecc-silk-mapping__ruleseditor__propertyType"
                            entity="propertyType"
                            ruleId={this.props.parentId}
                            value={this.state.propertyType}
                            clearable={false}
                            onChange={this.handleChangeSelectBox.bind(
                                null,
                                'propertyType',
                            )}
                        />
                        {sourcePropertyInput}
                        <TextField
                            multiline
                            label="Description"
                            className="ecc-silk-mapping__ruleseditor__comment"
                            value={this.state.comment}
                            onChange={this.handleChangeTextfield.bind(
                                null,
                                'comment',
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
                            className="ecc-silk-mapping__ruleseditor___actionrow-cancel"
                            onClick={this.handleClose}>
                            Cancel
                        </DismissiveButton>
                    </div>
                </div>
            </div>
        );
    },
});

export default ValueMappingRuleForm;
