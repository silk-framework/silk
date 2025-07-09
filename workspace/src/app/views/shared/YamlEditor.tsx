import React from "react";
import { debounce } from "lodash";

import { AutoSuggestionList, ContextOverlay, ExtendedCodeEditor, IconButton } from "@eccenca/gui-elements";
import { EditorView, Rect } from "@codemirror/view";
import { Classes as BlueprintClassNames } from "@blueprintjs/core";
import {
    CodeAutocompleteFieldSuggestionWithReplacementInfo,
    CodeAutocompleteFieldValidationResult,
    OVERWRITTEN_KEYS,
    OverwrittenKeyTypes,
} from "@eccenca/gui-elements/src/components/AutoSuggestion/AutoSuggestion";
import { CLASSPREFIX as eccgui } from "@eccenca/gui-elements/src/configuration/constants";
import { IRange } from "@eccenca/gui-elements/src/components/AutoSuggestion/ExtendedCodeEditor";
import { sharedOp } from "@ducks/shared";
import { IPropertyAutocomplete } from "@ducks/common/typings";
import {
    dependentValueIsSet,
    DependsOnParameterValueAny,
} from "./modals/CreateArtefactModal/ArtefactForms/ParameterAutoCompletion";
import { DependsOnParameterValue } from "@ducks/shared/typings";

interface YamlEditorProps {
    /** ID of the parameter. */
    projectId?: string;
    /** ID of this plugin. */
    pluginId?: string;
    /** Unique ID/name of the parameter in the form. */
    formParamId?: string;
    /** Get parameter values this auto-completion might depend on. */
    dependentValue?: (paramId: string) => DependsOnParameterValueAny | undefined;
    /** The default value as defined in the parameter spec. */
    defaultValue?: (paramId: string) => string | null | undefined;
    id: string;
    className?: string;
    label?: string;
    initialValue?: string;
    /** Callback on value change
     */
    onChange: (currentValue: string) => any;
    checkInput?: (
        inputString: string,
    ) => CodeAutocompleteFieldValidationResult | Promise<CodeAutocompleteFieldValidationResult | undefined>;
    /** Called with the input validation result
     */
    onInputChecked?: (validInput: boolean) => any;

    /** Delay in ms before an auto-completion request should be send after nothing is typed in anymore.
     * This should prevent the UI to send too many requests to the backend. */
    autoCompletionRequestDelay?: number;
    /** Called when focus status changes
     */
    onFocusChange?: (hasFocus: boolean) => any;
    /** The auto-completion config. */
    autoCompletion?: IPropertyAutocomplete;
}

type SyntaxValidationError = {
    line: number;
    message: string;
    valid: boolean;
};

interface RequestMetaData {
    requestId: string | undefined;
}

//constants
const EXTRA_VERTICAL_PADDING = 10;

