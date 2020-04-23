import React from "react";
import { IPrefixState } from "@ducks/workspace/typings";
import {
    IconButton,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDescription,
    OverviewItemLine,
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
