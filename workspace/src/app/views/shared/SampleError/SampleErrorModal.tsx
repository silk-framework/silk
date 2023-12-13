import {SampleError, Stacktrace} from "./SampleError";
import {Button, Markdown, SimpleDialog} from "@eccenca/gui-elements"
import React from "react";
import {useTranslation} from "react-i18next";

interface SampleErrorModalProps {
    /** The sample error */
    sampleError: SampleError
    /** Callback to close this modal. */
    onClose
}
/** Shows the details of a sample error. */
export const SampleErrorModal = ({sampleError, onClose}: SampleErrorModalProps) => {
    const [t] = useTranslation()
    const handleDownload = React.useCallback(() => {

    }, [])
    return <SimpleDialog
        title={t("SampleError.title")}
        isOpen={true}
        size={"large"}
        onClose={onClose}
        showFullScreenToggler={true}
        actions={[
            <Button data-test-id={"sample-error-report-download-btn"} affirmative onClick={handleDownload} key="download">
                {t("common.action.download")}
            </Button>,
            <Button data-test-id={"sample-error-report-close-btn"} key="close" onClick={onClose}>
                {t("common.action.close")}
            </Button>,
        ]}
    >
        <Markdown>
            {sampleErrorToMarkdown(sampleError)}
        </Markdown>
    </SimpleDialog>
}

const stacktraceToMarkdown = (stacktrace: Stacktrace, prefix: string = "* Stacktrace:"): string => {
    const lines = stacktrace.lines
    return `${prefix}
  \`\`\`
  ${lines.length ? stacktrace.lines[0] : ""}${stacktrace.lines.slice(1).map(line => `\n    ${line}`)}
  \`\`\`
${stacktrace.cause ? stacktraceToMarkdown(stacktrace.cause, "  Cause:") : ""}
`
}

export const sampleErrorToMarkdown = (sampleError: SampleError): string => {
    return `## Details

* Error message: \`${sampleError.error}\`
${sampleError.entity ? "* Input entity URI: `" + sampleError.entity + "`" : ""}
${sampleError.values.length ? `* Input values: ${sampleError.values.map((vals, idx) => `\n  ${idx + 1}. ${vals.map(v => `\`${v}\``).join(", ")}`)}` : ""}
${sampleError.stacktrace ? stacktraceToMarkdown(sampleError.stacktrace) : ""}
    `
}

