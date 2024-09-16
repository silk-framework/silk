import React from "react";

import {Button, HtmlContentBlock, Markdown, SimpleDialog, CLASSPREFIX as eccgui} from "@eccenca/gui-elements"
import {ArtefactDocumentation} from "./CreateArtefactModal";

interface TaskDocumentationModalProps {
    documentationToShow: ArtefactDocumentation
    onClose: () => any
}

const testId = "artefact-documentation-modal"

/** Modal that shows the detailed task documentation, e.g. the Markdown. */
export const TaskDocumentationModal = ({documentationToShow, onClose}: TaskDocumentationModalProps) => {
    const [initialized, setInitialized] = React.useState(false)

    React.useEffect(() => {
        // If an anchor is defined, jump to it
        if(initialized && documentationToShow.markdownDocumentation && documentationToShow.namedAnchor) {
            const element = document.querySelector(`[data-test-id="${testId}"] a[id="${documentationToShow.namedAnchor}"]`)
            if(element) {
                element.classList.add(`${eccgui}-link__tempHighlight`)
                element.scrollIntoView()
            }
        }
    }, [documentationToShow, initialized])

    return <SimpleDialog
        data-test-id={testId}
        isOpen
        showFullScreenToggler={true}
        enforceFocus={true}
        onClose={onClose}
        title={documentationToShow.title ?? "Documentation"}
        actions={
            <Button
                text="Close"
                onClick={onClose}
            />
        }
        size="large"
    >
        <HtmlContentBlock>
            <span ref={() => setInitialized(true)} />
            <Markdown allowHtml>
                {documentationToShow.markdownDocumentation || documentationToShow.description || ""}
            </Markdown>
        </HtmlContentBlock>
    </SimpleDialog>
}
