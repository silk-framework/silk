import { Keywords } from "@ducks/workspace/typings";
import { Highlighter, Spacing, Tag } from "@eccenca/gui-elements";
import React from "react";

import metadataUtils from "../Metadata/MetadataUtils";

interface IProps {
    maxLength?: number;
    tags?: Keywords;
    query?: string;
}

const ProjectTags: React.FC<IProps> = ({ tags = [], query = "" }) => {
    return (
        <>
            {metadataUtils.sortTags(tags.slice()).map((t, i) => (
                <div key={i}>
                    <Tag emphasis="weaker">
                        <Highlighter label={t.label} searchValue={query} />
                    </Tag>
                    <Spacing size="tiny" vertical />
                </div>
            ))}
        </>
    );
};

export default ProjectTags;
