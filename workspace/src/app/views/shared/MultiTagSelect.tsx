import { Keyword } from "@ducks/workspace/typings";
import { MultiSelect } from "@eccenca/gui-elements";
import React from "react";
import utils from "./Metadata/MetadataUtils";
import useErrorHandler from "../../hooks/useErrorHandler";
import { SelectedParamsType } from "@eccenca/gui-elements/src/components/MultiSelect/MultiSelect";
import { useTranslation } from "react-i18next";
import { removeExtraSpaces } from "@eccenca/gui-elements/src/common/utils/stringUtils";

interface IProps {
    projectId?: string;
    handleTagSelectionChange: (params: SelectedParamsType<Keyword>) => any;
    initialTags?: Keyword[];
}

/** Multi selection component for project and task tags. */
export const MultiTagSelect = ({ projectId, handleTagSelectionChange, initialTags }: IProps) => {
    const { registerError } = useErrorHandler();
    const [t] = useTranslation();

    const handleTagQueryChange = React.useCallback(
        async (query: string) => {
            if (projectId) {
                try {
                    const res = await utils.queryTags(projectId, query);
                    return res?.data.tags ?? [];
                } catch (ex) {
                    registerError(
                        "MultiTagSelect-handleTagQueryChange",
                        "An error occurred while searching for tags.",
                        ex
                    );
                    return [];
                }
            }
        },
        [projectId]
    );

    return (
        <MultiSelect<Keyword>
            prePopulateWithItems={!!initialTags}
            openOnKeyDown
            itemId={(keyword) => keyword.uri}
            itemLabel={(keyword) => keyword.label}
            items={initialTags ?? []}
            onSelection={handleTagSelectionChange}
            runOnQueryChange={handleTagQueryChange}
            newItemCreationText={t("Metadata.addNewTag")}
            newItemPostfix={t("Metadata.newTagPostfix")}
            inputProps={{
                placeholder: `${t("form.field.searchOrEnterTags")}...`,
            }}
            tagInputProps={{
                placeholder: `${t("form.field.searchOrEnterTags")}...`,
            }}
            createNewItemFromQuery={(query) => ({
                uri: removeExtraSpaces(query),
                label: removeExtraSpaces(query),
            })}
            requestDelay={200}
        />
    );
};