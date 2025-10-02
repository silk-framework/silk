import React from "react";
import PropTypes from "prop-types";
import { ConfirmationDialog } from "gui-elements-deprecated";
import { MAPPING_RULE_TYPE_OBJECT } from "../utils/constants";
import { Button } from "@eccenca/gui-elements";

const RemoveMappingRuleDialog = (props) => {
    const { mappingType, handleConfirmRemove, handleCancelRemove, label } = props;
    return (
        <ConfirmationDialog
            className="ecc-hm-delete-dialog"
            active
            modal
            title="Remove mapping rule?"
            confirmButton={
                <Button disruptive className="ecc-hm-delete-accept" onClick={handleConfirmRemove}>
                    Remove
                </Button>
            }
            cancelButton={
                <Button className="ecc-hm-delete-cancel" onClick={handleCancelRemove}>
                    Cancel
                </Button>
            }
        >
            <p>
                When you click REMOVE the mapping rule{label ? ` '${label}'` : ""}
                {mappingType === MAPPING_RULE_TYPE_OBJECT ? " including all child rules " : " "}
                will be deleted permanently.
            </p>
        </ConfirmationDialog>
    );
};

RemoveMappingRuleDialog.propTypes = {
    mappingType: PropTypes.string,
    handleConfirmRemove: PropTypes.func.isRequired,
    handleCancelRemove: PropTypes.func.isRequired,
};

RemoveMappingRuleDialog.defaultProps = {
    mappingType: "",
};

export default RemoveMappingRuleDialog;
