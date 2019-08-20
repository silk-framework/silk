import React, { Component } from 'react';
import PropTypes from 'prop-types';
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
    ScrollingHOC,
} from '@eccenca/gui-elements';
import _ from 'lodash';
import ExampleView from '../ExampleView';
import UseMessageBus from '../../../UseMessageBusMixin';
import {ParentElement} from '../SharedComponents';
import hierarchicalMappingChannel, { createMappingAsync, getRuleAsync } from '../../../store';
import {newValueIsIRI, wasTouched, convertToUri} from './helpers';
import ErrorView from '../ErrorView';
import AutoComplete from './AutoComplete';
import {
    MAPPING_RULE_TYPE_ROOT,
    MAPPING_RULE_TYPE_URI,
    trimValueLabelObject,
    trimUriPattern,
} from '../../../helpers';
import { MESSAGES } from '../../../constants';

/**
 * Provides the editable form for object mappings.
 */
class ObjectMappingRuleForm extends Component {
    // define property types
    static propTypes = {
        id: PropTypes.string,
        parentId: PropTypes.string.isRequired,
        parent: PropTypes.shape({
            id: PropTypes.string,
            // property,
            type: PropTypes.string,
        }).isRequired,
        scrollIntoView: PropTypes.func.isRequired,
        scrollElementIntoView: PropTypes.func.isRequired,
        ruleData: PropTypes.object.isRequired,
    };
    static defaultProps = {
        id: undefined,
    };

    /**
     * React's lifecycle method
     * @param props
     */
    constructor(props) {
        super(props);

        this.state = {
            loading: false,
            changed: false,
            create: !props.id,
            // get a deep copy of origin data for modification
            modifiedValues: _.cloneDeep(props.ruleData),
            saveObjectError: undefined,
        };

        this.handleConfirm = this.handleConfirm.bind(this);
        this.handleChangeValue = this.handleChangeValue.bind(this);
        this.handleClose = this.handleClose.bind(this);
    }

    /**
     * React's lifecycle method
     */
    componentDidMount() {
        const { id, scrollIntoView } = this.props;
        // set screen focus to this element
        scrollIntoView({ topOffset: 75 });
        if (!id) {
            hierarchicalMappingChannel.subject(MESSAGES.RULE_VIEW.CHANGE).onNext({ id: 0 });
        }
    },

