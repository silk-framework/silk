import React, {useEffect, useState} from 'react';
import {
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
import {
    AffirmativeButton,
    DismissiveButton,
} from '@gui-elements/legacy-replacements';
import _ from 'lodash';
import ExampleView from '../ExampleView';
import store from '../../../store';
import { convertToUri } from '../../../utils/convertToUri';
import ErrorView from '../../../components/ErrorView';
import AutoComplete from '../../../components/AutoComplete';
import {
    trimValue,
} from '../../../utils/trimValue';
import { MAPPING_RULE_TYPE_COMPLEX, MAPPING_RULE_TYPE_DIRECT, MESSAGES } from '../../../utils/constants';
import EventEmitter from '../../../utils/EventEmitter';
import { wasTouched } from '../../../utils/wasTouched';
import { newValueIsIRI } from '../../../utils/newValueIsIRI';

const LANGUAGES_LIST = [
    'en', 'de', 'es', 'fr', 'bs', 'bg', 'ca', 'ce', 'zh', 'hr', 'cs', 'da', 'nl', 'eo', 'fi', 'ka', 'el', 'hu', 'ga', 'is', 'it',
    'ja', 'kn', 'ko', 'lb', 'no', 'pl', 'pt', 'ru', 'sk', 'sl', 'sv', 'tr', 'uk',
];

// The type of the value of the mapping rule, e.g. string, URI
interface IValueType {
    // The type ID
    nodeType: string,
    // If this is a lang type value, this property specifies the language code
    lang?: string
}

interface IState {
    // If the form is loading
    loading: boolean
    // If the rule has been changed
    changed: boolean
    // The type of the mapping rule
    type: string
    valueType: IValueType
    // The source path / property
    sourceProperty: string
    // Optional comment
    comment?: string
    // Optional label
    label?: string
    // The target property / attribute
    targetProperty?: string
    // If the target property is an attribute (only relevant for XML as target)
    isAttribute: boolean
}

interface IProps {
    // ID of the rule
    id?: string
    //
    scrollIntoView: (({topOffset: number}) => any)
    parentId?: string
    onAddNewRule?: (call: () => any) => any
}

/** The edit form of a value mapping rule. */
export function ValueRuleForm(props: IProps) {
    const [loading, setLoading] = useState<boolean>(false)
    const [changed, setChanged] = useState(false)
    const [type, setType] = useState(MAPPING_RULE_TYPE_DIRECT)
    const [valueType, setValueType] = useState<IValueType>({ nodeType: 'StringValueType'})
    const [sourceProperty, setSourceProperty] = useState<string | {value: string}>("")
    const [isAttribute, setIsAttribute] = useState(false)
    const [initialValues, setInitialValues] = useState<Partial<IState>>({})
    const [error, setError] = useState<any>(null)
    const [label, setLabel] = useState<string>("")
    const [comment, setComment] = useState<string>("")
    const [targetProperty, setTargetProperty] = useState<string>("")

    const state = {
        loading,
        changed,
        type,
        valueType,
        sourceProperty,
        isAttribute,
        initialValues,
        error,
        label,
        comment,
        targetProperty
    }

    useEffect(() => {
        loadData()
    }, [])

    useEffect(() => {
        if(!loading) {
            props.scrollIntoView({
                topOffset: 75,
            });
        }
    }, [loading])

    const loadData = () => {
        setLoading(true)
        if (props.id) {
            store.getRuleAsync(props.id)
                .subscribe(
                    ({ rule }) => {
                        const initialValues: Partial<IState> = {
                            type: _.get(rule, 'type', MAPPING_RULE_TYPE_DIRECT) as string,
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

                        setType(initialValues.type)
                        setComment(initialValues.comment)
                        setLabel(initialValues.label)
                        setTargetProperty(initialValues.targetProperty)
                        setValueType(initialValues.valueType)
                        setSourceProperty(initialValues.sourceProperty)
                        setIsAttribute(initialValues.isAttribute)
                        setInitialValues(initialValues)
                        setLoading(false)
                    },
                    err => {
                        setLoading(false)
                    }
                );
        } else {
            setLoading(false)
            EventEmitter.emit(MESSAGES.RULE_VIEW.CHANGE, { id: 0 });
        }
    }

    const handleConfirm = event => {
        event.stopPropagation();
        event.persist();
        setLoading(true)
        store.createMappingAsync({
            id: props.id,
            parentId: props.parentId,
            type: type,
            comment: comment,
            label: label,
            targetProperty: trimValue(targetProperty),
            valueType: valueType,
            sourceProperty: trimValue(sourceProperty),
            isAttribute: isAttribute,
        }).subscribe(
            () => {
                if (props.onAddNewRule) {
                    props.onAddNewRule(() => {
                        handleCloseWithChanges()
                    });
                } else {
                    handleCloseWithChanges()
                }
            },
            err => {
                setError(err)
                setLoading(false)
            }
        );
    }

    // remove rule
    const handleChangeTextfield = (statePropertyName: string, setValueFunction: (v: any) => void, { value }) => {
        handleChangeValue(statePropertyName, value, setValueFunction);
    };

    const handleChangeSelectBox = (statePropertyName: string, setValueFunction: (v: any) => void, value) => {
        handleChangeValue(statePropertyName, value, setValueFunction);
    };

    const handleChangePropertyType = value => {
        const valueType = { nodeType: value.value };
        handleChangeValue('valueType', valueType, setValueType);
    };

    const handleChangeLanguageTag = value => {
        let lang = value;
        if (typeof lang === 'object') {
            lang = value.value;
        }
        const valueType = { nodeType: 'LanguageValueType', lang };
        handleChangeValue('valueType', valueType, setValueType);
    };

    const handleChangeValue = (stateProperty: string, value, setValueFunction: (v: any) => void) => {
        const { initialValues, ...currValues } = state;
        currValues[stateProperty] = value;

        const touched = wasTouched(initialValues, currValues);
        const id = _.get(props, 'id', 0);

        if (id !== 0) {
            if (touched) {
                EventEmitter.emit(MESSAGES.RULE_VIEW.CHANGE, { id });
            } else {
                EventEmitter.emit(MESSAGES.RULE_VIEW.UNCHANGED, { id });
            }
        }

        setValueFunction(value)
        setChanged(touched)
    };

    const handleClose = () => {
        const id = _.get(props, 'id', 0);
        EventEmitter.emit(MESSAGES.RULE_VIEW.UNCHANGED, { id });
        EventEmitter.emit(MESSAGES.RULE_VIEW.CLOSE, { id });
    };

    // Closes the edit form. Reacts to changes in the mapping rules.
    const handleCloseWithChanges = () => {
        handleClose()
        EventEmitter.emit(MESSAGES.RELOAD, true)
    }

    const allowConfirmation = () => {
        const targetPropertyNotEmpty = !_.isEmpty(targetProperty);
        const languageTagSet = valueType.nodeType !== 'LanguageValueType' || typeof valueType.lang === 'string';
        return targetPropertyNotEmpty && languageTagSet;
    }

    // template rendering
    const render = () => {
        const { id, parentId } = props;

        const autoCompleteRuleId = id || parentId;

        if (loading) {
            return <Spinner />;
        }
        const errorMessage = error ?
            <ErrorView {...error.response.body} />
            :
            false
        ;

        const allowConfirm = allowConfirmation();

        const title = !id ? <CardTitle>Add value mapping</CardTitle> : false;

        // TODO: Unfold complex mapping
        let sourcePropertyInput: React.ReactElement | undefined = undefined;

        if (type === MAPPING_RULE_TYPE_DIRECT) {
            sourcePropertyInput = (
                <AutoComplete
                    placeholder="Value path"
                    className="ecc-silk-mapping__ruleseditor__sourcePath"
                    entity="sourcePath"
                    creatable
                    value={sourceProperty}
                    ruleId={autoCompleteRuleId}
                    onChange={handleChangeSelectBox.bind(
                        null,
                        'sourceProperty',
                        setSourceProperty
                    )}
                    resetQueryToValue={true}
                    itemDisplayLabel={(item) => item.label ? `${item.label} <${item.value}>` : item.value}
                />
            );
        } else if (type === MAPPING_RULE_TYPE_COMPLEX) {
            sourcePropertyInput = (
                <TextField
                    data-id="test-complex-input"
                    disabled
                    label="Value formula"
                    value="The value formula cannot be modified in the edit form."
                />
            );
        }
        const exampleView = !_.isEmpty(sourceProperty) ? (
            <ExampleView
                id={props.parentId || 'root'}
                key={
                    typeof sourceProperty === "string" ? sourceProperty : sourceProperty.value
                }
                rawRule={state}
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
                            value={targetProperty}
                            ruleId={autoCompleteRuleId}
                            onChange={handleChangeSelectBox.bind(
                                null,
                                'targetProperty',
                                setTargetProperty
                            )}
                            resetQueryToValue={true}
                            itemDisplayLabel={(item) => item.label ? `${item.label} <${item.value}>` : item.value}
                        />
                        <Checkbox
                            checked={isAttribute}
                            className="ecc-silk-mapping__ruleseditor__isAttribute"
                            onChange={() => handleChangeValue('isAttribute', !isAttribute, setIsAttribute)}
                        >
                            Write values as attributes (if supported by the
                            target dataset)
                        </Checkbox>
                        <AutoComplete
                            placeholder="Data type"
                            className="ecc-silk-mapping__ruleseditor__propertyType"
                            entity="propertyType"
                            ruleId={autoCompleteRuleId}
                            value={valueType.nodeType}
                            clearable={false}
                            onChange={handleChangePropertyType}
                        />
                        { (valueType.nodeType === 'LanguageValueType') &&
                        <SelectBox
                            data-id="lng-select-box"
                            placeholder="Language Tag"
                            options={LANGUAGES_LIST}
                            optionsOnTop={true} // option list opens up on top of select input (default: false)
                            value={valueType.lang}
                            onChange={handleChangeLanguageTag}
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
                            value={label}
                            onChange={handleChangeTextfield.bind(
                                null,
                                'label',
                                setLabel
                            )}
                        />
                        <TextField
                            multiline
                            label="Description"
                            className="ecc-silk-mapping__ruleseditor__comment"
                            value={comment}
                            onChange={handleChangeTextfield.bind(
                                null,
                                'comment',
                                setComment
                            )}
                        />
                    </CardContent>
                    <CardActions className="ecc-silk-mapping__ruleseditor__actionrow">
                        <AffirmativeButton
                            className="ecc-silk-mapping__ruleseditor__actionrow-save"
                            raised
                            onClick={handleConfirm}
                            disabled={!allowConfirm || !changed && !!id}
                        >
                            Save
                        </AffirmativeButton>
                        <DismissiveButton
                            className="ecc-silk-mapping__ruleseditor___actionrow-cancel"
                            raised
                            onClick={handleClose}
                        >
                            Cancel
                        </DismissiveButton>
                    </CardActions>
                </Card>
            </div>
        );
    }
    return render()
}
export default ScrollingHOC(ValueRuleForm);
