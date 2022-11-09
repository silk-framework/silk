import React, {useEffect, useState} from "react";
import {Card, CardActions, CardContent, CardTitle, ScrollingHOC} from "gui-elements-deprecated";
import {
    AffirmativeButton,
    DismissiveButton,
    TextField as LegacyTextField,
} from "@eccenca/gui-elements/src/legacy-replacements";
import {AutoSuggestion, IconButton, Spacing, Spinner, TextField} from "@eccenca/gui-elements";
import _ from "lodash";
import ExampleView from "../ExampleView";
import store, {checkValuePathValidity, fetchValuePathSuggestions} from "../../../store";
import {convertToUri} from "../../../utils/convertToUri";
import ErrorView from "../../../components/ErrorView";
import AutoComplete from "../../../components/AutoComplete";
import {trimValue} from "../../../utils/trimValue";
import {MAPPING_RULE_TYPE_COMPLEX, MAPPING_RULE_TYPE_DIRECT, MESSAGES} from "../../../utils/constants";
import EventEmitter from "../../../utils/EventEmitter";
import {wasTouched} from "../../../utils/wasTouched";
import {newValueIsIRI} from "../../../utils/newValueIsIRI";
import TargetCardinality from "../../../components/TargetCardinality";

const LANGUAGES_LIST = [
    "en",
    "de",
    "es",
    "fr",
    "bs",
    "bg",
    "ca",
    "ce",
    "zh",
    "hr",
    "cs",
    "da",
    "nl",
    "eo",
    "fi",
    "ka",
    "el",
    "hu",
    "ga",
    "is",
    "it",
    "ja",
    "kn",
    "ko",
    "lb",
    "no",
    "pl",
    "pt",
    "ru",
    "sk",
    "sl",
    "sv",
    "tr",
    "uk",
];

// The type of the value of the mapping rule, e.g. string, URI
interface IValueType {
    // The type ID
    nodeType: string;
    // If this is a lang type value, this property specifies the language code
    lang?: string;
}

interface IState {
    // If the form is loading
    loading: boolean;
    // If the rule has been changed
    changed: boolean;
    // The type of the mapping rule
    type: string;
    valueType: IValueType;
    // The source path / property
    sourceProperty: string;
    // Optional comment
    comment?: string;
    // Optional label
    label?: string;
    // The target property / attribute
    targetProperty?: string;
    // If the target property is an attribute (only relevant for XML as target)
    isAttribute: boolean;
}

interface IProps {
    // ID of the rule
    id?: string;
    //
    scrollIntoView: ({ topOffset }) => any;
    parentId?: string;
    onAddNewRule?: (call: () => any) => any;
    openMappingEditor: (ruleId: string) => void;
}

