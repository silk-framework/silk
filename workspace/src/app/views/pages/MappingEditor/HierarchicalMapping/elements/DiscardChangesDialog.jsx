import React from "react";
import PropTypes from "prop-types";
import { ConfirmationDialog } from "gui-elements-deprecated";
import { Button } from "@eccenca/gui-elements";

const DiscardChangesDialog = (props) => {
    const { numberEditingElements, handleDiscardCancel, handleDiscardConfirm } = props;
    return (
        <ConfirmationDialog
            active
            modal
            className="ecc-hm-discard-dialog"
            title="Discard changes?"
            confirmButton={
                <Button disruptive className="ecc-hm-accept-discard" onClick={handleDiscardConfirm}>
                    Discard
                </Button>
            }
            cancelButton={
                <Button className="ecc-hm-cancel-discard" onClick={handleDiscardCancel}>
                    Cancel
                </Button>
            }
        >
            <p>
                {`You currently have unsaved changes${
                    numberEditingElements === 1 ? "" : ` in ${numberEditingElements} mapping rules`
                }.`}
            </p>
        </ConfirmationDialog>
    );
};

DiscardChangesDialog.propTypes = {
    numberEditingElements: PropTypes.number,
    handleDiscardCancel: PropTypes.func.isRequired,
    handleDiscardConfirm: PropTypes.func.isRequired,
};

DiscardChangesDialog.defaultProps = {
    numberEditingElements: 0,
};

export default DiscardChangesDialog;
