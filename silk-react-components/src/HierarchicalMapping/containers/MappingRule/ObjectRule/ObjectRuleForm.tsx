import React, {useCallback, useEffect, useState} from 'react';
import {Card, CardActions, CardContent, CardTitle, RadioGroup, ScrollingHOC, Spinner,} from '@eccenca/gui-elements';
import {Button, FieldItem, Notification, Spacing, TextField} from "gui-elements";
import {
    AffirmativeButton,
    DismissiveButton,
    Radio,
    TextField as LegacyTextField,
} from "gui-elements/legacy-replacements";
import _ from 'lodash';
import ExampleView from '../ExampleView';
import {ParentElement} from '../../../components/ParentElement';
import {
    checkUriPatternValidity,
    checkValuePathValidity,
    createMappingAsync,
    fetchUriPatternAutoCompletions,
    fetchValuePathSuggestions, updateVocabularyCacheEntry,
    useApiDetails
} from '../../../store';
import {convertToUri} from '../../../utils/convertToUri';
import ErrorView from '../../../components/ErrorView';
import AutoComplete from '../../../components/AutoComplete';
import {MAPPING_RULE_TYPE_ROOT, MAPPING_RULE_TYPE_URI, MESSAGES,} from '../../../utils/constants';
import EventEmitter from '../../../utils/EventEmitter';
import {trimValue} from '../../../utils/trimValue';
import {wasTouched} from '../../../utils/wasTouched';
import {newValueIsIRI} from '../../../utils/newValueIsIRI';
import TargetCardinality from "../../../components/TargetCardinality";
import MultiAutoComplete from "../../../components/MultiAutoComplete";
import AutoSuggestion from "../../../components/AutoSuggestion/AutoSuggestion";
import silkApi from "../../../../api/silkRestApi";
import {IUriPattern} from "../../../../api/types";
import {UriPatternSelectionModal} from "./UriPatternSelectionModal";

interface IProps {
    id?: string
    parentId?: string
    scrollIntoView: ({topOffset: number}) => any
    onAddNewRule: (callback: () => any) => any
    scrollElementIntoView: () => any
    ruleData: object
    parent: any
}

