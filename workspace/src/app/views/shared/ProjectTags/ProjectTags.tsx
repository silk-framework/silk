import React from "react";
import { KeywordProps } from "@ducks/workspace/typings";
import { Highlighter, Spacing, Tag } from "gui-elements";
import metadataUtils from "../Metadata/MetadataUtils";

interface IProps {
    maxLength?: number;
    tags?: KeywordProps;
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
