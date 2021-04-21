import React from "react";
import { Controlled as ControlledEditor } from "react-codemirror2";
import "codemirror/mode/sparql/sparql.js";
import PropTypes from "prop-types";

export function CodeEditor({
    setEditorInstance,
    onChange,
    onCursorChange,
    mode = "sparql",
    value,
}) {
    return (
        <div className="ecc-input-editor">
            <ControlledEditor
                editorDidMount={(editor) => setEditorInstance(editor)}
                value={value}
                options={{
                    mode: mode,
                    lineNumbers: false,
                    theme: "xq-light",
                    lint:true, 
                    gutters:["CodeMirror-lint-markers"]
                }}
                onCursor={(editor, data) => {
                    onCursorChange(data, editor.cursorCoords(true, "div"));
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
