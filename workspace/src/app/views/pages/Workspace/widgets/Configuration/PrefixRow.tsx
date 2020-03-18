import React from "react";
import styles from './index.module.scss';
import { IPrefixState } from "@ducks/workspace/typings";
import {
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
    OverviewItemActions,
    Icon,
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
                <OverviewItemLine>
                    <span>{prefix.prefixUri}</span>
                </OverviewItemLine>
            </OverviewItemDescription>
            <OverviewItemActions>
                <Icon
                    name="item-remove"
                    onClick={onRemove}
                />
            </OverviewItemActions>
        </OverviewItem>
    )
};

export default PrefixRow;
