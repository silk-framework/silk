import React from "react";
import CodeMirror from "codemirror";
import { Icon } from "@eccenca/gui-elements";

//custom components
import { CodeEditor } from "../CodeEditor";
import Dropdown from "./Dropdown";
import { replace } from "lodash";

//styles
require("./AutoSuggestion.scss");

const AutoSuggestion = ({
    onEditorParamsChange,
    data,
    checkPathValidity,
    validationResponse,
    pathValidationPending,
    suggestionsPending,
}) => {
    const [value, setValue] = React.useState("");
    const [inputString, setInputString] = React.useState("");
    const [cursorPosition, setCursorPosition] = React.useState(0);
    const [coords, setCoords] = React.useState({ left: 0 });
    const [shouldShowDropdown, setShouldShowDropdown] = React.useState(false);
    const [replacementIndexesDict, setReplacementIndexesDict] = React.useState(
        {}
    );
    const [suggestions, setSuggestions] = React.useState<
        Array<{ value: string; description?: string; label?: string }>
    >([]);
    const [markers, setMarkers] = React.useState([]);
    const [
        editorInstance,
        setEditorInstance,
    ] = React.useState<CodeMirror.Editor>();
    const pathIsValid = validationResponse?.valid ?? true

    //handle linting 
    React.useEffect(() => {
        const parseError = validationResponse?.parseError
        if(parseError){
            clearMarkers()
            const {offset:start, inputLeadingToError} = parseError
            const end = start + inputLeadingToError?.length 
            const marker = editorInstance.markText(
                { line: 0, ch: start },
                { line: 0, ch: end },
                { className: "ecc-text-error-highlighting" }
            );
            setMarkers((previousMarkers) => [...previousMarkers, marker]);
        }
    },[validationResponse?.parseError])

    /** generate suggestions and also populate the replacement indexes dict */
    React.useEffect(() => {
        let newSuggestions = [];
        let newReplacementIndexesDict = {};
        if (
            data?.replacementResults?.length === 1 &&
            !data?.replacementResults?.replacements?.length
        ) {
            setShouldShowDropdown(false);
        }
        if (data?.replacementResults?.length) {
            data.replacementResults.forEach(
                ({ replacements, replacementInterval: { from, length } }) => {
                    newSuggestions = [...newSuggestions, ...replacements];
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
            setSuggestions(() => newSuggestions);
            setReplacementIndexesDict(() => newReplacementIndexesDict);
        }
    }, [data]);

    React.useEffect(() => {
        setInputString(() => value);
        setShouldShowDropdown(true);
        checkPathValidity(inputString);
        onEditorParamsChange(inputString, cursorPosition);
    }, [cursorPosition, value, inputString]);

    const handleChange = (val) => {
        setValue(val);
    };

    const handleCursorChange = (pos, coords) => {
        setCursorPosition(pos.ch);
        setCoords(() => coords);
    };

    const handleTextHighlighting = (focusedSuggestion: string) => {
        const indexes = replacementIndexesDict[focusedSuggestion];
        if (indexes) {
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

    const clearMarkers = () => {
        markers.forEach((marker) => marker.clear());
    };

    const handleDropdownChange = (selectedSuggestion: string) => {
        const indexes = replacementIndexesDict[selectedSuggestion];
        if (indexes) {
            const { from, length } = indexes;
            const to = from + length;
            setValue(
                (value) =>
                    `${value.substring(
                        0,
                        from
                    )}${selectedSuggestion}${value.substring(to)}`
            );
            setShouldShowDropdown(false);
            editorInstance.setCursor({ line: 0, ch: to });
            clearMarkers();
        }
    };

    const handleInputEditorClear = () => {
        if (!pathIsValid) {
            setValue("");
        }
    };

    return (
        <div className="ecc-auto-suggestion-box">
            <div className="ecc-auto-suggestion-box__editor-box">
                <CodeEditor
                    mode={'null'}
                    setEditorInstance={setEditorInstance}
                    onChange={handleChange}
                    onCursorChange={handleCursorChange}
                    value={value}
                />
                {!pathIsValid && (
                    <div onClick={handleInputEditorClear}>
                        <Icon className="editor__icon clear" name="clear" />
                    </div>
                )}
            </div>
            {shouldShowDropdown ? (
                <div
                    className="ecc-auto-suggestion-box__dropdown"
                    style={{ left: coords.left }}
                >
                    <Dropdown
                        query={value}
                        options={suggestions}
                        isOpen={shouldShowDropdown}
                        onItemSelectionChange={handleDropdownChange}
                        onMouseOverItem={handleTextHighlighting}
                    />
                </div>
            ) : null}
        </div>
    );
};

export default AutoSuggestion;
