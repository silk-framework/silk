import React from 'react';
import PropTypes from 'prop-types';
import { DismissiveButton, DisruptiveButton, ConfirmationDialog } from '@eccenca/gui-elements';
import { MAPPING_RULE_TYPE_OBJECT } from '../helpers';

const RemoveMappingRuleDialog = props => {
    const {
        mappingType, handleConfirmRemove, handleCancelRemove,
    } = props;
    return (
        <ConfirmationDialog
            className="ecc-hm-delete-dialog"
            active
            modal
            title="Remove mapping rule?"
            confirmButton={
                <DisruptiveButton
                    className="ecc-hm-delete-accept"
                    onClick={handleConfirmRemove}
                >
                  Remove
                </DisruptiveButton>
            }
            cancelButton={
                <DismissiveButton
                    className="ecc-hm-delete-cancel"
                    onClick={handleCancelRemove}
                >
                  Cancel
                </DismissiveButton>
            }
        >
            <p>
              When you click REMOVE the mapping rule
                {
                    mappingType === MAPPING_RULE_TYPE_OBJECT
                        ? ' including all child rules '
                        : ' '
                }
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
    mappingType: '',
};

export default RemoveMappingRuleDialog;