// Extracts the pure URI string if it has the form "<...>"
const pureUri = (uri: string) => uri ? uri.replace(/^<|>$/g, "") : uri

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
    const [uriPatternIsValid, setUriPatternIsValid] = useState<boolean>(true)
    const [objectPathValid, setObjectPathValid] = useState<boolean>(true)
    // When creating a new rule only when this is enabled the URI pattern input will be shown
    const [createCustomUriPatternForNewRule, setCreateCustomUriPatternForNewRule] = useState<boolean>(false)
    const [uriPatternSuggestions, setUriPatternSuggestions] = useState<IUriPattern[]>([])
    const [showUriPatternModal, setShowUriPatternModal] = useState<boolean>(false)
    const [targetEntityTypeOptions] = useState<Map<string, any>>(new Map())
    const {baseUrl, project, transformTask} = useApiDetails()
    const { id, parentId, parent } = props;

    const autoCompleteRuleId = id || parentId;

    const distinctUriPatterns = Array.from(new Map(uriPatternSuggestions
        .filter(p => p.value !== (modifiedValues as any).pattern)
        .map(p => [p.value, p])).values())

    useEffect(() => {
        const { id, scrollIntoView } = props;
        // set screen focus to this element
        scrollIntoView({ topOffset: 75 });
        if (!id) {
            EventEmitter.emit(MESSAGES.RULE_VIEW.CHANGE, { id: 0 });
        }
    }, [])

    const uriValue = (uri: string) => {
        if(!uri) {
            return uri
        }
        return uri.replaceAll(/(^<)|(>$)/g, "")
    }

    // Fetch labels for target entity types
    useEffect(() => {
        if(modifiedValues.targetEntityType && modifiedValues.targetEntityType.length > 0 && baseUrl !== undefined && project && transformTask) {
            modifiedValues.targetEntityType.forEach((targetEntityType) => {
                if(typeof targetEntityType === "string") {
                    const value = uriValue(targetEntityType)
                    if(value !== targetEntityType || value.includes(":")) {
                        fetchTargetEntityUriInfo(value, targetEntityType)
                    }
                }
            })
        }
    }, [id, parentId, !!modifiedValues.targetEntityType, baseUrl, project, transformTask])

    const fetchTargetEntityUriInfo = async (uri: string, originalTargetEntityType: string) => {
        const {data} = await silkApi.retrieveTargetVocabularyTypeOrPropertyInfo(baseUrl!!, project!!, transformTask!!, uri)
        if(data?.genericInfo) {
            const info = data?.genericInfo
            const typeInfo = {value: info.uri, label: info.label ?? info.uri, description: info.description}
            targetEntityTypeOptions?.set(info.uri, typeInfo)
            targetEntityTypeOptions?.set(uri, typeInfo)
            updateVocabularyCacheEntry(originalTargetEntityType, info.label, info.description)
            setModifiedValues(old => ({
                ...old,
                targetEntityType: old.targetEntityType.map(o => {
                    const value = typeof o === "string" ? uriValue(o) : uriValue(o.value)
                    if (targetEntityTypeOptions.has(value)) {
                        return targetEntityTypeOptions.get(value)
                    } else {
                        return o
                    }
                })
            }))
        }
    }

    const targetClassUris = () => modifiedValues.targetEntityType.map(t => typeof t === "string" ? pureUri(t) : pureUri(t.value))

    useEffect(() => {
        if(modifiedValues.targetEntityType && modifiedValues.targetEntityType.length > 0 && baseUrl !== undefined && project) {
            silkApi.uriPatternsByTypes(baseUrl, project, targetClassUris())
                .then(result => {
                    setUriPatternSuggestions(result.data.results)
                })
        }
    }, [modifiedValues.targetEntityType ? targetClassUris().join("") : "", baseUrl])

    /**
     * Saves the modified data
     */
    const handleConfirm = (event) => {
        event.stopPropagation();
        event.persist();
        const uriPattern = trimValue(modifiedValues.pattern)
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
            // Reset URI pattern if it was previously set and is now empty
            pattern: modifiedValues.uriRule?.type === MAPPING_RULE_TYPE_URI && !uriPattern ? null : uriPattern,
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

        if (loading) {
            return <Spinner />;
        }

        const allowConfirm =
            modifiedValues.type === MAPPING_RULE_TYPE_ROOT || !_.isEmpty(modifiedValues.targetProperty) || modifiedValues.sourceProperty && !_.isEmpty(modifiedValues.sourceProperty.trim());
        const errorMessage = saveObjectError && (
            <ErrorView {...saveObjectError} />
        );

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
                    fetchSuggestions={(input, cursorPosition) => fetchValuePathSuggestions(parentId, input, cursorPosition)}
                    checkInput={checkValuePathValidity}
                    onInputChecked={setObjectPathValid}
                />
            );
        }

    let patternInput: JSX.Element | undefined

    // URI pattern
    if (!id || modifiedValues.uriRuleType === 'uri') {
        if (!modifiedValues.pattern && !createCustomUriPatternForNewRule && (!id || !(props.ruleData as any).pattern)) {
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
                    fetchUriPatternAutoCompletions(parentId ? parentId : "root", input, cursorPosition, modifiedValues.sourceProperty)}
                checkInput={checkUriPattern}
                rightElement={distinctUriPatterns.length > 0 ? <>
                    <Spacing vertical={true} size={"tiny"} />
                    <Button
                        data-test-id="object-rule-form-uri-pattern-selection-btn"
                        elevated={true}
                        tooltip={`Choose URI pattern from ${distinctUriPatterns.length} existing URI pattern/s.`}
                        onClick={() => setShowUriPatternModal(true)}
                    >Choose</Button>
                </> : undefined}
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

    let previewExamples: null | JSX.Element = null

    if(!modifiedValues.pattern && (!modifiedValues.uriRule || modifiedValues.uriRule.type === MAPPING_RULE_TYPE_URI)) {
        previewExamples =
            <Notification data-test-id={"object-rule-form-preview-no-pattern"}>No preview shown for default URI pattern.</Notification>
    } else if (!uriPatternIsValid || !objectPathValid) {
        previewExamples =
            <Notification warning={true} data-test-id={"object-rule-form-preview-invalid-input"}>URI pattern or value
                path is invalid. No preview shown.</Notification>
    } else if (modifiedValues.pattern || modifiedValues.uriRule) {
        previewExamples = <ExampleView
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
            objectSourcePathContext={modifiedValues.sourceProperty}
        />
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
                        {showUriPatternModal && distinctUriPatterns.length > 0 && <UriPatternSelectionModal
                            onClose={() => setShowUriPatternModal(false)}
                            uriPatterns={distinctUriPatterns}
                            onSelect={uriPattern => handleChangeValue('pattern', uriPattern.value)}
                        />}
                        {
                            <FieldItem data-test-id="object-rule-form-example-preview" labelAttributes={{text: "Examples of target data"}}>
                                {previewExamples}
                            </FieldItem>
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
                            disabled={!allowConfirm || !changed || !uriPatternIsValid && modifiedValues.pattern || !objectPathValid}
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
