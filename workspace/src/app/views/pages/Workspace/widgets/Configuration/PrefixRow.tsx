import { IconNames } from "@wrappers/blueprint/constants";
import React from "react";
import Icon from "@wrappers/blueprint/icon";
import styles from './index.module.scss';
import { IPrefixState } from "@ducks/workspace/typings";
import DataList from "../../../../components/Datalist";

const {Cell, ListRow} = DataList;

interface IProps {
    prefix: IPrefixState;

    onRemove();
}

const PrefixRow = ({prefix, onRemove}: IProps) => {
    return (
        <ListRow className={`${styles.prefixRow}`}>
            <Cell span={7}>
                <span>{prefix.prefixName}</span>
            </Cell>
            <Cell span={8}>
                <span>{prefix.prefixUri}</span>
            </Cell>
            <Cell span={1}>
                <Icon
                    icon={IconNames.TRASH}
                    onClick={onRemove}
                />
            </Cell>
        </ListRow>
    )
};

export default PrefixRow;
