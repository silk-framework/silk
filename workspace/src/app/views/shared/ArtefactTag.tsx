import React from "react";
import { Tag, TagProps, utils } from "@eccenca/gui-elements";
import { extend } from "lodash";

interface ArtefactTagProps extends Omit<TagProps, "intent" | "emphasis" | "minimal"> {
    /**
     * Specify artefact type.
     * This leads to a tag element with a configured background color.
     */
    artefactType:
        | "datasetNode"
        | "linkingNode"
        | "transformNode"
        | "taskNode"
        | "workflowNode"
        | "replaceableInput"
        | string;
    /**
     * Stronger appearance.
     * Usually a darker background color is used then.
     */
    strong?: boolean;
}

export const ArtefactTag = ({ artefactType, strong, ...otherTagProps }: ArtefactTagProps) => {
    const paletteWorkflow = utils.getColorConfiguration("react-flow-workflow");
    const tagColors = { ...paletteWorkflow }; // maybe we have more colors later
    const colorKey = artefactType + (strong ? "" : "Bright");
    return (
        <Tag
            className="diapp-artefacttag"
            backgroundColor={tagColors[colorKey]} // TODO: calculate color hash if not defined (possible when CMEM-6442 will be done)
            {...otherTagProps}
        />
    );
};
