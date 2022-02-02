import { requestProjectTags } from "@ducks/workspace/requests";
import { Tags } from "@ducks/workspace/typings";
import { Highlighter, Spacing, Tag } from "gui-elements";
import React from "react";

interface IProps {
    projectId: string;
    query?: string;
    minLength?: number;
}

const ProjectTags: React.FC<IProps> = ({ projectId, query = "", minLength = 4 }) => {
    const [tags, setTags] = React.useState<Tags["tags"]>([]);
    React.useEffect(() => {
        requestProjectTags(projectId).then((res) => {
            setTags(res.data?.tags ?? []);
        });
    }, []);
    const filteredTags = tags.filter((t) => t.label.includes(query));
    return (
        <>
            {filteredTags.slice(0, minLength).map((t, i) => (
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
