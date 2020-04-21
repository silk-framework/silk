import React from 'react';
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import {
    TitleSubsection,
    Menu,
    MenuItem,
} from "@wrappers/index";

function ArtefactTypesList({ onSelect }) {
    const {selectedDType} = useSelector(commonSel.artefactModalSelector);
    const typeModifier = useSelector(commonSel.availableDTypesSelector).type;

    return <>
        <TitleSubsection>Artefact Type</TitleSubsection>
        <Menu>
            <MenuItem
                text={'All'}
                key='all'
                onClick={() => onSelect('all')}
                active={selectedDType === 'all' ? true : false}
            />
            {
                typeModifier.options.map(type =>
                    <MenuItem
                        text={type.label}
                        key={type.id}
                        onClick={() => onSelect(type.id)}
                        active={selectedDType === type.id ? true : false}
                    />
                )
            }
        </Menu>
    </>
}

export default ArtefactTypesList;
