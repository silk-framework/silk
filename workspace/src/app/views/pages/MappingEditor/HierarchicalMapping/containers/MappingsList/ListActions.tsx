import React from "react";
import { ContextMenu, MenuItem, Button } from "@eccenca/gui-elements";
import _ from "lodash";
import { MAPPING_RULE_TYPE_DIRECT, MAPPING_RULE_TYPE_OBJECT } from "../../utils/constants";

interface ListActionsProps {
    // Executes when one of the create mapping options are clicked. The type specifies the type of mapping.
    onMappingCreate: (mappingSkeleton: { type: "direct" | "object" }) => any;
    // Executes when the 'Paste' option is clicked
    onPaste: () => any;
    // Executes when the 'Mapping suggestion' option is clicked
    onShowSuggestions: () => any;
    // true if the mapping rules list is still loading
    listLoading: boolean;
}

const ListActions = ({ onMappingCreate, onPaste, onShowSuggestions, listLoading }: ListActionsProps) => {
    return listLoading ? null : (
        <ContextMenu
            togglerText={""}
            togglerElement={
                <Button
                    data-test-id={"add-mapping-button"}
                    text={"Add mapping"}
                    icon={"item-add-artefact"}
                    rightIcon={"toggler-caretdown"}
                    elevated
                    outlined
                />
            }
        >
            <MenuItem
                data-test-id={"add-value-mapping-btn"}
                text={"Add value mapping"}
                icon={"artefact-file"}
                onClick={() => {
                    onMappingCreate({
                        type: MAPPING_RULE_TYPE_DIRECT,
                    });
                }}
            />
            <MenuItem
                data-test-id={"add-object-mapping-btn"}
                text={"Add object mapping"}
                icon={"artefact-project"}
                onClick={() => {
                    onMappingCreate({
                        type: MAPPING_RULE_TYPE_OBJECT,
                    });
                }}
            />
            {sessionStorage.getItem("copyingData") !== null ? (
                <MenuItem
                    data-test-id={"paste-mapping-btn"}
                    text={"Paste mapping"}
                    icon={"artefact-project"}
                    onClick={() => onPaste()}
                />
            ) : (
                <></>
            )}
            <MenuItem
                data-test-id={"suggest-mapping-btn"}
                text={"Suggest mappings"}
                icon={"item-magic-edit"}
                onClick={(e) => {
                    e.stopPropagation();
                    onShowSuggestions();
                }}
            />
        </ContextMenu>
    );
};

export default ListActions;
