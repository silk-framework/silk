import React from "react";
import styles from './index.module.scss';
import { IPrefixState } from "@ducks/workspace/typings";
import {
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
    OverviewItemActions,
    IconButton,
} from "@wrappers/index";
interface IProps {
    prefix: IPrefixState;

    onRemove();
}

const PrefixRow = ({prefix, onRemove}: IProps) => {
    return (
        <OverviewItem>
            <OverviewItemDescription>
                <OverviewItemLine>
                    <span>{prefix.prefixName}</span>
                </OverviewItemLine>
                <OverviewItemLine small>
                    <span>{prefix.prefixUri}</span>
                </OverviewItemLine>
            </OverviewItemDescription>
            <OverviewItemActions>
                <IconButton
                    name="item-remove"
                    text="Remove prefix"
                    onClick={onRemove}
                />
            </OverviewItemActions>
        </OverviewItem>
    )
};

export default PrefixRow;
