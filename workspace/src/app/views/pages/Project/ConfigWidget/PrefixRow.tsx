import React from "react";
import { IPrefixState } from "@ducks/workspace/typings";
import {
    IconButton,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDescription,
    OverviewItemLine,
} from "@wrappers/index";
import { useTranslation } from "react-i18next";

interface IProps {
    prefix: IPrefixState;

    onRemove();
}

const PrefixRow = ({ prefix, onRemove }: IProps) => {
    const [t] = useTranslation();

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
                    text={t("common.action.deletePrefix", "Delete prefix")}
                    onClick={onRemove}
                />
            </OverviewItemActions>
        </OverviewItem>
    );
};

export default PrefixRow;
