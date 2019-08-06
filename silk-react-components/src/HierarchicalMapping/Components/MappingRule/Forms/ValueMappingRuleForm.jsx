import React from 'react';
import {
    AffirmativeButton,
    DismissiveButton,
    Card,
    CardTitle,
    CardContent,
    CardActions,
    TextField,
    Spinner,
    ScrollingHOC,
    Checkbox,
    SelectBox,
} from '@eccenca/gui-elements';
import { URI } from 'ecc-utils';
import _ from 'lodash';
import ExampleView from '../ExampleView';
import UseMessageBus from '../../../UseMessageBusMixin';
import hierarchicalMappingChannel from '../../../store';
import { newValueIsIRI, wasTouched, convertToUri } from './helpers';
import ErrorView from '../ErrorView';
import AutoComplete from './AutoComplete';
import {
    MAPPING_RULE_TYPE_COMPLEX,
    MAPPING_RULE_TYPE_DIRECT,
    trimValueLabelObject,
} from '../../../helpers';
import { MESSAGES } from '../../../constants';

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
    componentDidUpdate(prevProps, prevState) {
        if (
            prevState.loading === true &&
            _.get(this.state, 'loading', false) === false
        ) {
            this.props.scrollIntoView({
                topOffset: 75,
            });
        }
    },

    loadData() {
        if (this.props.id) {
            hierarchicalMappingChannel
                .request({
                    topic: MESSAGES.RULE.GET,
                    data: {
                        id: this.props.id,
                    },
                })
                .subscribe(
                    ({ rule }) => {
                        const initialValues = {
                            type: _.get(rule, 'type', MAPPING_RULE_TYPE_DIRECT),
                            comment: _.get(rule, 'metadata.description', ''),
                            label: _.get(rule, 'metadata.label', ''),
                            targetProperty: _.get(
                                rule,
                                'mappingTarget.uri',
                                ''
                            ),
                            valueType: _.get(
                                rule,
                                'mappingTarget.valueType',
                                { nodeType: 'StringValueType' }
                            ),
                            sourceProperty: rule.sourcePath,
                            isAttribute: _.get(
                                rule,
                                'mappingTarget.isAttribute',
                                false
                            ),
                        };

                        this.setState({
                            loading: false,
                            ...initialValues,
                            initialValues,
                        });
                    },
                    err => {
                        this.setState({ loading: false });
                    }
                );
        } else {
            hierarchicalMappingChannel
                .subject(MESSAGES.RULE_VIEW.CHANGE)
                .onNext({ id: 0 });
            this.setState({
                create: true,
                loading: false,
                type: MAPPING_RULE_TYPE_DIRECT,
                valueType: { nodeType: 'StringValueType' },
                sourceProperty: '',
                isAttribute: false,
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
                topic: MESSAGES.RULE.CREATE_VALUE_MAPPING,
                data: {
                    id: this.props.id,
                    parentId: this.props.parentId,
                    type: this.state.type,
                    comment: this.state.comment,
                    label: this.state.label,
                    targetProperty: trimValueLabelObject(this.state.targetProperty),
                    valueType: this.state.valueType,
                    sourceProperty: trimValueLabelObject(this.state.sourceProperty),
                    isAttribute: this.state.isAttribute,
                },
            })
            .subscribe(
                () => {
                    this.handleClose(event);
                    hierarchicalMappingChannel.subject(MESSAGES.RELOAD).onNext(true);
                },
                err => {
                    this.setState({
                        error: err,
                        loading: false,
                    });
                }
            );
    },
    // remove rule
    handleChangeTextfield(state, { value }) {
        this.handleChangeValue(state, value);
    },
    handleChangeSelectBox(state, value) {
        this.handleChangeValue(state, value);
    },
    handleChangePropertyType(value) {
        const valueType = { nodeType: value.value };
        this.handleChangeValue('valueType', valueType);
    },
    handleChangeLanguageTag(value) {
        let lang = value;
        if (typeof lang === 'object') {
            lang = value.value;
        }
        const valueType = { nodeType: 'LanguageValueType', lang };
        this.handleChangeValue('valueType', valueType);
    },
    handleChangeValue(name, value) {
        const { initialValues, create, ...currValues } = this.state;
        currValues[name] = value;

        const touched = create || wasTouched(initialValues, currValues);
        const id = _.get(this.props, 'id', 0);

        if (id !== 0) {
            if (touched) {
                hierarchicalMappingChannel
                    .subject(MESSAGES.RULE_VIEW.CHANGE)
                    .onNext({ id });
            } else {
                hierarchicalMappingChannel
                    .subject(MESSAGES.RULE_VIEW.UNCHANGED)
                    .onNext({ id });
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
        hierarchicalMappingChannel.subject(MESSAGES.RULE_VIEW.UNCHANGED).onNext({ id });
        hierarchicalMappingChannel.subject(MESSAGES.RULE_VIEW.CLOSE).onNext({ id });
    },
    allowConfirmation() {
        const targetPropertyNotEmpty = !_.isEmpty(this.state.targetProperty);
        const valueType = this.state.valueType;
        const languageTagSet = valueType.nodeType !== 'LanguageValueType' || typeof valueType.lang === 'string';
        return targetPropertyNotEmpty && languageTagSet;
    },
    // template rendering
    render() {
        const { id, parentId } = this.props;

        const autoCompleteRuleId = id || parentId;

        const { type, error } = this.state;

        if (this.state.loading) {
            return <Spinner />;
        }

        const errorMessage = error ? (
            <ErrorView {...error.response.body} />
        ) : (
            false
        );

        const allowConfirm = this.allowConfirmation();

        const title = !id ? <CardTitle>Add value mapping</CardTitle> : false;

        // TODO: Unfold complex mapping
        let sourcePropertyInput = false;

        if (type === MAPPING_RULE_TYPE_DIRECT) {
            sourcePropertyInput = (
                <AutoComplete
                    placeholder="Value path"
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
        } else if (type === MAPPING_RULE_TYPE_COMPLEX) {
            sourcePropertyInput = (
                <TextField
                    disabled
                    label="Value formula"
                    value="The value formula cannot be modified in the edit form."
                />
            );
        }
        const exampleView = !_.isEmpty(this.state.sourceProperty) ? (
            <ExampleView
                id={this.props.parentId || 'root'}
                key={
                    this.state.sourceProperty.value || this.state.sourceProperty
                }
                rawRule={this.state}
                ruleType={type}
            />
        ) : (
            false
        );

        return (
            <div className="ecc-silk-mapping__ruleseditor">
                <Card shadow={!id ? 1 : 0}>
                    {title}
                    <CardContent>
                        {errorMessage}
                        <AutoComplete
                            placeholder="Target property"
                            className="ecc-silk-mapping__ruleseditor__targetProperty"
                            entity="targetProperty"
                            newOptionCreator={convertToUri}
                            isValidNewOption={newValueIsIRI}
                            creatable
                            value={this.state.targetProperty}
                            ruleId={autoCompleteRuleId}
                            onChange={this.handleChangeSelectBox.bind(
                                null,
                                'targetProperty'
                            )}
                        />
                        <Checkbox
                            checked={this.state.isAttribute}
                            className="ecc-silk-mapping__ruleseditor__isAttribute"
                            onChange={this.handleChangeValue.bind(
                                null,
                                'isAttribute',
                                !this.state.isAttribute
                            )}
                        >
                            Write values as attributes (if supported by the
                            target dataset)
                        </Checkbox>
                        <AutoComplete
                            placeholder="Data type"
                            className="ecc-silk-mapping__ruleseditor__propertyType"
                            entity="propertyType"
                            ruleId={autoCompleteRuleId}
                            value={this.state.valueType.nodeType}
                            clearable={false}
                            onChange={this.handleChangePropertyType}
                        />
                        { (this.state.valueType.nodeType === 'LanguageValueType') &&
                            <SelectBox
                                placeholder="Language Tag"
                                options={[
                                    'en',
                                    'de',
                                    'es',
                                    'fr',
                                    'bs',
                                    'bg',
                                    'ca',
                                    'ce',
                                    'zh',
                                    'hr',
                                    'cs',
                                    'da',
                                    'nl',
                                    'eo',
                                    'fi',
                                    'ka',
                                    'el',
                                    'hu',
                                    'ga',
                                    'is',
                                    'it',
                                    'ja',
                                    'kn',
                                    'ko',
                                    'lb',
                                    'no',
                                    'pl',
                                    'pt',
                                    'ru',
                                    'sk',
                                    'sl',
                                    'sv',
                                    'tr',
                                    'uk']}
                                optionsOnTop={true} // option list opens up on top of select input (default: false)
                                value={this.state.valueType.lang}
                                onChange={this.handleChangeLanguageTag}
                                isValidNewOption={({ label = '' }) =>
                                    !_.isNull(label.match(/^[a-z]{2}(-[A-Z]{2})?$/))
                                }
                                creatable={true} // allow creation of new values
                                noResultsText="Not a valid language tag"
                                promptTextCreator={newLabel => (`Create language tag: ${newLabel}`)}
                                multi={false} // allow multi selection
                                clearable={false} // hide 'remove all selected values' button
                                searchable={true} // whether to behave like a type-ahead or not
                            />
                        }
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
                            disabled={!allowConfirm || !this.state.changed}
                        >
                            Save
                        </AffirmativeButton>
                        <DismissiveButton
                            className="ecc-silk-mapping__ruleseditor___actionrow-cancel"
                            raised
                            onClick={this.handleClose}
                        >
                            Cancel
                        </DismissiveButton>
                    </CardActions>
                </Card>
            </div>
        );
    },
});

export default ScrollingHOC(ValueMappingRuleForm);
