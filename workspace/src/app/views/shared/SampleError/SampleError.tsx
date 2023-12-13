import React from "react";
import {IconButton} from "@eccenca/gui-elements"
import {SampleErrorModal} from "./SampleErrorModal";

interface SampleErrorProps {
    /** The sample error. */
    sampleError: SampleError
}

export interface SampleError {
    /** The entity URI for which the error happened. */
    entity: string
    /** The input values, e.g. for the operator that threw the error. */
    values: string[][]
    /** The main error message. */
    error: string
    /** Optional stacktrace. */
    stacktrace?: Stacktrace
}

export interface Stacktrace {
    /** The error message on the current level of the stacktrace. */
    errorMessage: string
    /** The class of the exception. */
    exceptionClass: string
    /** The lines of the stacktrace for the current level, i.e. without causes. */
    lines: string[]
    /** The optional cause, i.e. a nested stacktrace. */
    cause?: Stacktrace
}

export const SampleError = ({sampleError}: SampleErrorProps) => {
    // Shows an error report modal with all the details
    const [showSameErrorReport, setShowSampleErrorReport] = React.useState(false)

    const onClick = React.useCallback(() => {
        setShowSampleErrorReport(old => !old)
    }, [])

    return <>
        <IconButton
            key={"show-report"}
            data-test-id={"show-sample-error-btn"}
            name={"artefact-report"}
            intent={"warning"}
            onClick={onClick}
        />
        {showSameErrorReport ?
            <SampleErrorModal
                sampleError={sampleError}
                onClose={() => setShowSampleErrorReport(false)}
            /> :
            null
        }
    </>
}
