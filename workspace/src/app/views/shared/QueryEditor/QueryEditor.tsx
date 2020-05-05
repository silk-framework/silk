import React, { useEffect, useRef } from "react";
import CodeMirror from "codemirror";
import "codemirror/mode/sparql/sparql.js";

export function QueryEditor({ onChange, name }: any) {
    const ref = useRef();

    useEffect(() => {
        const editorInstance = CodeMirror.fromTextArea(ref.current, {
            mode: "sparql",
            lineWrapping: true,
            lineNumbers: true,
            tabSize: 2,
            theme: "xq-light",
        });

        editorInstance.on("change", (api) => {
            onChange(api.getValue());
        });
    }, []);

    return <textarea data-test-id="codemirror-wrapper" ref={ref} id={"codemirror"} name={name} />;
}