    loadData() {
        if (this.props.id) {
            getRuleAsync(this.props.id)
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
                .subject(MESSAGES.RULE_VIEW.CHANGE)
                .onNext({id: 0});
            this.setState({
                create: true,
                loading: false,
                type: MAPPING_RULE_TYPE_OBJECT,
            });
        }
    },
    }

    /**
     * Saves the modified data
     * @param event
     */
    handleConfirm(event) {
        event.stopPropagation();
        event.persist();
        this.setState({
            loading: true,
        });
        createMappingAsync({
            id: this.props.id,
            parentId: this.props.parentId,
            type: this.state.type,
            comment: this.state.comment,
            label: this.state.label,
            sourceProperty: trimValueLabelObject(
                this.state.sourceProperty
            ),
            targetProperty: trimValueLabelObject(
                this.state.targetProperty
            ),
            targetEntityType: this.state.targetEntityType,
            pattern: trimUriPattern(this.state.pattern),
            entityConnection: this.state.entityConnection === 'to',
        }, true)
            .subscribe(
                () => {
                    this.handleClose(event);
                    hierarchicalMappingChannel.subject(MESSAGES.RELOAD).onNext(true);
                },
                err => {
                    this.setState({
                        saveObjectError: err.response.body,
                        loading: false,
                    });
                }
            );
    }

    /**
     * Handle input changes from user
     * @param name
     * @param value
     */
    handleChangeValue(name, value) {
        const { id, ruleData } = this.props;
        const { create, modifiedValues } = this.state;

        const newModifiedValues = { ...modifiedValues, [name]: value };

        const changed = create || wasTouched(ruleData, newModifiedValues);

        if (id) {
            if (changed) {
                hierarchicalMappingChannel.subject(MESSAGES.RULE_VIEW.CHANGE).onNext({ id });
            } else {
                hierarchicalMappingChannel.subject(MESSAGES.RULE_VIEW.UNCHANGED).onNext({ id });
            }
        }
        this.setState({
            modifiedValues: newModifiedValues,
            changed,
        });
    }

    /**
     * handle form close event
     * @param event
     */
    handleClose(event) {
        event.stopPropagation();
        const { id = 0 } = this.props;
        hierarchicalMappingChannel.subject(MESSAGES.RULE_VIEW.UNCHANGED).onNext({ id });
        hierarchicalMappingChannel.subject(MESSAGES.RULE_VIEW.CLOSE).onNext({ id });
    }

    /**
     * React's lifecycle method
     * @returns {*}
     */
    render() {
        const { id, parentId, parent } = this.props;
        const {
            saveObjectError, loading, modifiedValues, changed,
        } = this.state;

        const autoCompleteRuleId = id || parentId;

        if (loading) {
            return <Spinner />;
        }

        // FIXME: also check if data really has changed before allow saving
        const allowConfirm =
            modifiedValues.type === MAPPING_RULE_TYPE_ROOT || !_.isEmpty(modifiedValues.targetProperty);

        const errorMessage = saveObjectError && (
            <ErrorView {...saveObjectError} />
        );

        // TODO: add source path if: parent, not edit, not root element
        const title = !id && <CardTitle>Add object mapping</CardTitle>;

        let targetPropertyInput = false;
        let entityRelationInput = false;
        let sourcePropertyInput = false;

        if (modifiedValues.type !== MAPPING_RULE_TYPE_ROOT) {
            // TODO: where to get get list of target properties
            targetPropertyInput = (
                <AutoComplete
                    placeholder="Target property"
                    className="ecc-silk-mapping__ruleseditor__targetProperty"
                    entity="targetProperty"
                    newOptionCreator={convertToUri}
                    isValidNewOption={newValueIsIRI}
                    creatable
                    ruleId={autoCompleteRuleId}
                    value={modifiedValues.targetProperty}
                    onChange={value => { this.handleChangeValue('targetProperty', value); }}
                />
            );
            entityRelationInput = (
                <RadioGroup
                    onChange={({ value }) => { this.handleChangeValue('entityConnection', value); }}
                    value={
                        !_.isEmpty(modifiedValues.entityConnection)
                            ? modifiedValues.entityConnection
                            : 'from'
                    }
                    name=""
                    disabled={false}
                >
                    <Radio
                        value="from"
                        label={
                            <div>
                                Connect from{' '}
                                <ParentElement parent={parent} />
                            </div>
                        }
                    />
                    <Radio
                        value="to"
                        label={
                            <div>
                                Connect to{' '}
                                <ParentElement parent={parent} />
                            </div>
                        }
                    />
                </RadioGroup>
            );

            sourcePropertyInput = (
                <AutoComplete
                    placeholder="Value path"
                    //key={modifiedValues.sourceProperty}
                    className="ecc-silk-mapping__ruleseditor__sourcePath"
                    entity="sourcePath"
                    creatable
                    value={modifiedValues.sourceProperty}
                    ruleId={parentId}
                    onChange={value => { this.handleChangeValue('sourceProperty', value); }}
                />
            );
        }

        let patternInput = false;

        if (id) {
            if (modifiedValues.uriRuleType === 'uri') {
                patternInput = (
                    <TextField
                        label="URI pattern"
                        className="ecc-silk-mapping__ruleseditor__pattern"
                        value={modifiedValues.pattern}
                        onChange={({ value }) => { this.handleChangeValue('pattern', value); }}
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

        return (
            <div className="ecc-silk-mapping__ruleseditor">
                <Card shadow={!id ? 1 : 0}>
                    {title}
                    <CardContent>
                        {errorMessage}
                        {targetPropertyInput}
                        {entityRelationInput}
                        <AutoComplete
                            placeholder="Target entity type"
                            className="ecc-silk-mapping__ruleseditor__targetEntityType"
                            entity="targetEntityType"
                            isValidNewOption={newValueIsIRI}
                            ruleId={autoCompleteRuleId}
                            value={modifiedValues.targetEntityType}
                            multi // allow multi selection
                            creatable
                            onChange={value => { this.handleChangeValue('targetEntityType', value); }}
                        />
                        {patternInput}
                        {sourcePropertyInput}
                        {
                            // Data preview
                            (modifiedValues.pattern || modifiedValues.uriRule) && (
                                <ExampleView
                                    id={parentId || 'root'}
                                    rawRule={
                                        // when not "pattern" then it is "uriRule"
                                        modifiedValues.pattern ?
                                            {
                                                type: MAPPING_RULE_TYPE_URI,
                                                pattern: modifiedValues.pattern,
                                            }
                                            : modifiedValues.uriRule
                                    }
                                    ruleType={
                                        // when not "pattern" then it is "uriRule"
                                        modifiedValues.pattern ? MAPPING_RULE_TYPE_URI : modifiedValues.uriRule.type
                                    }
                                />
                            )
                        }
                        <TextField
                            label="Label"
                            className="ecc-silk-mapping__ruleseditor__label"
                            value={modifiedValues.label}
                            onChange={({ value }) => { this.handleChangeValue('label', value); }}
                        />
                        <TextField
                            multiline
                            label="Description"
                            className="ecc-silk-mapping__ruleseditor__comment"
                            value={modifiedValues.comment}
                            onChange={({ value }) => { this.handleChangeValue('comment', value); }}
                        />
                    </CardContent>
                    <CardActions className="ecc-silk-mapping__ruleseditor__actionrow">
                        <AffirmativeButton
                            className="ecc-silk-mapping__ruleseditor__actionrow-save"
                            raised
                            onClick={this.handleConfirm}
                            disabled={!allowConfirm || !changed}
                        >
                            Save
                        </AffirmativeButton>
                        <DismissiveButton
                            className="ecc-silk-mapping__ruleseditor__actionrow-cancel"
                            raised
                            onClick={this.handleClose}
                        >
                            Cancel
                        </DismissiveButton>
                    </CardActions>
                </Card>
            </div>
        );
    }
}

export default ScrollingHOC(ObjectMappingRuleForm);
