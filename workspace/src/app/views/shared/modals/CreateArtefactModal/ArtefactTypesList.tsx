import React from 'react';
import { useSelector } from "react-redux";
import { globalSel } from "@ducks/common";
import {
    TitleSubsection,
    Menu,
    MenuItem,
} from "@wrappers/index";

function ArtefactTypesList({ onSelect }) {
    const {selectedDType} = useSelector(globalSel.artefactModalSelector);
    const typeModifier = useSelector(globalSel.availableDTypesSelector).type;

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
