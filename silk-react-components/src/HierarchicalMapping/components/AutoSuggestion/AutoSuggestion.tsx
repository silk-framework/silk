import React, {useCallback, useEffect, useState} from "react";
import CodeMirror from "codemirror";
import { FieldItem, IconButton, Spinner, Label } from "@gui-elements/index";
import { Classes as BlueprintClassNames } from "@blueprintjs/core";

//custom components
import SingleLineCodeEditor from "../SingleLineCodeEditor";
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
export interface ISuggestionWithQuery extends ISuggestionBase {
    query: string
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
                        }: IProps) => {
    const [value, setValue] = React.useState(initialValue);
    const [inputString, setInputString] = React.useState(initialValue);
    const [cursorPosition, setCursorPosition] = React.useState(0);
    const [coords, setCoords] = React.useState({ left: 0 });
    const [shouldShowDropdown, setShouldShowDropdown] = React.useState(false);
    const [replacementIndexesDict, setReplacementIndexesDict] = React.useState(
        {}
    );
    const [suggestions, setSuggestions] = React.useState<ISuggestionWithQuery[]>([]);
    const [suggestionsPending, setSuggestionsPending] = React.useState(false);
    const [pathValidationPending, setPathValidationPending] = React.useState(false)
    const [markers, setMarkers] = React.useState<CodeMirror.TextMarker[]>([]);
    const [, setErrorMarkers] = React.useState<CodeMirror.TextMarker[]>([]);
    const [validationResponse, setValidationResponse] = useState<IValidationResult | undefined>(undefined)
    const [suggestionResponse, setSuggestionResponse] = useState<IPartialAutoCompleteResult | undefined>(undefined)
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

    const valueRef = React.useRef("");
    const pathIsValid = validationResponse?.valid ?? true;

    //handle keypress
    React.useEffect(() => {
        makeDropDownRespondToKeyPress(keyPressedFromEditor);
    }, [keyPressCounter]);

    //handle linting
    React.useEffect(() => {
        const parseError = validationResponse?.parseError;
        if (parseError) {
            clearMarkers();
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
    }, [validationResponse?.valid, validationResponse?.parseError]);

    /** generate suggestions and also populate the replacement indexes dict */
    React.useEffect(() => {
        let newSuggestions: ISuggestionWithQuery[] = [];
        let newReplacementIndexesDict = {};
        if (
            suggestionResponse?.replacementResults?.length === 1 &&
            !suggestionResponse?.replacementResults[0]?.replacements?.length
        ) {
            setShouldShowDropdown(false);
        }
        if (suggestionResponse?.replacementResults?.length) {
            suggestionResponse.replacementResults.forEach(
                ({ replacements, replacementInterval: { from, length }, extractedQuery }) => {
                    const replacementsWithMetaData = replacements.map(r => ({...r, query: extractedQuery}))
                    newSuggestions = [...newSuggestions, ...replacementsWithMetaData];
                    replacements.forEach((replacement) => {
                        newReplacementIndexesDict = {
                            ...newReplacementIndexesDict,
                            [replacement.value]: {
                                from,
                                length,
                            },
                        };
                    });
                }
            );
            setSuggestions(newSuggestions);
            setReplacementIndexesDict(newReplacementIndexesDict)
        }
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
            setInputString(value);
            setShouldShowDropdown(true);
            handleEditorInputChange(inputString, cursorPosition)
            return handleEditorInputChange.cancel
        }
    }, [cursorPosition, value, inputString, isFocused]);

    // Trigger input validation
    useEffect(() => {
        checkValuePathValidity(value)
        return checkValuePathValidity.cancel
    }, [inputString])

    const asyncHandleEditorInputChange = async (inputString: string, cursorPosition: number) => {
        setSuggestionsPending(true)
        try {
            const result: IPartialAutoCompleteResult = await fetchSuggestions(inputString, cursorPosition)
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
        onChange(val)
        setValue(val);
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

    const handleTextHighlighting = (focusedSuggestion: string) => {
        const indexes = replacementIndexesDict[focusedSuggestion];
        if (indexes) {
            clearMarkers();
            const { from, length } = indexes;
            const to = from + length;
            const marker = editorInstance.markText(
                { line: 0, ch: from },
                { line: 0, ch: to },
                { className: "ecc-text-highlighting" }
            );
            setMarkers((previousMarkers) => [...previousMarkers, marker]);
        }
    };

    //remove all the underline highlighting
    const clearMarkers = () => {
        markers.forEach((marker) => marker.clear());
    };

    const handleDropdownChange = (selectedSuggestion: string) => {
        const indexes = replacementIndexesDict[selectedSuggestion];
        if (indexes) {
            const { from, length } = indexes;
            const to = from + length;
            editorInstance.replaceRange(selectedSuggestion, {line: 0, ch: from}, {line: 0, ch: to})
            setShouldShowDropdown(false);
            editorInstance.setCursor({ line: 0, ch: to });
            editorInstance.focus();
            clearMarkers();
        }
    };

    const handleInputEditorClear = () => {
        editorInstance.getDoc().setValue("")
    };

    const handleInputFocus = (focusState: boolean) => {
        onFocusChange && onFocusChange(focusState)
        setIsFocused(focusState)
        setShouldShowDropdown(focusState)
    };

    //keyboard handlers
    const handleArrowDown = () => {
        const lastSuggestionIndex = suggestions.length - 1;
        let nextIndex;
        if (currentIndex === lastSuggestionIndex) {
            nextIndex = 0;
            setCurrentIndex(nextIndex);
            handleTextHighlighting(suggestions[nextIndex]?.value);
        } else {
            setCurrentIndex((index) => {
                nextIndex = ++index;
                handleTextHighlighting(suggestions[nextIndex]?.value);
                return nextIndex;
            });
        }
    };

    const handleArrowUp = () => {
        const lastSuggestionIndex = suggestions.length - 1;
        let nextIndex;
        if (currentIndex === 0) {
            nextIndex = lastSuggestionIndex;
            setCurrentIndex(nextIndex);
            handleTextHighlighting(suggestions[nextIndex]?.value);
        } else {
            setCurrentIndex((index) => {
                nextIndex = --index;
                handleTextHighlighting(suggestions[nextIndex]?.value);
                return nextIndex;
            });
        }
        const chosenSuggestion = suggestions[nextIndex]?.value;
        handleTextHighlighting(chosenSuggestion);
    };

    const handleEnterPressed = () => {
        handleDropdownChange(suggestions[currentIndex]?.value);
        setCurrentIndex(0);
    };

    const handleTabPressed = () => {
        handleDropdownChange(suggestions[currentIndex]?.value);
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
            hasStateDanger={!pathIsValid && !pathValidationPending}
            messageText={!pathIsValid && !pathValidationPending ? validationErrorText : undefined}
        >
            <div id={id} className="ecc-auto-suggestion-box">
                <div className={`ecc-auto-suggestion-box__editor-box ${BlueprintClassNames.INPUT_GROUP} ${BlueprintClassNames.FILL}`}>
                    <SingleLineCodeEditor
                        mode="null"
                        setEditorInstance={setEditorInstance}
                        onChange={handleChange}
                        onCursorChange={handleCursorChange}
                        initialValue={value}
                        onFocusChange={handleInputFocus}
                        handleSpecialKeysPress={handleInputEditorKeyPress}
                    />
                    <span className={BlueprintClassNames.INPUT_ACTION}>
                        <IconButton
                            data-test-id={"value-path-clear-btn"}
                            name="operation-clear"
                            tooltipText={clearIconText}
                            tooltipProperties={{usePortal: false}}
                            onClick={handleInputEditorClear}
                        />
                    </span>
                </div>
                <Dropdown
                    left={coords.left}
                    loading={suggestionsPending}
                    options={suggestions}
                    isOpen={shouldShowDropdown}
                    onItemSelectionChange={handleDropdownChange}
                    currentlyFocusedIndex={currentIndex}
                />
            </div>
        </FieldItem>
    );
};

export default AutoSuggestion;
