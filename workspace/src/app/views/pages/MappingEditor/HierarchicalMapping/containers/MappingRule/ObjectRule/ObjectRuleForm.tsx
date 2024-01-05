import React, { useEffect, useState } from "react";
import { Card, CardActions, CardContent, CardTitle, RadioGroup, ScrollingHOC } from "gui-elements-deprecated";
import {
    CodeAutocompleteField,
    Button,
    FieldItem,
    Notification,
    Spacing,
    TextField,
    Spinner,
    TextArea,
} from "@eccenca/gui-elements";
import { AffirmativeButton, DismissiveButton, Radio } from "@eccenca/gui-elements/src/legacy-replacements";
import _ from "lodash";
import ExampleView from "../ExampleView";
import { ParentElement } from "../../../components/ParentElement";
import {
    checkUriPatternValidity,
    checkValuePathValidity,
    createMappingAsync,
    fetchUriPatternAutoCompletions,
    fetchValuePathSuggestions,
    updateVocabularyCacheEntry,
    useApiDetails,
} from "../../../store";
import { convertToUri } from "../../../utils/convertToUri";
import ErrorView from "../../../components/ErrorView";
import AutoComplete from "../../../components/AutoComplete";
import { MAPPING_RULE_TYPE_ROOT, MAPPING_RULE_TYPE_URI, MESSAGES } from "../../../utils/constants";
import EventEmitter from "../../../utils/EventEmitter";
import { trimValue } from "../../../utils/trimValue";
import { wasTouched } from "../../../utils/wasTouched";
import { newValueIsIRI } from "../../../utils/newValueIsIRI";
import TargetCardinality from "../../../components/TargetCardinality";
import MultiAutoComplete from "../../../components/MultiAutoComplete";
import silkApi from "../../../../api/silkRestApi";
import { IUriPattern } from "../../../../api/types";
import { UriPatternSelectionModal } from "./UriPatternSelectionModal";
import { IViewActions } from "../../../../../../../views/plugins/PluginRegistry";
import { defaultUriPattern } from "./ObjectRule.utils";
import taskConfig from "../../../../../../shared/TaskConfig";
import {GlobalMappingEditorContext} from "../../../../contexts/GlobalMappingEditorContext";

interface IProps {
    id?: string;
    parentId?: string;
    scrollIntoView: ({ topOffset }) => any;
    onAddNewRule: (callback: () => any) => any;
    scrollElementIntoView: () => any;
    ruleData: object;
    parent: any;
    viewActions: IViewActions;
}

// Extracts the pure URI string if it has the form "<...>"
const pureUri = (uri: string) => (uri ? uri.replace(/^<|>$/g, "") : uri);

/**
 * Provides the editable form for object mappings.
 */
