import React from "react";
import PropTypes from "prop-types";
import { MAPPING_RULE_TYPE_OBJECT } from "../utils/constants";
import { Button, SimpleDialog, HtmlContentBlock } from "@eccenca/gui-elements";

const RemoveMappingRuleDialog = ({ mappingType = "", handleConfirmRemove, handleCancelRemove, label }) => {
    return (
        <SimpleDialog
            title={`Remove mapping rule ${label ? ` "${label}"` : ""}`}
            className="ecc-hm-delete-dialog"
            isOpen={true}
            intent={"danger"}
            size={"small"}
            actions={[
                <Button key={"remove"} disruptive className="ecc-hm-delete-accept" onClick={handleConfirmRemove}>
                    Remove
                </Button>,
                <Button key={"cancel"} className="ecc-hm-delete-cancel" onClick={handleCancelRemove}>
                    Cancel
                </Button>,
            ]}
        >
            <HtmlContentBlock>
                <p>
                    Remove the mapping rule{label ? ` "${label}"` : ""}
                    {mappingType === MAPPING_RULE_TYPE_OBJECT ? " including all child rules " : " "}
                    permanently?
                </p>
            </HtmlContentBlock>
        </SimpleDialog>
    );
};

RemoveMappingRuleDialog.propTypes = {
    mappingType: PropTypes.string,
    handleConfirmRemove: PropTypes.func.isRequired,
    handleCancelRemove: PropTypes.func.isRequired,
};

export default RemoveMappingRuleDialog;
