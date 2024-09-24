import React from "react";

import { Button, HtmlContentBlock, Markdown, SimpleDialog, CLASSPREFIX as eccgui } from "@eccenca/gui-elements";
import { ArtefactDocumentation } from "./CreateArtefactModal";

interface TaskDocumentationModalProps {
    documentationToShow: ArtefactDocumentation;
    onClose: () => any;
}

const testId = "artefact-documentation-modal";
const headerRegex = /^h[0-9]+/;

// Finds a heading that is eiter parent of this element or is a sibling before one of its parent, i.e. a sibling of a parent must be a heading.
const findHeadingBefore = (element: Element): Element | undefined => {
    let upperParent: Element | null = element;
    const isHeader = (element: Element): boolean => headerRegex.test(element.tagName.toLowerCase());
    const isHeaderOrParentDiv = (element: Element): boolean =>
        (element.parentElement && element.parentElement.tagName.toLowerCase() === "div") || isHeader(element);
    while (upperParent && !isHeaderOrParentDiv(upperParent)) {
        upperParent = upperParent.parentElement;
    }
    let sibling: Element | null = upperParent;
    let maxSiblingCount = 5;
    while (sibling && maxSiblingCount > 0) {
        if (isHeader(sibling)) {
            return sibling;
        } else {
            sibling = sibling.previousElementSibling;
        }
        maxSiblingCount -= 1;
    }
};

/** Modal that shows the detailed task documentation, e.g. the Markdown. */
export const TaskDocumentationModal = ({ documentationToShow, onClose }: TaskDocumentationModalProps) => {
    const [initialized, setInitialized] = React.useState(false);

    React.useEffect(() => {
        // If an anchor is defined, jump to it
        if (initialized && documentationToShow.markdownDocumentation && documentationToShow.namedAnchor) {
            const element = document.querySelector(
                `[data-test-id="${testId}"] a[id="${documentationToShow.namedAnchor}"]`
            );
            if (element) {
                element.classList.add(`${eccgui}-typography--spothighlight`);
                const previousHeader = findHeadingBefore(element);
                if (previousHeader) {
                    // Scroll to previous header to have the context heading in view, link will be highlighted in addition
                    setTimeout(
                        // use minimal timeout to scroll via JS after the browser scrolled to the local anchor
                        function () {
                            previousHeader.scrollIntoView({ block: "start", inline: "nearest", behavior: "smooth" });
                        },
                        1
                    );
                } else {
                    setTimeout(function () {
                        element.scrollIntoView({ block: "center", inline: "nearest", behavior: "smooth" });
                    }, 1);
                }
            }
        }
    }, [documentationToShow, initialized]);

    return (
        <SimpleDialog
            data-test-id={testId}
            isOpen
            showFullScreenToggler={true}
            enforceFocus={true}
            onClose={onClose}
            title={documentationToShow.title ?? "Documentation"}
            actions={<Button text="Close" onClick={onClose} />}
            size="large"
        >
            <HtmlContentBlock>
                <span ref={() => setInitialized(true)} />
                <Markdown allowHtml>
                    {documentationToShow.markdownDocumentation || documentationToShow.description || ""}
                </Markdown>
            </HtmlContentBlock>
        </SimpleDialog>
    );
};
