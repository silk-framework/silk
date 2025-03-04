import React from "react";
import { Keywords } from "@ducks/workspace/typings";
import { Highlighter, Tag, TagList } from "@eccenca/gui-elements";
import metadataUtils from "../Metadata/MetadataUtils";

interface IProps {
    maxLength?: number;
    tags?: Keywords;
    query?: string;
}

export const projectTagsRenderer = (props: IProps): JSX.Element[] => {
    const { tags = [], query = "" } = props;
    return metadataUtils.sortTags(tags.slice()).map((t, i) => (
        <Tag emphasis="weaker" key={i}>
            <Highlighter label={t.label} searchValue={query} />
        </Tag>
    ));
};

const ProjectTags: React.FC<IProps> = (props) => {
    const projectTagsElements = projectTagsRenderer(props);
    return projectTagsElements.length > 0 ? (
        <TagList className="diapp-projecttags">{projectTagsElements}</TagList>
    ) : (
        <></>
    );
};

export default ProjectTags;
