import React, {Component, useEffect, useState} from 'react';
import PropTypes from 'prop-types';
import {
    Card,
    CardTitle,
    CardContent,
    CardActions,
    RadioGroup,
    Spinner,
    ScrollingHOC,
} from '@eccenca/gui-elements';
import {
    Button,
    FieldItem, TextField
} from '@gui-elements/index';
import {
    AffirmativeButton,
    DismissiveButton,
    Radio,
    TextField as LegacyTextField,
} from '@gui-elements/legacy-replacements';
import _ from 'lodash';
import ExampleView from '../ExampleView';
import { ParentElement } from '../../../components/ParentElement';
import {
    checkUriPatternValidity,
    checkValuePathValidity,
    createMappingAsync,
    fetchUriPatternAutoCompletions,
    fetchValuePathSuggestions
} from '../../../store';
import { convertToUri } from '../../../utils/convertToUri';
import ErrorView from '../../../components/ErrorView';
import AutoComplete from '../../../components/AutoComplete';
import {
    MAPPING_RULE_TYPE_ROOT,
    } from '../../../utils/constants';
import { MAPPING_RULE_TYPE_URI, MESSAGES } from '../../../utils/constants';
import EventEmitter from '../../../utils/EventEmitter';
import { trimValue } from '../../../utils/trimValue';
import { wasTouched } from '../../../utils/wasTouched';
import { newValueIsIRI } from '../../../utils/newValueIsIRI';
import TargetCardinality from "../../../components/TargetCardinality";
import MultiAutoComplete from "../../../components/MultiAutoComplete";
import AutoSuggestion, {IReplacementResult} from "../../../components/AutoSuggestion/AutoSuggestion";

interface IProps {
    id?: string
    parentId: string
    scrollIntoView: ({topOffset: number}) => any
    onAddNewRule: (callback: () => any) => any
    scrollElementIntoView: () => any
    ruleData: object
    parent: any
}

/**
 * Provides the editable form for object mappings.
 */
