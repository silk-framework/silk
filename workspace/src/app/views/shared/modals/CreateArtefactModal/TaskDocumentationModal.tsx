import React, { useCallback } from "react";

import {
    Button,
    Markdown,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDescription,
    OverviewItemLine,
    OverviewItemList,
    TitleSubsection,
    SimpleDialog,
    SimpleDialogProps,
    Spacing,
    CLASSPREFIX as eccgui,
    OverflowText,
    Tooltip,
    Notification,
    CardActionsAux,
} from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { ArtefactDocumentation } from "./CreateArtefactModal";
import { IPluginOverview } from "@ducks/common/typings";

interface TaskDocumentationModalProps {
    documentationToShow: ArtefactDocumentation;
    size?: SimpleDialogProps["size"];
    onClose: () => any;
    onSwitchToRelatedPlugin?: (plugin: IPluginOverview) => void;
}

const testId = "artefact-documentation-modal";
const headerRegex = /^h[0-9]+/;

// Finds a heading that is either parent of this element or is a sibling before one of its parent, i.e. a sibling of a parent must be a heading.
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
export const TaskDocumentationModal = ({
    documentationToShow,
    onClose,
    onSwitchToRelatedPlugin,
    size = "large",
}: TaskDocumentationModalProps) => {
    const [initialized, setInitialized] = React.useState(false);
    const [shrinkRelatedPlugins, setShrinkRelatedPlugins] = React.useState<string | null>(
        localStorage.getItem("shrinkRelatedPlugins"),
    );
    const [t] = useTranslation();

    const toggleRelatedPlugins = () => {
        if (shrinkRelatedPlugins) {
            localStorage.removeItem("shrinkRelatedPlugins");
            setShrinkRelatedPlugins(null);
        } else {
            localStorage.setItem("shrinkRelatedPlugins", "true");
            setShrinkRelatedPlugins("true");
        }
    };

    React.useEffect(() => {
        // If an anchor is defined, jump to it
        if (initialized && documentationToShow.markdownDocumentation) {
            // Make anchor links non-functional
            const elements = document.querySelectorAll(`[data-test-id=${testId}] a[id^=parameter_doc_]`);
            elements.forEach((link) => {
                // Disable anchor links
                const l = link as HTMLLinkElement;
                l.onclick = () => false;
            });
            // Highlight named anchor
            if (documentationToShow.namedAnchor) {
                const element = document.querySelector(
                    `[data-test-id="${testId}"] a[id="${documentationToShow.namedAnchor}"]`,
                );
                if (element) {
                    element.classList.add(`${eccgui}-typography--spothighlight`);
                    const previousHeader = findHeadingBefore(element);
                    if (previousHeader) {
                        // Scroll to previous header to have the context heading in view, link will be highlighted in addition
                        setTimeout(
                            // use minimal timeout to scroll via JS after the browser scrolled to the local anchor
                            function () {
                                previousHeader.scrollIntoView({
                                    block: "start",
                                    inline: "nearest",
                                    behavior: "smooth",
                                });
                            },
                            1,
                        );
                    } else {
                        setTimeout(function () {
                            element.scrollIntoView({ block: "center", inline: "nearest", behavior: "smooth" });
                        }, 1);
                    }
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
            actions={
                <>
                    <Button text={t("common.action.close")} onClick={onClose} />
                    {shrinkRelatedPlugins && (
                        <CardActionsAux>
                            <Button
                                outlined
                                icon={"toggler-showless"}
                                text={t("CreateModal.relatedPlugins.displayPlugins")}
                                onClick={() => toggleRelatedPlugins()}
                            />
                        </CardActionsAux>
                    )}
                </>
            }
            size={size}
            notifications={
                !shrinkRelatedPlugins &&
                documentationToShow.relatedPlugins &&
                documentationToShow.relatedPlugins.length > 0 && (
                    <Notification intent={"neutral"} onDismiss={() => toggleRelatedPlugins()} timeout={0}>
                        <TitleSubsection>{t("CreateModal.relatedPlugins.title")}</TitleSubsection>
                        <Spacing size={"tiny"} />
                        <OverviewItemList data-test-id="related-plugins-list" columns={1} hasSpacing>
                            {documentationToShow.relatedPlugins.map((relatedPlugin) => {
                                const pluginLabel = relatedPlugin.plugin.title ?? relatedPlugin.plugin.key;
                                return (
                                    <OverviewItem
                                        key={relatedPlugin.plugin.key}
                                        data-test-id={`related-plugin-${relatedPlugin.plugin.key}`}
                                        hasCardWrapper
                                        hasSpacing
                                    >
                                        <OverviewItemDescription>
                                            <OverviewItemLine>
                                                <strong>{pluginLabel}</strong>
                                            </OverviewItemLine>
                                            <OverviewItemLine small>
                                                <Tooltip content={relatedPlugin.description}>
                                                    <OverflowText>{relatedPlugin.description}</OverflowText>
                                                </Tooltip>
                                            </OverviewItemLine>
                                        </OverviewItemDescription>
                                        {onSwitchToRelatedPlugin && (
                                            <OverviewItemActions>
                                                <Button
                                                    outlined
                                                    elevated
                                                    data-test-id={`related-plugin-${relatedPlugin.plugin.key}-use-btn`}
                                                    tooltip={t("CreateModal.relatedPlugins.switchTooltip", {
                                                        pluginLabel,
                                                    })}
                                                    onClick={() => onSwitchToRelatedPlugin(relatedPlugin.plugin)}
                                                >
                                                    {t("CreateModal.relatedPlugins.switchAction", {
                                                        pluginLabel,
                                                    })}
                                                </Button>
                                            </OverviewItemActions>
                                        )}
                                    </OverviewItem>
                                );
                            })}
                        </OverviewItemList>
                    </Notification>
                )
            }
        >
            <span ref={() => setInitialized(true)} />
            <Markdown allowHtml>
                {documentationToShow.markdownDocumentation || documentationToShow.description || ""}
            </Markdown>
        </SimpleDialog>
    );
};
