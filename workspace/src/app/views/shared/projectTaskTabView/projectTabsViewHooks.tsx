import { IItemLink } from "@ducks/shared/typings";
import { ProjectTaskTabView } from "./ProjectTaskTabView";
import React, { useState } from "react";

/** An I-frame supported version for item links. */
export const useProjectTabsView = (srcLinks: IItemLink[], startLink?: IItemLink) => {
    // active legacy link
    const [displayLegacyLink, setDisplayLegacyLink] = useState<IItemLink | undefined>(startLink);
    // handler for link change TODO: Add custom views
    const toggleIFrameLink = (linkItem?: IItemLink) => {
        setDisplayLegacyLink(linkItem);
    };
    const returnElement: JSX.Element | undefined = displayLegacyLink && (
        <ProjectTaskTabView
            srcLinks={srcLinks.map((link) => {
                return {
                    path: link.path,
                    label: link.label,
                    itemType: undefined,
                };
            })}
            startWithLink={displayLegacyLink}
            startFullscreen={true}
            handlerRemoveModal={() => toggleIFrameLink(undefined)}
        />
    );
    return { projectTabView: returnElement, toggleIFrameLink };
};
