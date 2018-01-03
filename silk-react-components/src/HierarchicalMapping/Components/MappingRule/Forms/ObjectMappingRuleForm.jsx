import React from 'react';
import {
    AffirmativeButton,
    DismissiveButton,
    Card,
    CardTitle,
    CardContent,
    CardActions,
    Radio,
    RadioGroup,
    TextField,
    Spinner,
    ScrollingMixin,
} from 'ecc-gui-elements';
import _ from 'lodash';
import ExampleView from '../ExampleView';
import UseMessageBus from '../../../UseMessageBusMixin';
import {ParentElement} from '../SharedComponents';
import hierarchicalMappingChannel from '../../../store';
import {newValueIsIRI, wasTouched} from './helpers';
import ErrorView from '../ErrorView';
import AutoComplete from './AutoComplete';
import {
    MAPPING_RULE_TYPE_OBJECT,
    MAPPING_RULE_TYPE_ROOT,
    MAPPING_RULE_TYPE_COMPLEX_URI,
    MAPPING_RULE_TYPE_URI
} from '../../../helpers';

const ObjectMappingRuleForm = React.createClass({
    mixins: [UseMessageBus, ScrollingMixin],

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
    componentDidUpdate(prevProps, prevState) {
        if (
            prevState.loading === true &&
            _.get(this.state, 'loading', false) === false
        ) {
            this.scrollIntoView({
                topOffset: 75,
            });
        }
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
                            label: _.get(rule, 'metadata.label', ''),
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
                            uriRuleType: _.get(
                                rule,
                                'rules.uriRule.type',
                                MAPPING_RULE_TYPE_URI
                            ),
                            uriRule: _.get(rule, 'rules.uriRule'),
                        };

                        this.setState({
                            loading: false,
                            initialValues,
                            ...initialValues,
                        });
                    },
                    err => {
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
                type: MAPPING_RULE_TYPE_OBJECT,
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
                    label: this.state.label,
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
    getExampleView() {
        if (this.state.pattern){
            return (
                <ExampleView
                    id={this.props.parentId || 'root'}
                    rawRule={{
                        type: MAPPING_RULE_TYPE_URI,
                        pattern: this.state.pattern,
                    }}
                    ruleType={MAPPING_RULE_TYPE_URI}
                />
            );
        }
        else if (this.state.uriRule) {
            return <ExampleView
                id={this.props.parentId || 'root'}
                rawRule={this.state.uriRule}
                ruleType={this.state.uriRule.type}
            />;
        }
        else {
            return false;
        }
    },
    // template rendering
    render() {
        const {id, parentId} = this.props;

        const autoCompleteRuleId = id || parentId;

        const {error} = this.state;

        const type = this.state.type;

        if (this.state.loading) {
            return <Spinner />;
        }

        // FIXME: also check if data really has changed before allow saving
        const allowConfirm =
            type === MAPPING_RULE_TYPE_ROOT
                ? true
                : !_.isEmpty(this.state.targetProperty);

        const errorMessage = error ? (
            <ErrorView {...error.response.body} />
        ) : (
            false
        );

        const title =
            // TODO: add source path if: parent, not edit, not root element
            !id ? <CardTitle>Add object mapping</CardTitle> : false;

        let targetPropertyInput = false;
        let entityRelationInput = false;
        let sourcePropertyInput = false;

        if (type !== MAPPING_RULE_TYPE_ROOT) {
            // TODO: where to get get list of target properties
            targetPropertyInput = (
                <AutoComplete
                    placeholder={'Target property'}
                    className="ecc-silk-mapping__ruleseditor__targetProperty"
                    entity="targetProperty"
                    isValidNewOption={newValueIsIRI}
                    creatable
                    ruleId={autoCompleteRuleId}
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
                    ruleId={autoCompleteRuleId}
                    onChange={this.handleChangeSelectBox.bind(
                        null,
                        'sourceProperty'
                    )}
                />
            );
        }

        let patternInput = false;

        if (id) {
            if (this.state.uriRuleType === 'uri') {
                patternInput = (
                    <TextField
                        label="URI pattern"
                        className="ecc-silk-mapping__ruleseditor__pattern"
                        value={this.state.pattern}
                        onChange={this.handleChangeTextfield.bind(
                            null,
                            'pattern'
                        )}
                    />
                );
            } else {
                patternInput = (
                    <TextField
                        disabled
                        label="URI formula"
                        value="This URI cannot be edited in the edit form."
                    />
                );
            }
        }

        const exampleView = this.getExampleView();

        return (
            <div className="ecc-silk-mapping__ruleseditor">
                <Card shadow={!id ? 1 : 0}>
                    {title}
                    <CardContent>
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
                            ruleId={autoCompleteRuleId}
                            value={this.state.targetEntityType}
                            multi // allow multi selection
                            creatable
                            onChange={this.handleChangeSelectBox.bind(
                                null,
                                'targetEntityType'
                            )}
                        />
                        {patternInput}
                        {sourcePropertyInput}
                        {exampleView}
                        <TextField
                            label="Label"
                            className="ecc-silk-mapping__ruleseditor__label"
                            value={this.state.label}
                            onChange={this.handleChangeTextfield.bind(
                                null,
                                'label'
                            )}
                        />
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
                    </CardContent>
                    <CardActions className="ecc-silk-mapping__ruleseditor__actionrow">
                        <AffirmativeButton
                            className="ecc-silk-mapping__ruleseditor__actionrow-save"
                            raised
                            onClick={this.handleConfirm}
                            disabled={!allowConfirm || !this.state.changed}>
                            Save
                        </AffirmativeButton>
                        <DismissiveButton
                            className="ecc-silk-mapping__ruleseditor__actionrow-cancel"
                            raised
                            onClick={this.handleClose}>
                            Cancel
                        </DismissiveButton>
                    </CardActions>
                </Card>
            </div>
        );
    },
});

export default ObjectMappingRuleForm;
