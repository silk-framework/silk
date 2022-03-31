import React from "react";
import { Tags } from "@ducks/workspace/typings";
import { Highlighter, Spacing, Tag } from "gui-elements";
import metadataUtils from "../Metadata/MetadataUtils";

interface IProps {
    maxLength?: number;
    tags?: Tags["tags"];
    query?: string;
}

const ProjectTags: React.FC<IProps> = ({ tags = [], maxLength = 4, query = "" }) => {
    const filteredTags = metadataUtils.sortTags(tags.filter((t) => t.label.includes(query)));
    return (
        <>
            {filteredTags.map((t, i) => (
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