export const YamlEditor: React.FC<YamlEditorProps> = (props) => {
    const [cm, setCM] = React.useState<EditorView>();
    const value = React.useRef<string>(props.initialValue ?? "");
    const cursorPosition = React.useRef(0);
    const autoSuggestionDivRef = React.useRef<HTMLDivElement>(null);
    const isFocused = React.useRef(false);
    const [shouldShowDropdown, setShouldShowDropdown] = React.useState(false);
    const [suggestions, setSuggestions] = React.useState<CodeAutocompleteFieldSuggestionWithReplacementInfo[]>([]);
    const [suggestionsPending, setSuggestionsPending] = React.useState(false);
    const [hasError, setHasError] = React.useState<boolean>(false);
    const dropdownXYoffset = React.useRef<{ x: number; y: number }>({ x: 0, y: 0 });
    /** This is for the AutoSuggestionList component in order to re-render. todo change to key={x} in component*/
    const [focusedIndex, setFocusedIndex] = React.useState(0);
    const [syntaxValidationError, setSyntaxValidationError] = React.useState<SyntaxValidationError>();
    const [editorState] = React.useState<{
        index: number;
        suggestions: CodeAutocompleteFieldSuggestionWithReplacementInfo[];
        cm?: EditorView;
        dropdownShown: boolean;
    }>({ index: 0, suggestions: [], dropdownShown: false });
    // The element that should be used for replacement highlighting
    const [highlightedElement, setHighlightedElement] = React.useState<
        CodeAutocompleteFieldSuggestionWithReplacementInfo | undefined
    >(undefined);
    const selectedTextRanges = React.useRef<IRange[]>([]);

    React.useEffect(() => {
        editorState.cm = cm;
    }, [cm, editorState]);

    const currentIndex = () => editorState.index;

    const setCurrentIndex = (newIndex: number) => {
        editorState.index = newIndex;
        setFocusedIndex(newIndex);
    };

    React.useEffect(() => {
        editorState.dropdownShown = !!suggestions.length;
    }, [shouldShowDropdown, editorState, suggestions]);

    const getOffsetRange = (cm: EditorView, from: number, to: number) => {
        if (!cm) return { fromOffset: 0, toOffset: 0 };
        const cursor = cm.state.selection.main.head;
        const cursorLine = cm.state.doc.lineAt(cursor).number;
        const offsetFromFirstLine = cm.state.doc.line(cursorLine).from; //all characters including line breaks
        const fromOffset = offsetFromFirstLine + from;
        const toOffset = offsetFromFirstLine + to;

        return { fromOffset, toOffset };
    };

    const dispatch = (
        typeof editorState?.cm?.dispatch === "function" ? editorState?.cm?.dispatch : () => {}
    ) as EditorView["dispatch"];

    const inputactionsDisplayed = React.useCallback((node) => {
        if (!node) return;
        const width = node.offsetWidth;
        const slCodeEditor = node.parentElement.getElementsByClassName(`${eccgui}-singlelinecodeeditor`);
        if (slCodeEditor.length > 0) {
            slCodeEditor[0].style.paddingRight = `${width}px`;
        }
    }, []);

    const syntaxValidation = React.useCallback(
        (yaml: string) => {
            //disallow nesting of yaml objects, arrays, etc.
            /**
             * return {
             *   valid: false,
             *   message: "Nesting of yaml objects, arrays, etc. is not allowed.",
             *   line: 1
             * }
             */
        },
        [cm],
    );

    const handleChange = React.useMemo(() => {
        return (val: string) => {
            value.current = val;
            syntaxValidation(val);
            props.onChange(val);
        };
    }, [props.onChange, syntaxValidation]);

    const handleInputEditorClear = React.useCallback(() => {
        dispatch({
            changes: { from: 0, to: cm?.state.doc.length, insert: "" },
        });
        cursorPosition.current = 0;
        handleChange("");
        cm?.focus();
    }, []);

    const selectDependentValues = (autoCompletion: IPropertyAutocomplete): DependsOnParameterValue[] => {
        if (!props.formParamId || !props.defaultValue || !props.dependentValue) return [];
        const prefixIdx = props.formParamId.lastIndexOf(".");
        const parameterPrefix = prefixIdx >= 0 ? props.formParamId.substring(0, prefixIdx + 1) : "";
        return autoCompletion.autoCompletionDependsOnParameters.flatMap((paramId) => {
            const value = props.dependentValue!(paramId);
            if (dependentValueIsSet(value?.value, props.defaultValue!(parameterPrefix + paramId) != null)) {
                return [{ value: `${value!.value}`, isTemplate: value!.isTemplate }];
            } else {
                return [];
            }
        });
    };

    const asyncHandleEditorInputChange = React.useMemo(
        () => async (inputString: string, cursorPosition: number) => {
            if (!editorState?.cm) return;
            setSuggestionsPending(true);
            try {
                const cursor = editorState?.cm.state.selection.main.head; ///actual cursor position
                const cursorLineObject = editorState?.cm.state.doc.lineAt(cursor ?? 0);
                const cursorLine = cursorLineObject.number; //line starts from 1
                const text = cursorLineObject.text;
                const indexOfColumn = text.indexOf(":");
                const queryStr = inputString.split("\n")[cursorLine - 1].trimStart();

                //previous line does not exist, has a colon, is an empty space
                const previousLine = cursorLine !== 1 ? editorState?.cm.state.doc.line(cursorLine - 1) : null;
                const previousLineHasColonOrIsEmpty =
                    !previousLine || previousLine.text.indexOf(":") > -1 || !previousLine.text.length;
                const currentLineHasTextJustBeforeColon =
                    cursorLine && ((indexOfColumn > -1 && cursor < indexOfColumn) || indexOfColumn < 0);

                if (
                    previousLineHasColonOrIsEmpty &&
                    currentLineHasTextJustBeforeColon &&
                    props.projectId &&
                    props.pluginId &&
                    props.autoCompletion
                ) {
                    const result = await sharedOp.getAutocompleteResultsAsync({
                        pluginId: props.pluginId,
                        parameterId: props.id,
                        projectId: props.projectId,
                        dependsOnParameterValues: selectDependentValues(props.autoCompletion),
                        textQuery: queryStr,
                        limit: 5,
                    });

                    if (value.current === inputString) {
                        const newSuggestions = result.data.map((r) => ({
                            ...r,
                            query: queryStr,
                            from: cursorPosition,
                            length: 0,
                        }));
                        editorState.suggestions = newSuggestions;
                        setSuggestions(newSuggestions);
                        setShouldShowDropdown(true);
                    }
                } else {
                    editorState.suggestions = [];
                    setSuggestions([]);
                    setShouldShowDropdown(false);
                }
            } catch (e) {
                // TODO: Error handling
            } finally {
                setSuggestionsPending(false);
            }
        },
        [cm, props.projectId, props.pluginId, props.autoCompletion, props.defaultValue, props.dependentValue],
    );

    const handleEditorInputChange = React.useMemo(
        () =>
            debounce(
                (inputString: string, cursorPosition: number) =>
                    asyncHandleEditorInputChange(inputString, cursorPosition),
                props.autoCompletionRequestDelay,
            ),
        [asyncHandleEditorInputChange, props.autoCompletionRequestDelay],
    );

    const handleCursorChange = (cursor: number, coords: Rect, scrollinfo: HTMLElement, view: EditorView) => {
        //cursor here is offset from line 1, autosuggestion works with cursor per-line.
        // derived cursor is current cursor position - start of line of cursor
        const cursorLine = view.state.doc.lineAt(cursor).number;
        const offsetFromFirstLine = view.state.doc.line(cursorLine).from; //;
        cursorPosition.current = cursor - offsetFromFirstLine;
        // cursor change is fired after onChange, so we put the auto-complete logic here
        //get value at line
        if (isFocused.current) {
            // setShouldShowDropdown(true);
            handleEditorInputChange.cancel();
            handleEditorInputChange(value.current, cursorPosition.current);
        }

        setTimeout(() => {
            dropdownXYoffset.current = {
                x: Math.min(coords.left, Math.max(coords.left - scrollinfo?.scrollLeft, 0)),
                y: Math.min(coords.bottom, Math.max(coords.bottom - scrollinfo?.scrollTop, 0)) + EXTRA_VERTICAL_PADDING,
            };
        }, 1);
    };

    const handleItemHighlighting = React.useCallback(
        (item: CodeAutocompleteFieldSuggestionWithReplacementInfo | undefined) => {
            setHighlightedElement(item);
        },
        [],
    );

    const handleDropdownChange = React.useCallback(
        (selectedSuggestion: CodeAutocompleteFieldSuggestionWithReplacementInfo) => {
            if (selectedSuggestion && editorState.cm) {
                const { from: start, value, query } = selectedSuggestion;
                const cursor = editorState.cm?.state.selection.main.head;
                const from = Math.max(0, start - query.length);
                const to = from + value.length - 1;

                //getOffsetRange helps get the offset from the beginning of the line
                const { fromOffset } = getOffsetRange(editorState.cm, from, to);
                editorState.cm.dispatch({
                    changes: {
                        from: fromOffset,
                        insert: value,
                        to: fromOffset + query.length,
                    },
                });
                closeDropDown();
                const cursorLine = editorState.cm.state.doc.lineAt(cursor).number;
                const newCursorPos = editorState.cm.state.doc.line(cursorLine).from + (from + value.length);

                editorState.cm.dispatch({ selection: { anchor: newCursorPos } });
                editorState.cm.focus();
            }
        },
        [],
    );

    const closeDropDown = () => {
        setHighlightedElement(undefined);
        setShouldShowDropdown(false);
    };

    const handleInputFocus = (focusState: boolean) => {
        props.onFocusChange?.(focusState);
        if (focusState) {
            setShouldShowDropdown(true);
        } else {
            closeDropDown();
        }

        if (!isFocused.current && focusState) {
            // Just got focus
            // Clear suggestions and repeat suggestion request, something else might have changed while this component was not focused
            setSuggestions([]);
            isFocused.current = focusState;
            handleEditorInputChange.cancel();
            handleEditorInputChange(value.current, cursorPosition.current);
        } else {
            isFocused.current = focusState;
        }
    };

    //keyboard handlers
    const handleArrowDown = () => {
        const lastSuggestionIndex = editorState.suggestions.length - 1;
        setCurrentIndex(currentIndex() === lastSuggestionIndex ? 0 : currentIndex() + 1);
    };

    const handleArrowUp = () => {
        const lastSuggestionIndex = editorState.suggestions.length - 1;
        setCurrentIndex(currentIndex() === 0 ? lastSuggestionIndex : currentIndex() - 1);
    };

    const handleEnterPressed = () => {
        handleDropdownChange(editorState.suggestions[currentIndex()]);
        setCurrentIndex(0);
    };

    const handleTabPressed = () => {
        handleDropdownChange(editorState.suggestions[currentIndex()]);
    };

    const handleEscapePressed = () => {
        closeDropDown();
        editorState.suggestions = [];
        setSuggestions([]);
    };

    const makeDropDownRespondToKeyPress = (keyPressedFromInput: OverwrittenKeyTypes) => {
        // React state unknown
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
            case OVERWRITTEN_KEYS.Escape:
                handleEscapePressed();
                break;
            default:
            //do nothing
        }
    };

    //todo check out typings for event type
    const handleInputEditorKeyPress = (event: any) => {
        //only when dropdown is shown
        const overWrittenKeys: Array<string> = Object.values(OVERWRITTEN_KEYS);
        if (overWrittenKeys.includes(event.key) && editorState.dropdownShown) {
            //don't prevent when enter should create new line (multiline config) and dropdown isn't shown
            const overwrittenKey = OVERWRITTEN_KEYS[event.key as keyof typeof OVERWRITTEN_KEYS];
            makeDropDownRespondToKeyPress(overwrittenKey);
            event.preventDefault();
            event.stopPropagation();
            return true;
        }
        //no nesting
        // disallow enter after ":"
        const cursor = editorState.cm?.state.selection.main.head;
        const cursorLineText = editorState.cm?.state.doc.lineAt(cursor ?? 0)?.text;
        const colonIndex = cursorLineText?.indexOf(":");
        if (colonIndex && colonIndex > 0) {
            const textAfterColon = cursorLineText?.substring(colonIndex + 1).trim();
            if (!textAfterColon?.length) return true; //can't nest
        }
        //allow when you add |
        return false;
    };

    const onSelection = React.useMemo(
        () => (ranges: IRange[]) => {
            selectedTextRanges.current = ranges;
        },
        [],
    );

    const handleInputMouseDown = React.useCallback((editor: EditorView) => {
        const cursor = editorState.cm?.state.selection.main.head;
        const currentLine = editorState.cm?.state.doc.lineAt(cursor ?? 0).number;
        const clickedLine = editor?.state.doc.lineAt(cursor ?? 0).number;
        //Clicking on a different line other than the current line
        //where the dropdown already suggests should close the dropdown
        if (currentLine !== clickedLine) {
            closeDropDown();
            editorState.suggestions = [];
            setSuggestions([]);
        }
    }, []);

    const codeEditor = React.useMemo(() => {
        return (
            <ExtendedCodeEditor
                mode="yaml"
                setCM={setCM}
                onChange={handleChange}
                onCursorChange={handleCursorChange}
                onFocusChange={handleInputFocus}
                initialValue={props.initialValue ?? ""}
                onSelection={onSelection}
                onKeyDown={handleInputEditorKeyPress}
                onMouseDown={handleInputMouseDown}
                multiline
                enableTab
            />
        );
    }, [setCM]);

    return (
        <div
            id={props.id}
            ref={autoSuggestionDivRef}
            className={`${eccgui}-autosuggestion` + (props.className ? ` ${props.className}` : "")}
        >
            <div
                className={` ${eccgui}-autosuggestion__inputfield ${BlueprintClassNames.INPUT_GROUP} ${
                    BlueprintClassNames.FILL
                } ${hasError ? BlueprintClassNames.INTENT_DANGER : ""}`}
            >
                <ContextOverlay
                    minimal
                    fill
                    isOpen={shouldShowDropdown}
                    placement="bottom-start"
                    modifiers={{ flip: { enabled: false } }}
                    openOnTargetFocus={false}
                    autoFocus={false}
                    content={
                        <AutoSuggestionList
                            id={props.id + "__dropdown"}
                            offsetValues={dropdownXYoffset.current}
                            loading={suggestionsPending}
                            options={suggestions}
                            isOpen={!suggestionsPending && shouldShowDropdown}
                            onItemSelectionChange={handleDropdownChange}
                            currentlyFocusedIndex={focusedIndex}
                            itemToHighlight={handleItemHighlighting}
                        />
                    }
                >
                    {codeEditor}
                </ContextOverlay>
                {!!value.current && (
                    <span className={BlueprintClassNames.INPUT_ACTION} ref={inputactionsDisplayed}>
                        <IconButton
                            data-test-id={"value-path-clear-btn"}
                            name="operation-clear"
                            text="Clear"
                            onClick={handleInputEditorClear}
                        />
                    </span>
                )}
            </div>
        </div>
    );
};
