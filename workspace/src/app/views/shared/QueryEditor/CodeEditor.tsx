import React, { useEffect, useRef } from "react";
import CodeMirror from "codemirror";
import "codemirror/mode/sparql/sparql.js";

interface IProps {
    name: string;
    onChange: (v) => void;
    mode?: string;
    defaultValue?: any;
}

export function CodeEditor({ onChange, name, mode = "sparql", defaultValue }: IProps) {
    const ref = useRef();

    useEffect(() => {
        if (onChange !== undefined) {
            const editorInstance = CodeMirror.fromTextArea(ref.current, {
                mode: mode,
                lineWrapping: true,
                lineNumbers: true,
                tabSize: 2,
                theme: "xq-light",
            });

            editorInstance.on("change", (api) => {
                onChange(api.getValue());
            });
        }
    }, [onChange]);

    return (
        <textarea
            data-test-id="codemirror-wrapper"
            ref={ref}
            id={"codemirror"}
            name={name}
            defaultValue={defaultValue}
        />
    );
}
