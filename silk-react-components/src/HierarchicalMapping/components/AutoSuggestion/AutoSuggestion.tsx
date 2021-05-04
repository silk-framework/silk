import React, {useCallback, useEffect, useState} from "react";
import CodeMirror from "codemirror";
import { FieldItem, IconButton, Spinner, Label } from "@gui-elements/index";
import { Classes as BlueprintClassNames } from "@blueprintjs/core";

//custom components
import SingleLineCodeEditor, {IRange} from "../SingleLineCodeEditor";
import {Dropdown} from "./Dropdown";
import { debounce } from "lodash";

export enum OVERWRITTEN_KEYS {
    ArrowUp = "ArrowUp",
    ArrowDown = "ArrowDown",
    Enter = "Enter",
    Tab = "Tab",
}

/** A single suggestion. */
export interface ISuggestionBase {
    // The actual value
    value: string
    // Optional human-readable label
    label?: string
    // Optional description of the value.
    description?: string
}

/** Same as ISuggestionBase, but with the query that was used to fetch this suggestion. */
export interface ISuggestionWithReplacementInfo extends ISuggestionBase {
    // The query this result was filtered by
    query: string
    // The offset of the original string that should be replaced
    from: number
    // The length of the original string that should be replaced
    length: number
}

/** The suggestions for a specific substring of the given input string. */
export interface IReplacementResult {
    // The range of the input string that should be replaced
    replacementInterval: {
        from: number
        length: number
    }
    // The extracted query from the local value at the cursor position of the path that was used to retrieve the suggestions.
    extractedQuery: string
    // The suggested replacements for the substring that should be replaced.
    replacements: ISuggestionBase[]
}

export interface IPartialAutoCompleteResult {
    // Repeats the input string from the corresponding request
    inputString: string
    // Repeats the cursor position from the corresponding request
    cursorPosition: number
    replacementResults: IReplacementResult[]
}

/** Validation result */
export interface IValidationResult {
    // If the input value is valid or not
    valid: boolean,
    parseError?: {
        // Where the error is located
        offset: number
        // Detail error message
        message: string
        // The input before the cursor that is considered invalid
        inputLeadingToError: string
    }
}

interface IProps {
    // Optional label to be shown for the input (above)
    label: string
    // The value the component is initialized with, do not use this to control value changes.
    initialValue: string
    // Callback on value change
    onChange: (currentValue: string) => any
    // Fetches the suggestions
    fetchSuggestions: (inputString: string, cursorPosition: number) => IPartialAutoCompleteResult | Promise<IPartialAutoCompleteResult | undefined>
    // Checks if the input is valid
    checkInput: (inputString: string) => IValidationResult | Promise<IValidationResult | undefined>
    // Called with the input validation result
    onInputChecked?: (validInput: boolean) => any
    // Text that should be shown if the input validation failed.
    validationErrorText: string
    // Text that should be shown when hovering over the clear icon
    clearIconText: string
    // Called when focus status changes
    onFocusChange?: (hasFocus: boolean) => any
    // Optional ID to attach to the outer element
    id?: string
}

/** Input component that allows partial, fine-grained auto-completion, i.e. of sub-strings of the input string.
 * This is comparable to a one line code editor. */
