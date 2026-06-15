import React from "react";
import { IPrefixDefinition } from "@ducks/workspace/typings";
import {
    IconButton,
    Icon,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDescription,
    OverviewItemLine,
} from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";

interface IProps {
    prefix: IPrefixDefinition;
    ownership: "project" | "workspace";
    onRemove?: () => void;
}

const PrefixRow = ({ prefix, ownership, onRemove }: IProps) => {
    const [t] = useTranslation();
    const isWorkspacePrefix = ownership === "workspace";

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
                {isWorkspacePrefix ? (
                    <Icon
                        name="state-locked"
                        tooltipText={t("PrefixDialog.workspacePrefixReadOnly", "Workspace prefix, read-only here")}
                    />
                ) : (
                    onRemove && (
                        <IconButton
                            name="item-remove"
                            text={t("common.action.DeleteSmth", { smth: t("widget.ConfigWidget.prefix") })}
                            onClick={onRemove}
                        />
                    )
                )}
            </OverviewItemActions>
        </OverviewItem>
    );
};

export default PrefixRow;
