import { IItemLink } from "@ducks/shared/typings";
import { IframeWindow } from "./IframeWindow";
import React, { useState } from "react";

/** An I-frame supported version for item links. */
export const useIFrameWindowLinks = (srcLinks: IItemLink[], startLink?: IItemLink) => {
    // active legacy link
    const [displayLegacyLink, setDisplayLegacyLink] = useState<IItemLink | undefined>(startLink);
    // handler for link change
    const toggleIFrameLink = (linkItem?: IItemLink) => {
        setDisplayLegacyLink(linkItem);
    };
    const returnElement: JSX.Element | undefined = displayLegacyLink && (
        <IframeWindow
            srcLinks={srcLinks.map((link) => {
                return {
                    path: link.path,
                    label: link.label,
                    itemType: null,
                };
            })}
            startWithLink={displayLegacyLink}
            startFullscreen={true}
            handlerRemoveModal={() => toggleIFrameLink(undefined)}
        />
    );
    return { iframeWindow: returnElement, toggleIFrameLink };
};