export const ObjectRuleForm = (props: IProps) => {
    const mappingEditorContext = React.useContext(GlobalMappingEditorContext);
    const [loading, setLoading] = useState(false);
    const [changed, setChanged] = useState(false);
    const [allowConfirm, setAllowConfirm] = useState(false);
    const create = !props.id;
    // get a deep copy of origin data for modification
    const _modifiedValues = React.useRef<any>(_.cloneDeep(props.ruleData));
    const modifiedValues = () => _modifiedValues.current;
    const setModifiedValues = (valueOrFunction: any | ((old: any) => any)) => {
        if (typeof valueOrFunction === "function") {
            _modifiedValues.current = valueOrFunction(_modifiedValues.current);
        } else {
            _modifiedValues.current = valueOrFunction;
        }
    };
    const [targetEntityType, setTargetEntityType] = useState<(string | { value: string })[]>(
        _modifiedValues.current.targetEntityType
    );
    // Used for setting a new URI pattern from the existing URI pattern selection
    const [initialUriPattern, setInitialUriPattern] = useState<string>((props.ruleData as any).pattern ?? "");
    const [saveObjectError, setSaveObjectError] = useState<any>(undefined);
    const [uriPatternIsValid, setUriPatternIsValid] = useState<boolean>(true);
    const [objectPathValid, setObjectPathValid] = useState<boolean>(true);
    // When creating a new rule only when this is enabled the URI pattern input will be shown
    const [createCustomUriPatternForNewRule, setCreateCustomUriPatternForNewRule] = useState<boolean>(false);
    const [uriPatternSuggestions, setUriPatternSuggestions] = useState<IUriPattern[]>([]);
    const [showUriPatternModal, setShowUriPatternModal] = useState<boolean>(false);
    const [targetEntityTypeOptions] = useState<Map<string, any>>(new Map());
    const lastEmittedEvent = React.useRef<string>("");
    const { project, transformTask } = useApiDetails();
    const [valuePathInputHasFocus, setValuePathInputHasFocus] = useState<boolean>(false);
    const [uriPatternInputHasFocus, setUriPatternInputHasFocus] = useState<boolean>(false);
    const { id, parentId, parent } = props;

    const autoCompleteRuleId = id || parentId;

    const distinctUriPatterns = Array.from(
        new Map(
            uriPatternSuggestions.filter((p) => p.value !== (modifiedValues() as any).pattern).map((p) => [p.value, p])
        ).values()
    );

    useEffect(() => {
        const { id, scrollIntoView } = props;
        // set screen focus to this element
        scrollIntoView({ topOffset: 75 });
        if (!id) {
            EventEmitter.emit(MESSAGES.RULE_VIEW.CHANGE, { id: 0 });
        }
    }, []);

    const toggleTabViewDirtyState = React.useCallback((status: boolean) => {
        props.viewActions.savedChanges && props.viewActions.savedChanges(status);
    }, []);

    const uriValue = (uri: string) => {
        if (!uri) {
            return uri;
        }
        return uri.replaceAll(/(^<)|(>$)/g, "");
    };

    // Fetch labels for target entity types
    useEffect(() => {
        if (
            modifiedValues().targetEntityType &&
            modifiedValues().targetEntityType.length > 0 &&
            project &&
            transformTask
        ) {
            modifiedValues().targetEntityType.forEach((targetEntityType) => {
                if (typeof targetEntityType === "string") {
                    const value = uriValue(targetEntityType);
                    if (value !== targetEntityType || value.includes(":")) {
                        fetchTargetEntityUriInfo(value, targetEntityType);
                    }
                }
            });
        }
    }, [id, parentId, !!modifiedValues().targetEntityType, project, transformTask]);

    const fetchTargetEntityUriInfo = async (uri: string, originalTargetEntityType: string) => {
        const { data } = await silkApi.retrieveTargetVocabularyTypeOrPropertyInfo(project!!, transformTask!!, uri);
        if (data?.genericInfo) {
            const info = data?.genericInfo;
            const typeInfo = { value: info.uri, label: info.label ?? info.uri, description: info.description };
            targetEntityTypeOptions?.set(info.uri, typeInfo);
            targetEntityTypeOptions?.set(uri, typeInfo);
            updateVocabularyCacheEntry(originalTargetEntityType, info.label, info.description);
            setModifiedValues((old) => ({
                ...old,
                targetEntityType: old.targetEntityType.map((o) => {
                    const value = typeof o === "string" ? uriValue(o) : uriValue(o.value);
                    if (targetEntityTypeOptions.has(value)) {
                        return targetEntityTypeOptions.get(value);
                    } else {
                        return o;
                    }
                }),
            }));
        }
    };

    const targetClassUris = () => targetEntityType.map((t) => (typeof t === "string" ? pureUri(t) : pureUri(t.value)));

    useEffect(() => {
        if (modifiedValues().targetEntityType && modifiedValues().targetEntityType.length > 0 && project) {
            silkApi.uriPatternsByTypes(project, targetClassUris()).then((result) => {
                setUriPatternSuggestions(result.data.results);
            });
        }
    }, [targetEntityType ? targetClassUris().join("") : ""]);

    /**
     * Saves the modified data
     */
    const handleConfirm = (event) => {
        event.stopPropagation();
        event.persist();
        toggleTabViewDirtyState(false);
        const uriPattern = trimValue(modifiedValues().pattern);
        setLoading(true);
        createMappingAsync(
            {
                id: props.id,
                parentId: props.parentId,
                type: modifiedValues().type,
                comment: modifiedValues().comment,
                label: modifiedValues().label,
                sourceProperty: trimValue(modifiedValues().sourceProperty),
                targetProperty: trimValue(modifiedValues().targetProperty),
                targetEntityType: modifiedValues().targetEntityType,
                isAttribute: modifiedValues().isAttribute,
                // Reset URI pattern if it was previously set and is now empty
                pattern: modifiedValues().uriRule?.type === MAPPING_RULE_TYPE_URI && !uriPattern ? null : uriPattern,
                entityConnection: modifiedValues().entityConnection === "to",
            },
            true
        ).subscribe(
            () => {
                if (props.onAddNewRule) {
                    props.onAddNewRule(() => {
                        handleClose(event);
                    });
                } else {
                    handleClose(event);
                }
            },
            (err) => {
                setSaveObjectError(err.response.body);
                setLoading(false);
            }
        );
    };

    /**
     * Handle input changes from user
     */
    const handleChangeValue = (name, value) => {
        const { id, ruleData } = props;

        const newModifiedValues = {
            ...modifiedValues(),
            [name]: value,
        };

        const changed = create || wasTouched(ruleData, newModifiedValues);

        toggleTabViewDirtyState(Object.keys(initialValues).length ? changed : true);

        const eventId = `${id}_${changed}`;
        if (id && eventId !== lastEmittedEvent.current) {
            lastEmittedEvent.current = eventId;
            if (changed) {
                EventEmitter.emit(MESSAGES.RULE_VIEW.CHANGE, { id });
            } else {
                EventEmitter.emit(MESSAGES.RULE_VIEW.UNCHANGED, { id });
            }
        }
        setChanged(changed);
        setModifiedValues(newModifiedValues);
        const _allowConfirm: boolean =
            modifiedValues().type === MAPPING_RULE_TYPE_ROOT ||
            !_.isEmpty(modifiedValues().targetProperty) ||
            (!!modifiedValues().sourceProperty && !_.isEmpty(modifiedValues().sourceProperty.trim()));
        setAllowConfirm(_allowConfirm);
        if (name === "targetEntityType") {
            // Need to react to target entity changes
            setTargetEntityType(value);
        }
    };

    /**
     * handle form close event
     * @param event
     */
    const handleClose = (event) => {
        event.stopPropagation();
        const { id = 0 } = props;
        EventEmitter.emit(MESSAGES.RULE_VIEW.UNCHANGED, { id });
        EventEmitter.emit(MESSAGES.RULE_VIEW.CLOSE, { id });
        toggleTabViewDirtyState(false);
    };

    const checkUriPattern = async (uriPattern: string) => {
        const validationResult = await checkUriPatternValidity(uriPattern);
        if (!uriPattern) {
            setUriPatternIsValid(true);
        } else if (validationResult?.valid !== undefined) {
            setUriPatternIsValid(validationResult?.valid as boolean);
        }
        return validationResult;
    };

    if (loading) {
        return <Spinner />;
    }

    const errorMessage = saveObjectError && <ErrorView {...saveObjectError} />;

    const title = !id && <CardTitle>Add object mapping</CardTitle>;

    let targetPropertyInput: JSX.Element | undefined = undefined;
    let targetCardinality: JSX.Element | undefined = undefined;
    let entityRelationInput: JSX.Element | undefined = undefined;
    let sourcePropertyInput: JSX.Element | undefined = undefined;

    targetCardinality = (
        <TargetCardinality
            className="ecc-silk-mapping__ruleseditor__isAttribute"
            isAttribute={modifiedValues().isAttribute}
            isObjectMapping={true}
            onChange={(value) => handleChangeValue("isAttribute", value)}
        />
    );

    const initialValues: any = props.ruleData;

    if (modifiedValues().type !== MAPPING_RULE_TYPE_ROOT) {
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
                value={modifiedValues().targetProperty}
                onChange={(value) => {
                    handleChangeValue("targetProperty", value);
                }}
                resetQueryToValue={true}
                itemDisplayLabel={(item) => (item.label ? `${item.label} (${item.value})` : item.value)}
            />
        );
        entityRelationInput = (
            <FieldItem>
                <RadioGroup
                    onChange={({ value }) => {
                        handleChangeValue("entityConnection", value);
                    }}
                    value={!_.isEmpty(modifiedValues().entityConnection) ? modifiedValues().entityConnection : "from"}
                    name=""
                    disabled={false}
                    data-id="entity_radio_group"
                >
                    <Radio
                        value="from"
                        label={
                            <span>
                                Connect from <ParentElement parent={parent} />
                            </span>
                        }
                    />
                    <Radio
                        value="to"
                        label={
                            <span>
                                Connect to <ParentElement parent={parent} />
                            </span>
                        }
                    />
                </RadioGroup>
            </FieldItem>
        );
        const valuePath =
            initialValues.sourceProperty == null
                ? ""
                : typeof initialValues.sourceProperty === "string"
                ? initialValues.sourceProperty
                : initialValues.sourceProperty.value;
        sourcePropertyInput = (
            <CodeAutocompleteField
                id={"object-value-path-auto-suggestion"}
                label="Value path"
                initialValue={valuePath}
                clearIconText={"Clear value path"}
                validationErrorText={"The entered value path is invalid."}
                onChange={(value) => {
                    handleChangeValue("sourceProperty", value);
                }}
                fetchSuggestions={(input, cursorPosition) =>
                    fetchValuePathSuggestions(parentId, input, cursorPosition, true, mappingEditorContext.taskContext)
                }
                checkInput={checkValuePathValidity}
                onInputChecked={setObjectPathValid}
                onFocusChange={setValuePathInputHasFocus}
            />
        );
    }

    let patternInput: JSX.Element | undefined;

    // URI pattern
    if (!id || modifiedValues().uriRuleType === "uri") {
        if (
            !modifiedValues().pattern &&
            !createCustomUriPatternForNewRule &&
            (!id || !(props.ruleData as any).pattern)
        ) {
            patternInput = (
                <FieldItem labelProps={{ text: "URI pattern" }}>
                    <TextField
                        data-test-id="object-rule-form-default-pattern"
                        disabled
                        value="Default pattern."
                        rightElement={
                            <Button
                                data-test-id="object-rule-form-default-pattern-custom-pattern-btn"
                                onClick={() => setCreateCustomUriPatternForNewRule(true)}
                            >
                                Create custom pattern
                            </Button>
                        }
                    />
                </FieldItem>
            );
        } else {
            patternInput = (
                <CodeAutocompleteField
                    id={"uri-pattern-auto-suggestion"}
                    label="URI pattern"
                    initialValue={initialUriPattern}
                    clearIconText={"Clear URI pattern"}
                    validationErrorText={"The entered URI pattern is invalid."}
                    onChange={(value) => {
                        handleChangeValue("pattern", value);
                    }}
                    fetchSuggestions={(input, cursorPosition) =>
                        fetchUriPatternAutoCompletions(
                            parentId ? parentId : "root",
                            input,
                            cursorPosition,
                            modifiedValues().sourceProperty
                        )
                    }
                    onFocusChange={setUriPatternInputHasFocus}
                    checkInput={checkUriPattern}
                    rightElement={
                        distinctUriPatterns.length > 0 ? (
                            <>
                                <Spacing vertical={true} size={"tiny"} />
                                <Button
                                    data-test-id="object-rule-form-uri-pattern-selection-btn"
                                    elevated={true}
                                    tooltip={`Choose URI pattern from ${distinctUriPatterns.length} existing URI pattern/s.`}
                                    onClick={() => setShowUriPatternModal(true)}
                                >
                                    Choose
                                </Button>
                            </>
                        ) : undefined
                    }
                />
            );
        }
    } else {
        patternInput = (
            <FieldItem labelProps={{ text: "URI formula" }}>
                <TextField disabled value="This URI cannot be edited in the edit form." />
            </FieldItem>
        );
    }

    let previewExamples: null | JSX.Element = null;
    const noUriRule = !modifiedValues().uriRule || modifiedValues().uriRule.type === MAPPING_RULE_TYPE_URI;
    const noUriPattern = !modifiedValues().pattern;

    if (valuePathInputHasFocus || uriPatternInputHasFocus) {
        previewExamples = (
            <Notification data-test-id={"object-rule-form-preview-path-has-focus"}>
                No preview is shown while {valuePathInputHasFocus ? "value path" : "URI pattern"} is being edited.
            </Notification>
        );
    } else if (noUriPattern && noUriRule && !modifiedValues().sourceProperty) {
        previewExamples = (
            <Notification data-test-id={"object-rule-form-preview-no-pattern"}>
                No preview shown for default URI pattern with empty value path.
            </Notification>
        );
    } else if (!uriPatternIsValid || !objectPathValid) {
        previewExamples = (
            <Notification warning={true} data-test-id={"object-rule-form-preview-invalid-input"}>
                URI pattern or value path is invalid. No preview shown.
            </Notification>
        );
    } else if (modifiedValues().pattern || modifiedValues().uriRule || modifiedValues().sourceProperty) {
        const ruleType = modifiedValues().pattern
            ? MAPPING_RULE_TYPE_URI
            : modifiedValues().uriRule
            ? modifiedValues().uriRule.type
            : MAPPING_RULE_TYPE_URI;
        previewExamples = (
            <ExampleView
                id={parentId || "root"}
                rawRule={
                    ruleType === MAPPING_RULE_TYPE_URI
                        ? {
                              type: MAPPING_RULE_TYPE_URI,
                              pattern: modifiedValues().pattern
                                  ? modifiedValues().pattern
                                  : defaultUriPattern(id ?? "yetUnknownRuleId"),
                          }
                        : modifiedValues().uriRule
                }
                ruleType={ruleType}
                objectSourcePathContext={modifiedValues().sourceProperty}
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
                        value={modifiedValues().targetEntityType}
                        creatable
                        onChange={(value) => {
                            handleChangeValue("targetEntityType", value);
                        }}
                    />
                    {targetCardinality}
                    {sourcePropertyInput}
                    {patternInput}
                    {showUriPatternModal && distinctUriPatterns.length > 0 && (
                        <UriPatternSelectionModal
                            onClose={() => setShowUriPatternModal(false)}
                            uriPatterns={distinctUriPatterns}
                            onSelect={(uriPattern) => {
                                setInitialUriPattern(uriPattern.value);
                                handleChangeValue("pattern", uriPattern.value);
                            }}
                        />
                    )}
                    {
                        <FieldItem
                            data-test-id="object-rule-form-example-preview"
                            labelProps={{ text: "Examples of target data" }}
                        >
                            {previewExamples}
                        </FieldItem>
                    }
                    <FieldItem labelProps={{ text: "Label" }}>
                        <TextField
                            data-test-id={"object-rule-form-label-input"}
                            className="ecc-silk-mapping__ruleseditor__label"
                            defaultValue={props.ruleData["label"]}
                            onChange={(event) => {
                                const value = event.target.value;
                                handleChangeValue("label", value);
                            }}
                        />
                    </FieldItem>
                    <FieldItem labelProps={{ text: "Description" }}>
                        <TextArea
                            data-test-id={"object-rule-form-description-input"}
                            className="ecc-silk-mapping__ruleseditor__comment"
                            defaultValue={props.ruleData["comment"]}
                            onChange={(event) => {
                                const value = event.target.value;
                                handleChangeValue("comment", value);
                            }}
                        />
                    </FieldItem>
                </CardContent>
                <CardActions className="ecc-silk-mapping__ruleseditor__actionrow">
                    <AffirmativeButton
                        className="ecc-silk-mapping__ruleseditor__actionrow-save"
                        raised
                        data-test-id={"object-rule-form-confirm-button"}
                        onClick={handleConfirm}
                        disabled={
                            !allowConfirm ||
                            !changed ||
                            (!uriPatternIsValid && modifiedValues().pattern) ||
                            !objectPathValid
                        }
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
};

export default ScrollingHOC(ObjectRuleForm);