const AutoSuggestion = ({
                            label,
                            initialValue,
                            onChange,
                            fetchSuggestions,
                            checkInput,
                            validationErrorText,
                            clearIconText,
                            onFocusChange,
                            id,
                            onInputChecked,
                        }: IProps) => {
    const [value, setValue] = React.useState(initialValue);
    const [cursorPosition, setCursorPosition] = React.useState(0);
    const [coords, setCoords] = React.useState({ left: 0 });
    const [shouldShowDropdown, setShouldShowDropdown] = React.useState(false);
    const [suggestions, setSuggestions] = React.useState<ISuggestionWithReplacementInfo[]>([]);
    const [suggestionsPending, setSuggestionsPending] = React.useState(false);
    const [pathValidationPending, setPathValidationPending] = React.useState(false)
    const [, setErrorMarkers] = React.useState<CodeMirror.TextMarker[]>([]);
    const [validationResponse, setValidationResponse] = useState<IValidationResult | undefined>(undefined)
    const [suggestionResponse, setSuggestionResponse] = useState<IPartialAutoCompleteResult | undefined>(undefined)
    // The element that should be used for replacement highlighting
    const [highlightedElement, setHighlightedElement] = useState<ISuggestionWithReplacementInfo | undefined>(undefined)
    const [
        editorInstance,
        setEditorInstance,
    ] = React.useState<CodeMirror.Editor>();
    const [isFocused, setIsFocused] = React.useState(false);
    const [currentIndex, setCurrentIndex] = React.useState<number>(0);
    const [keyPressCounter, setKeyPressCounter] = React.useState(0);
    const [
        keyPressedFromEditor,
        setKeyPressedFromEditor,
    ] = React.useState<OVERWRITTEN_KEYS>();
    const [selectedTextRanges, setSelectedTextRanges] = useState<IRange[]>([])

    const pathIsValid = validationResponse?.valid ?? true;

    //handle keypress
    React.useEffect(() => {
        makeDropDownRespondToKeyPress(keyPressedFromEditor);
    }, [keyPressCounter]);

    // Handle replacement highlighting
    useEffect(() => {
        if (highlightedElement && editorInstance) {
            const { from, length } = highlightedElement;
            if(length > 0 && selectedTextRanges.length == 0) {
                const to = from + length;
                const marker = editorInstance.markText(
                    {line: 0, ch: from},
                    {line: 0, ch: to},
                    {className: "ecc-text-highlighting"}
                );
                return () => marker.clear()
            }
        }
    }, [highlightedElement, selectedTextRanges])

    //handle linting
    React.useEffect(() => {
        const parseError = validationResponse?.parseError;
        if (parseError && editorInstance) {
            const { offset, inputLeadingToError, message } = parseError;
            const start = inputLeadingToError.length > 1 ? offset - inputLeadingToError.length + 1 : offset
            const end = offset + 2;
            editorInstance.getDoc().getEditor()
            const marker = editorInstance.markText(
                { line: 0, ch: start },
                { line: 0, ch: end },
                { className: "ecc-text-error-highlighting" }
            );
            setErrorMarkers((previousMarkers) => {
                previousMarkers.forEach(marker => marker.clear())
                return [marker]
            });
        } else {
            // Valid, clear all error markers
            setErrorMarkers((previous) => {
                previous.forEach(marker => marker.clear())
                return []
            })
        }
        onInputChecked && onInputChecked(!!validationResponse?.valid)
    }, [validationResponse?.valid, validationResponse?.parseError]);

    /** generate suggestions and also populate the replacement indexes dict */
    React.useEffect(() => {
        let newSuggestions: ISuggestionWithReplacementInfo[] = [];
        if (
            suggestionResponse?.replacementResults?.length === 1 &&
            !suggestionResponse?.replacementResults[0]?.replacements?.length
        ) {
            setShouldShowDropdown(false);
        }
        if (suggestionResponse?.replacementResults?.length) {
            suggestionResponse.replacementResults.forEach(
                ({ replacements, replacementInterval: { from, length }, extractedQuery }) => {
                    const replacementsWithMetaData = replacements.map(r => ({...r, query: extractedQuery, from, length}))
                    newSuggestions = [...newSuggestions, ...replacementsWithMetaData];
                }
            );
            setSuggestions(newSuggestions);
        } else {
            setSuggestions([])
        }
        setCurrentIndex(0)
    }, [suggestionResponse]);

    const asyncCheckInput = async (inputString: string) => {
        setPathValidationPending(true)
        try {
            const result: IValidationResult | undefined = await checkInput(inputString)
            setValidationResponse(result)
        } catch(e) {
            setValidationResponse(undefined)
            // TODO: Error handling
        } finally {
            setPathValidationPending(false)
        }
    }

    const checkValuePathValidity = useCallback(
        debounce((inputString: string) => asyncCheckInput(inputString), 200),
        [checkInput]
    )

    React.useEffect(() => {
        if (isFocused) {
            setShouldShowDropdown(true);
            handleEditorInputChange(value, cursorPosition)
            return handleEditorInputChange.cancel
        }
    }, [cursorPosition, value, isFocused]);

    // Trigger input validation
    useEffect(() => {
        checkValuePathValidity(value)
        return checkValuePathValidity.cancel
    }, [value])

    const asyncHandleEditorInputChange = async (inputString: string, cursorPosition: number) => {
        setSuggestionsPending(true)
        try {
            const result: IPartialAutoCompleteResult | undefined = await fetchSuggestions(inputString, cursorPosition)
            setSuggestionResponse(result)
        } catch(e) {
            setSuggestionResponse(undefined)
            // TODO: Error handling
        } finally {
            setSuggestionsPending(false)
        }
    }

    const handleEditorInputChange = useCallback(
        debounce((inputString: string, cursorPosition: number) => asyncHandleEditorInputChange(inputString, cursorPosition), 200),
        [checkInput]
    )

    const handleChange = (val: string) => {
        setValue(val);
        onChange(val)
    };

    const handleCursorChange = (pos, coords) => {
        setCursorPosition(pos.ch);
        setCoords(() => coords);
    };

    const handleInputEditorKeyPress = (event: KeyboardEvent) => {
        const overWrittenKeys: Array<string> = Object.values(OVERWRITTEN_KEYS);
        if (overWrittenKeys.includes(event.key)) {
            event.preventDefault();
            setKeyPressedFromEditor(OVERWRITTEN_KEYS[event.key]);
            setKeyPressCounter((counter) => ++counter);
        }
    };

    const closeDropDown = () => {
        setHighlightedElement(undefined)
        setShouldShowDropdown(false)
    }

    const handleDropdownChange = (selectedSuggestion: ISuggestionWithReplacementInfo) => {
        if (selectedSuggestion && editorInstance) {
            const { from, length } = selectedSuggestion;
            const to = from + length;
            editorInstance.replaceRange(selectedSuggestion.value, {line: 0, ch: from}, {line: 0, ch: to})
            closeDropDown()
            editorInstance.setCursor({ line: 0, ch: to });
            editorInstance.focus();
        }
    };

    const handleInputEditorClear = () => {
        handleChange("");
        setValue("")
        editorInstance?.focus();
    };

    const handleInputFocus = (focusState: boolean) => {
        onFocusChange && onFocusChange(focusState)
        setIsFocused(focusState)
        focusState ? setShouldShowDropdown(true) : closeDropDown()
    };

    //keyboard handlers
    const handleArrowDown = () => {
        const lastSuggestionIndex = suggestions.length - 1;
        if (currentIndex === lastSuggestionIndex) {
            // wrap around
            setCurrentIndex(0);
        } else {
            setCurrentIndex((index) => index + 1)
        }
    };

    const handleArrowUp = () => {
        const lastSuggestionIndex = suggestions.length - 1;
        if (currentIndex === 0) {
            // wrap around
            setCurrentIndex(lastSuggestionIndex);
        } else {
            setCurrentIndex((index) => index - 1);
        }
    };

    const handleEnterPressed = () => {
        handleDropdownChange(suggestions[currentIndex]);
        setCurrentIndex(0);
    };

    const handleTabPressed = () => {
        handleDropdownChange(suggestions[currentIndex]);
    };

    const makeDropDownRespondToKeyPress = (keyPressedFromInput) => {
        if (shouldShowDropdown) {
            switch (keyPressedFromInput) {
                case OVERWRITTEN_KEYS.ArrowUp:
                    handleArrowUp();
                    break;
                case OVERWRITTEN_KEYS.ArrowDown:
                    handleArrowDown();
                    break;
                case OVERWRITTEN_KEYS.Enter:
                    handleEnterPressed();
                    break;
                case OVERWRITTEN_KEYS.Tab:
                    handleTabPressed();
                    break;
                default:
                    //do nothing
                    null;
            }
        }
    };

    const handleItemHighlighting = (item: ISuggestionWithReplacementInfo | undefined) => {
        setHighlightedElement(item)
    }

    const hasError = !!value && !pathIsValid && !pathValidationPending;

    return (
        <FieldItem
            labelAttributes={{
                text: (
                    <>
                        {label}
                        {pathValidationPending && (
                            <Spinner size="tiny" position="inline" description="Validating value path" />
                        )}
                    </>)
            }}
            hasStateDanger={hasError}
            messageText={hasError ? validationErrorText : undefined}
        >
            <div id={id} className="ecc-auto-suggestion-box">
                <div className={`ecc-auto-suggestion-box__editor-box ${BlueprintClassNames.INPUT_GROUP} ${BlueprintClassNames.FILL} ${hasError ? BlueprintClassNames.INTENT_DANGER : ""}`}>
                    <SingleLineCodeEditor
                        mode="null"
                        setEditorInstance={setEditorInstance}
                        onChange={handleChange}
                        onCursorChange={handleCursorChange}
                        initialValue={value}
                        onFocusChange={handleInputFocus}
                        onKeyDown={handleInputEditorKeyPress}
                        onSelection={setSelectedTextRanges}/>
                    {!!value && (
                        <span className={BlueprintClassNames.INPUT_ACTION}>
                            <IconButton
                                data-test-id={"value-path-clear-btn"}
                                name="operation-clear"
                                text={clearIconText}
                                onClick={handleInputEditorClear}
                            />
                        </span>
                    )}
                </div>
                <Dropdown
                    left={coords.left}
                    loading={suggestionsPending}
                    options={suggestions}
                    isOpen={shouldShowDropdown}
                    onItemSelectionChange={handleDropdownChange}
                    currentlyFocusedIndex={currentIndex}
                    itemToHighlight={handleItemHighlighting}
                />
            </div>
        </FieldItem>
    );
};

export default AutoSuggestion;
