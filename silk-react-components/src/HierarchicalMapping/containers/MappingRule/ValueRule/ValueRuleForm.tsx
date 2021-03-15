import React, {useEffect, useState} from 'react';
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
import _ from 'lodash';
import ExampleView from '../ExampleView';
import { createMappingAsync, getRuleAsync } from '../../../store';
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
import {AutoCompleteField} from "@gui-elements/index";

const LANGUAGES_LIST = [
    'en', 'de', 'es', 'fr', 'bs', 'bg', 'ca', 'ce', 'zh', 'hr', 'cs', 'da', 'nl', 'eo', 'fi', 'ka', 'el', 'hu', 'ga', 'is', 'it',
    'ja', 'kn', 'ko', 'lb', 'no', 'pl', 'pt', 'ru', 'sk', 'sl', 'sv', 'tr', 'uk',
];

interface INodeType {
    nodeType: string,
    lang?: string
}

interface IState {
    loading: boolean
    changed: boolean
    create: boolean
    type: string
    valueType: {
        nodeType: string
        [key: string]: any
    }
    sourceProperty: string
    comment?: string
    label?: string
    targetProperty?: string
    isAttribute: boolean
}

interface IProps {
    id?: string
    scrollIntoView: (({topOffset: number}) => any)
    parentId?: string
    onAddNewRule?: (call: () => any) => any
}

export function ValueRuleForm(props: IProps) {
    const [loading, setLoading] = useState<boolean>(false)
    const [changed, setChanged] = useState(false)
    const [create, setCreate] = useState(true)
    const [type, setType] = useState(MAPPING_RULE_TYPE_DIRECT)
    const [valueType, setValueType] = useState<INodeType>({ nodeType: 'StringValueType'})
    const [sourceProperty, setSourceProperty] = useState<string | {value: string}>("")
    const [isAttribute, setIsAttribute] = useState(false)
    const [initialValues, setInitialValues] = useState<Partial<IState>>({})
    const [error, setError] = useState<any>(null)
    const [label, setLabel] = useState<string | undefined>(undefined)
    const [comment, setComment] = useState<string | undefined>(undefined)
    const [targetProperty, setTargetProperty] = useState<string | undefined>(undefined)

    const state = {
        loading,
        changed,
        create,
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
            getRuleAsync(props.id)
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
        createMappingAsync({
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
        const { initialValues, create, ...currValues } = state;
        currValues[stateProperty] = value;

        const touched = create || wasTouched(initialValues, currValues);
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
                // <AutoCompleteField
                //     //     className="ecc-silk-mapping__ruleseditor__sourcePath" TODO
                //     inputProps={{
                //         placeholder: "Value path"
                //     }}
                //     onChange={handleChangeSelectBox.bind(
                //         null,
                //         'sourceProperty',
                //         setSourceProperty
                //     )}
                //     initialValue={sourceProperty}
                //     itemValueSelector={item => item}
                //     onSearch={() => []}
                //     itemRenderer={item => item}
                //     itemValueRenderer={item => item}
                //     noResultText={"No result."}
                // />
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
                            disabled={!allowConfirm || !changed}
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
