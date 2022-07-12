import React from 'react';
import { FloatingActionList } from 'gui-elements-deprecated';
import _ from 'lodash';
import { MAPPING_RULE_TYPE_DIRECT, MAPPING_RULE_TYPE_OBJECT } from '../../utils/constants';

const openToBottomFn = () => {
    // Calculates if the floating menu list should be opened to the top or bottom depending on the space to the top.
    let toBottom = false;
    try {
        const floatButtonRect = document.querySelectorAll('.ecc-floatingactionlist button.mdl-button')[0].getBoundingClientRect();
        const navHeaderRect = document.querySelectorAll('.ecc-silk-mapping__navheader div.eccgui-card')[0].getBoundingClientRect();
        const availableSpace = floatButtonRect.top - navHeaderRect.bottom;
        const spaceNeededForMenuList = 200; // This is not known before the menu list is rendered, so we assume at most 4 elements
        toBottom = availableSpace < spaceNeededForMenuList;
    } catch (error) {}
    return toBottom;
};

interface ListActionsProps {
    // Executes when one of the create mapping options are clicked. The type specifies the type of mapping.
    onMappingCreate: (mappingSkeleton: { type: "direct" | "object" }) => any
    // Executes when the 'Paste' option is clicked
    onPaste: () => any
    // Executes when the 'Mapping suggestion' option is clicked
    onShowSuggestions: () => any
    // true if the mapping rules list is still loading
    listLoading: boolean
}

const ListActions = ({ onMappingCreate, onPaste, onShowSuggestions, listLoading }: ListActionsProps) => {
    return listLoading ? null : (
        <FloatingActionList
            fabSize="large"
            fixed
            disabled={true}
            iconName="add"
            openToBottom={openToBottomFn}
            actions={_.concat(
                {
                    icon: 'insert_drive_file',
                    label: 'Add value mapping',
                    handler: () => {
                        onMappingCreate({
                            type: MAPPING_RULE_TYPE_DIRECT,
                        });
                    },
                },
                {
                    icon: 'folder',
                    label: 'Add object mapping',
                    handler: () => {
                        onMappingCreate({
                            type: MAPPING_RULE_TYPE_OBJECT,
                        });
                    },
                },
                (sessionStorage.getItem('copyingData') !== null) ? {
                    icon: 'folder',
                    label: 'Paste mapping',
                    handler: () => onPaste(),
                } : [],
                {
                    icon: 'lightbulb_outline',
                    label: 'Suggest mappings',
                    handler: e => {
                        e.stopPropagation();
                        onShowSuggestions()
                    },
                },
            )}
        />
    )
};

export default ListActions;
