import { Tags } from "@ducks/workspace/typings";
import { Highlighter, Spacing, Tag } from "gui-elements";
import React from "react";

interface IProps {
    maxLength?: number;
    tags?: Tags["tags"];
    query?: string;
}

const ProjectTags: React.FC<IProps> = ({ tags = [], maxLength = 4, query = "" }) => {
    const filteredTags = tags.filter((t) => t.label.includes(query));
    return (
        <>
            {filteredTags.slice(0, Math.min(tags.length, maxLength)).map((t, i) => (
                <div key={i}>
                    <Tag emphasis="weak">
                        <Highlighter label={t.label} searchValue={query} />
                    </Tag>
                    <Spacing size="tiny" vertical />
                </div>
            ))}
        </>
    );
};

export default ProjectTags;