export const ObjectRuleForm = (props: IProps) => {
    const [loading, setLoading] = useState(false)
    const [changed, setChanged] = useState(false)
    const create = !props.id
    // get a deep copy of origin data for modification
    const [modifiedValues, setModifiedValues] = useState<any>(_.cloneDeep(props.ruleData))
    const [saveObjectError, setSaveObjectError] = useState<any>(undefined)
    const [uriPatternHasFocus, setUriPatternHasFocus] = useState<boolean>(false)
    const [uriPatternIsValid, setUriPatternIsValid] = useState<boolean>(true)
    // When creating a new rule only when this is enabled the URI pattern input will be shown
    const [createCustomUriPatternForNewRule, setCreateCustomUriPatternForNewRule] = useState<boolean>(false)

    useEffect(() => {
        const { id, scrollIntoView } = props;
        // set screen focus to this element
        scrollIntoView({ topOffset: 75 });
        if (!id) {
            EventEmitter.emit(MESSAGES.RULE_VIEW.CHANGE, { id: 0 });
        }
    }, [])

    /**
     * Saves the modified data
     */
    const handleConfirm = (event) => {
        event.stopPropagation();
        event.persist();
        setLoading(true)
        createMappingAsync({
            id: props.id,
            parentId: props.parentId,
            type: modifiedValues.type,
            comment: modifiedValues.comment,
            label: modifiedValues.label,
            sourceProperty: trimValue(modifiedValues.sourceProperty),
            targetProperty: trimValue(modifiedValues.targetProperty),
            targetEntityType: modifiedValues.targetEntityType,
            isAttribute: modifiedValues.isAttribute,
            pattern: trimValue(modifiedValues.pattern),
            entityConnection: modifiedValues.entityConnection === 'to',
        }, true)
            .subscribe(
                () => {
                    if (props.onAddNewRule) {
                        props.onAddNewRule(() => {
                            handleClose(event);
                        });
                    } else {
                        handleClose(event);
                    }
                },
                err => {
                    setSaveObjectError(err.response.body)
                    setLoading(false)
                }
            );
    }

    /**
     * Handle input changes from user
     */
    const handleChangeValue = (name, value) => {
        const { id, ruleData } = props;

        const newModifiedValues = {
            ...modifiedValues,
            [name]: value
        };

        const changed = create || wasTouched(ruleData, newModifiedValues);

        if (id) {
            if (changed) {
                EventEmitter.emit(MESSAGES.RULE_VIEW.CHANGE, { id });
            } else {
                EventEmitter.emit(MESSAGES.RULE_VIEW.UNCHANGED, { id });
            }
        }
        setChanged(changed)
        setModifiedValues(newModifiedValues)
    }

    /**
     * handle form close event
     * @param event
     */
    const handleClose = (event) => {
        event.stopPropagation();
        const { id = 0 } = props;
        EventEmitter.emit(MESSAGES.RULE_VIEW.UNCHANGED, { id });
        EventEmitter.emit(MESSAGES.RULE_VIEW.CLOSE, { id });
    }

    const checkUriPattern = async(uriPattern: string) => {
        const validationResult = await checkUriPatternValidity(uriPattern)
        if(validationResult?.valid !== undefined) {
            setUriPatternIsValid(validationResult?.valid as boolean)
        }
        return validationResult
    }

        const { id, parentId, parent } = props;

        const autoCompleteRuleId = id || parentId;

        if (loading) {
            return <Spinner />;
        }

        // FIXME: also check if data really has changed before allow saving
        const allowConfirm =
            modifiedValues.type === MAPPING_RULE_TYPE_ROOT || !_.isEmpty(modifiedValues.targetProperty) || modifiedValues.sourceProperty && !_.isEmpty(modifiedValues.sourceProperty.trim());
        const errorMessage = saveObjectError && (
            <ErrorView {...saveObjectError} />
        );

        // TODO: add source path if: parent, not edit, not root element
        const title = !id && <CardTitle>Add object mapping</CardTitle>;

        let targetPropertyInput: JSX.Element | undefined = undefined
        let targetCardinality: JSX.Element | undefined = undefined
        let entityRelationInput: JSX.Element | undefined = undefined
        let sourcePropertyInput: JSX.Element | undefined = undefined

        targetCardinality = (
            <TargetCardinality
                className="ecc-silk-mapping__ruleseditor__isAttribute"
                isAttribute={modifiedValues.isAttribute}
                isObjectMapping={true}
                onChange={(value) => handleChangeValue('isAttribute', value)}
            />
        );

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
                    data-id="autocomplete_target_prop"
                    value={modifiedValues.targetProperty}
                    onChange={value => { handleChangeValue('targetProperty', value); }}
                    resetQueryToValue={true}
                    itemDisplayLabel={(item) => item.label ? `${item.label} <${item.value}>` : item.value}
                />
            );
            entityRelationInput = (
                <FieldItem>
                    <RadioGroup
                        onChange={({ value }) => { handleChangeValue('entityConnection', value); }}
                        value={
                            !_.isEmpty(modifiedValues.entityConnection)
                                ? modifiedValues.entityConnection
                                : 'from'
                        }
                        name=""
                        disabled={false}
                        data-id="entity_radio_group"
                    >
                        <Radio
                            value="from"
                            label={
                                <span>
                                    Connect from{' '}
                                    <ParentElement parent={parent} />
                                </span>
                            }
                        />
                        <Radio
                            value="to"
                            label={
                                <span>
                                    Connect to{' '}
                                    <ParentElement parent={parent} />
                                </span>
                            }
                        />
                    </RadioGroup>
                </FieldItem>
            );
            const valuePath = modifiedValues.sourceProperty == null ? "" : typeof modifiedValues.sourceProperty === "string" ? modifiedValues.sourceProperty : modifiedValues.sourceProperty.value
            sourcePropertyInput = (
                <AutoSuggestion
                    id={"object-value-path-auto-suggestion"}
                    label="Value path"
                    initialValue={valuePath}
                    clearIconText={"Clear value path"}
                    validationErrorText={"The entered value path is invalid."}
                    onChange={value => {
                        handleChangeValue('sourceProperty', value);
                    }}
                    fetchSuggestions={(input, cursorPosition) => fetchValuePathSuggestions(autoCompleteRuleId, input, cursorPosition)}
                    checkInput={checkValuePathValidity}
                />
            );
        }

        let patternInput: JSX.Element | undefined = undefined

    // URI pattern
        if (!id || modifiedValues.uriRuleType === 'uri') {
            if (!id && !createCustomUriPatternForNewRule) {
                patternInput = <FieldItem labelAttributes={{text: "URI pattern"}}>
                    <TextField
                        data-test-id="object-rule-form-default-pattern"
                        disabled
                        value="Default pattern."
                        rightElement={<Button
                            data-test-id="object-rule-form-default-pattern-custom-pattern-btn"
                            onClick={() => setCreateCustomUriPatternForNewRule(true)}
                        >Create custom pattern</Button>}
                    />
                </FieldItem>
            } else {
                patternInput = <AutoSuggestion
                    id={"uri-pattern-auto-suggestion"}
                    label="URI pattern"
                    initialValue={modifiedValues.pattern}
                    clearIconText={"Clear URI pattern"}
                    validationErrorText={"The entered URI pattern is invalid."}
                    onChange={value => {
                        handleChangeValue('pattern', value);
                    }}
                    fetchSuggestions={(input, cursorPosition) =>
                        fetchUriPatternAutoCompletions(autoCompleteRuleId, input, cursorPosition, id ? undefined : modifiedValues.sourceProperty)}
                    checkInput={checkUriPattern}
                    onFocusChange={setUriPatternHasFocus}
                />
            }
        } else {
            patternInput = (
                <LegacyTextField
                    disabled
                    label="URI formula"
                    value="This URI cannot be edited in the edit form."
                />
            );
        }

        return (
            <div className="ecc-silk-mapping__ruleseditor">
                <Card shadow={!id ? 1 : 0}>
                    {title}
                    <CardContent>
                        {errorMessage}
                        {targetPropertyInput}
                        {entityRelationInput}
                        <MultiAutoComplete
                            placeholder="Target entity type"
                            className="ecc-silk-mapping__ruleseditor__targetEntityType"
                            entity="targetEntityType"
                            isValidNewOption={newValueIsIRI}
                            ruleId={autoCompleteRuleId}
                            value={modifiedValues.targetEntityType}
                            creatable
                            onChange={value => { handleChangeValue('targetEntityType', value); }}
                        />
                        {targetCardinality}
                        {patternInput}
                        {
                            // Data preview
                            (!uriPatternHasFocus && uriPatternIsValid && (modifiedValues.pattern || modifiedValues.uriRule)) && (
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
                                    objectSourcePathContext={!id ? modifiedValues.sourceProperty : undefined}
                                />
                            )
                        }
                        {sourcePropertyInput}
                        <LegacyTextField
                            data-test-id={"object-rule-form-label-input"}
                            label="Label"
                            className="ecc-silk-mapping__ruleseditor__label"
                            value={modifiedValues.label}
                            onChange={({ value }) => { handleChangeValue('label', value); }}
                        />
                        <LegacyTextField
                            multiline
                            label="Description"
                            className="ecc-silk-mapping__ruleseditor__comment"
                            value={modifiedValues.comment}
                            onChange={({ value }) => { handleChangeValue('comment', value); }}
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
                            className="ecc-silk-mapping__ruleseditor__actionrow-cancel"
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

export default ScrollingHOC(ObjectRuleForm);
