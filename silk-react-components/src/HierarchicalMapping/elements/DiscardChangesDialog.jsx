import React from 'react';
import PropTypes from 'prop-types';
import { DismissiveButton, DisruptiveButton, ConfirmationDialog } from '@eccenca/gui-elements';

const DiscardChangesDialog = props => {
    const {
        numberEditingElements, handleDiscardCancel, handleDiscardConfirm,
    } = props;
    return (
        <ConfirmationDialog
            active
            modal
            className="ecc-hm-discard-dialog"
            title="Discard changes?"
            confirmButton={
                <DisruptiveButton
                    className="ecc-hm-accept-discard"
                    onClick={handleDiscardConfirm}
                >
                    Discard
                </DisruptiveButton>
            }
            cancelButton={
                <DismissiveButton
                    className="ecc-hm-cancel-discard"
                    onClick={handleDiscardCancel}
                >
                    Cancel
                </DismissiveButton>
            }
        >
            <p>
                {
                    `You currently have unsaved changes${
                        numberEditingElements === 1 ? '' : ` in ${numberEditingElements} mapping rules`
                    }.`
                }
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