/** The edit form of a value mapping rule. */
export function ValueRuleForm(props: IProps) {
    const [loading, setLoading] = useState<boolean>(false);
    const [changed, setChanged] = useState(false);
    const [type, setType] = useState(MAPPING_RULE_TYPE_DIRECT);
    const [valueType, setValueType] = useState<IValueType>({ nodeType: "StringValueType" });
    const sourceProperty = React.useRef<string | { value: string }>("")
    const [isAttribute, setIsAttribute] = useState(false);
    const [initialValues, setInitialValues] = useState<Partial<IState>>({});
    const [error, setError] = useState<any>(null);
    const [label, setLabel] = useState<string>("");
    const [comment, setComment] = useState<string>("");
    const [targetProperty, setTargetProperty] = useState<string>("");
    const [valuePathValid, setValuePathValid] = useState<boolean>(false);
    const [valuePathInputHasFocus, setValuePathInputHasFocus] = useState<boolean>(false);
    const lastEmittedEvent = React.useRef<string>("")

    const { id, parentId } = props;

    const autoCompleteRuleId = id || parentId;

    const state = {
        loading,
        changed,
        type,
        valueType,
        sourceProperty: sourceProperty.current,
        isAttribute,
        initialValues,
        error,
        label,
        comment,
        targetProperty,
    };

    useEffect(() => {
        loadData();
    }, []);

    useEffect(() => {
        if (!loading) {
            props.scrollIntoView({
                topOffset: 75,
            });
        }
    }, [loading]);

    const loadData = () => {
        setLoading(true);
        if (props.id) {
            store.getRuleAsync(props.id).subscribe(
                ({ rule }) => {
                    const initialValues: Partial<IState> = {
                        type: _.get(rule, "type", MAPPING_RULE_TYPE_DIRECT) as string,
                        comment: _.get(rule, "metadata.description", ""),
                        label: _.get(rule, "metadata.label", ""),
                        targetProperty: _.get(rule, "mappingTarget.uri", ""),
                        valueType: _.get(rule, "mappingTarget.valueType", { nodeType: "StringValueType" }),
                        sourceProperty: rule.sourcePath,
                        isAttribute: _.get(rule, "mappingTarget.isAttribute", false),
                    };

                    initialValues.type && setType(initialValues.type);
                    setValuePathValid(initialValues.type === MAPPING_RULE_TYPE_COMPLEX);
                    initialValues.comment && setComment(initialValues.comment);
                    initialValues.label && setLabel(initialValues.label);
                    initialValues.targetProperty && setTargetProperty(initialValues.targetProperty);
                    initialValues.valueType && setValueType(initialValues.valueType);
                    if(initialValues.sourceProperty) {
                        sourceProperty.current = initialValues.sourceProperty
                    }
                    initialValues.isAttribute && setIsAttribute(initialValues.isAttribute);
                    setInitialValues(initialValues);
                    setLoading(false);
                },
                (err) => {
                    setLoading(false);
                }
            );
        } else {
            setLoading(false);
            EventEmitter.emit(MESSAGES.RULE_VIEW.CHANGE, { id: 0 });
        }
    };

    const handleConfirm = (event) => {
        event.stopPropagation();
        event.persist();
        saveRule(true);
    };

    const saveRule = (reload: boolean, onSuccess?: (ruleId?: string) => any) => {
        setLoading(true);
        store
            .createMappingAsync({
                id: props.id,
                parentId: props.parentId,
                type: type,
                comment: comment,
                label: label,
                targetProperty: trimValue(targetProperty),
                valueType: valueType,
                sourceProperty: trimValue(sourceProperty.current),
                isAttribute: isAttribute,
            })
            .subscribe(
                (response) => {
                    if (props.onAddNewRule) {
                        props.onAddNewRule(() => {
                            handleCloseWithChanges(reload);
                        });
                    } else {
                        handleCloseWithChanges(reload);
                    }
                    onSuccess?.(response?.body?.id);
                },
                (err) => {
                    setError(err);
                    setLoading(false);
                }
            );
    };

    // remove rule
    const handleChangeTextfield = (statePropertyName: string, setValueFunction: (v: any) => void, { value }) => {
        handleChangeValue(statePropertyName, value, setValueFunction);
    };

    const handleChangeSelectBox = (statePropertyName: string, setValueFunction: (v: any) => void, value) => {
        handleChangeValue(statePropertyName, value, setValueFunction);
    };

    const handleChangePropertyType = (value) => {
        const valueType = { nodeType: value };
        handleChangeValue("valueType", valueType, setValueType);
    };

    const handleChangeLanguageTag = (value) => {
        let lang = value;
        if (typeof lang === "object") {
            lang = value.value;
        }
        const valueType = { nodeType: "LanguageValueType", lang };
        handleChangeValue("valueType", valueType, setValueType);
    };

    const handleChangeValue = (stateProperty: string, value, setValueFunction: (v: any) => void) => {
        const { initialValues, ...currValues } = state;
        currValues[stateProperty] = value;

        const touched = wasTouched(initialValues, currValues);
        const id = _.get(props, "id", 0);

        const eventId = `${id}_${touched}`
        if (id !== 0 && eventId !== lastEmittedEvent.current) {
            lastEmittedEvent.current = eventId
            if (touched) {
                EventEmitter.emit(MESSAGES.RULE_VIEW.CHANGE, { id });
            } else {
                EventEmitter.emit(MESSAGES.RULE_VIEW.UNCHANGED, { id });
            }
        }

        setValueFunction(value);
        setChanged(touched);
    };

    const handleClose = () => {
        const id = _.get(props, "id", 0);
        EventEmitter.emit(MESSAGES.RULE_VIEW.UNCHANGED, { id });
        EventEmitter.emit(MESSAGES.RULE_VIEW.CLOSE, { id });
    };

    // Closes the edit form. Reacts to changes in the mapping rules.
    const handleCloseWithChanges = (reload: boolean) => {
        handleClose();
        if (reload) {
            // There are some use cases where the mapping editor should not be reloaded, e.g. when opening the rule editor right after creation of a rule.
            EventEmitter.emit(MESSAGES.RELOAD, true);
        }
    };

    const allowConfirmation = () => {
        const targetPropertyNotEmpty = !_.isEmpty(targetProperty);
        const languageTagSet = valueType.nodeType !== "LanguageValueType" || typeof valueType.lang === "string";
        return targetPropertyNotEmpty && languageTagSet;
    };

    const handleComplexEdit = (event) => {
        event.preventDefault();
        event.stopPropagation();
        saveRule(false, (ruleId) => {
            props.openMappingEditor(ruleId!);
        });
    };

    // Reset a complex mapping rule back to a direct mapping rule with empty path
    const handleComplexRemove = (event) => {
        event.preventDefault();
        event.stopPropagation();
        setType(MAPPING_RULE_TYPE_DIRECT)
        handleChangeValue("sourceProperty", "", () => {})
    };

    const allowConfirm = allowConfirmation();

    const ComplexRuleEditButton = () =>
        allowConfirm ? (
            <IconButton
                name="item-edit"
                data-test-id="complex-rule-edit-button"
                onClick={handleComplexEdit}
                text={changed || !id ? "Save rule and open formula editor" : "Open formula editor"}
            />
        ) : null;

    const ComplexRuleDeleteButton = () => <IconButton
        name="item-remove"
        disruptive={true}
        data-test-id="complex-rule-delete-button"
        onClick={handleComplexRemove}
        text={"Reset complex mapping rule to empty path."}
    />

    const updateSourceProperty = React.useMemo(() => (value: string | {value: string}) => {
        sourceProperty.current = value
    }, [])

    // template rendering
    const render = () => {
        if (loading) {
            return <Spinner />;
        }
        const errorMessage = error ? <ErrorView {...error.response.body} /> : false;
        const title = !id ? <CardTitle>Add value mapping</CardTitle> : false;

        let sourcePropertyInput: React.ReactElement | undefined = undefined;

        if (type === MAPPING_RULE_TYPE_DIRECT) {
            sourcePropertyInput = (
                <>
                    <AutoSuggestion
                        id={"value-path-auto-suggestion"}
                        label="Value path"
                        initialValue={initialValues.sourceProperty ?? ""}
                        clearIconText={"Clear value path"}
                        validationErrorText={"The entered value path is invalid."}
                        onChange={handleChangeSelectBox.bind(null, "sourceProperty", updateSourceProperty)}
                        fetchSuggestions={(input, cursorPosition) =>
                            fetchValuePathSuggestions(autoCompleteRuleId, input, cursorPosition, false)
                        }
                        checkInput={checkValuePathValidity}
                        onInputChecked={setValuePathValid}
                        onFocusChange={setValuePathInputHasFocus}
                        rightElement={<ComplexRuleEditButton />}
                    />
                </>
            );
        } else if (type === MAPPING_RULE_TYPE_COMPLEX) {
            const editButton = <ComplexRuleEditButton />
            const actions = <span>
                    {editButton}
                <ComplexRuleDeleteButton/>
                </span>
            sourcePropertyInput = (
                <TextField
                    data-id="test-complex-input"
                    disabled
                    value="The value formula cannot be modified in the edit form."
                    rightElement={actions}
                />
            );
        }
        const exampleView =
            (!_.isEmpty(sourceProperty.current) && valuePathValid && !valuePathInputHasFocus) ||
            (type === MAPPING_RULE_TYPE_COMPLEX && id) ? (
                <ExampleView
                    id={type === MAPPING_RULE_TYPE_COMPLEX ? id!! : props.parentId || "root"}
                    key={typeof sourceProperty.current === "string" ? sourceProperty.current : sourceProperty.current.value}
                    rawRule={type === MAPPING_RULE_TYPE_COMPLEX ? undefined : state}
                    ruleType={type}
                />
            ) : null;
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
                            onChange={handleChangeSelectBox.bind(null, "targetProperty", setTargetProperty)}
                            resetQueryToValue={true}
                            itemDisplayLabel={(item) => (item.label ? `${item.label} (${item.value})` : item.value)}
                        />
                        <AutoComplete
                            placeholder="Data type"
                            className="ecc-silk-mapping__ruleseditor__propertyType"
                            entity="propertyType"
                            ruleId={autoCompleteRuleId}
                            value={valueType.nodeType}
                            clearable={false}
                            onChange={handleChangePropertyType}
                        />
                        {valueType.nodeType === "LanguageValueType" && (
                            <AutoComplete
                                data-id="lng-select-box"
                                placeholder="Language Tag"
                                className="ecc-silk-mapping__ruleseditor__languageTag"
                                entity="langTag"
                                ruleId={autoCompleteRuleId}
                                options={LANGUAGES_LIST}
                                value={valueType.lang}
                                onChange={handleChangeLanguageTag}
                                isValidNewOption={(option) => !_.isNull(option.label.match(/^[a-z]{2}(-[A-Z]{2})?$/))}
                                creatable={true}
                                noResultsText="Not a valid language tag"
                                newOptionText={(newLabel) => `Create language tag: ${newLabel}`}
                                clearable={false} // hide 'remove all selected values' button
                            />
                        )}
                        <TargetCardinality
                            className="ecc-silk-mapping__ruleseditor__isAttribute"
                            isAttribute={isAttribute}
                            isObjectMapping={false}
                            onChange={() => handleChangeValue("isAttribute", !isAttribute, setIsAttribute)}
                        />
                        {sourcePropertyInput}
                        <Spacing size={"small"} />
                        {exampleView}
                        <Spacing size={"small"} />
                        <LegacyTextField
                            label="Label"
                            className="ecc-silk-mapping__ruleseditor__label"
                            value={label}
                            onChange={handleChangeTextfield.bind(null, "label", setLabel)}
                        />
                        <LegacyTextField
                            multiline
                            label="Description"
                            className="ecc-silk-mapping__ruleseditor__comment"
                            value={comment}
                            onChange={handleChangeTextfield.bind(null, "comment", setComment)}
                        />
                    </CardContent>
                    <CardActions className="ecc-silk-mapping__ruleseditor__actionrow">
                        <AffirmativeButton
                            className="ecc-silk-mapping__ruleseditor__actionrow-save"
                            raised
                            onClick={handleConfirm}
                            disabled={!allowConfirm || (!changed && !!id)}
                        >
                            Save
                        </AffirmativeButton>
                        <DismissiveButton
                            data-test-id={"value-rule-form-edit-cancel-btn"}
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
    };
    return render();
}
export default ScrollingHOC(ValueRuleForm);
