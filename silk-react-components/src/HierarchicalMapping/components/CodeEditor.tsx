import React from "react";
import { Controlled as ControlledEditor } from "react-codemirror2";
import "codemirror/mode/sparql/sparql.js";
import PropTypes from "prop-types";

export function CodeEditor({
    onChange,
    onCursorChange,
    onSelection,
    mode = "sparql",
    value,
}) {
    return (
        <div className="ecc-input-editor">
            <ControlledEditor
                value={value}
                onSelection={(editor) => onSelection(editor.getSelection())}
                options={{
                    mode: mode,
                    lineWrapping: true,
                    lineNumbers: false,
                    theme: "xq-light",
                }}
                onCursor={(editor, data) => {
                    onCursorChange(data, editor.cursorCoords(true));
                }}
                onBeforeChange={(editor, data, value) => onChange(value)}
            />
        </div>
    );
}

CodeEditor.propTypes = {
    mode: PropTypes.string,
    value: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    onCursorChange: PropTypes.func.isRequired,
    onSelection: PropTypes.func,
};
