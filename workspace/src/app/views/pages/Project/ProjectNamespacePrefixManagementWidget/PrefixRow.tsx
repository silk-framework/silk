import React from "react";
import { IPrefixDefinition } from "@ducks/workspace/typings";
import {
    IconButton,
    Icon,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDescription,
    OverviewItemLine,
    Tag,
} from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import styles from "./index.module.scss";

interface IProps {
    prefix: IPrefixDefinition;
    ownership: "project" | "workspace";
    rowId?: string;
    rowClassName?: string;
    overridesWorkspacePrefix?: boolean;
    overriddenInProject?: boolean;
    onJumpToProjectPrefix?: () => void;
    onRemove?: () => void;
}

const PrefixRow = ({
    prefix,
    ownership,
    rowId,
    rowClassName,
    overridesWorkspacePrefix = false,
    overriddenInProject = false,
    onJumpToProjectPrefix,
    onRemove,
}: IProps) => {
    const [t] = useTranslation();
    const isWorkspacePrefix = ownership === "workspace";

    return (
        <OverviewItem id={rowId} className={`${styles.prefixRow}${rowClassName ? ` ${rowClassName}` : ""}`}>
            <OverviewItemDescription>
                <OverviewItemLine>
                    <span>
                        {prefix.prefixName}
                        {overridesWorkspacePrefix && (
                            <>
                                {" "}
                                <Tag small emphasis="weaker">
                                    {t("PrefixDialog.overridesWorkspacePrefixBadge", "Overrides workspace prefix")}
                                </Tag>
                            </>
                        )}
                        {overriddenInProject && (
                            <>
                                {" "}
                                <Tag small emphasis="weaker">
                                    {t("PrefixDialog.overriddenInProjectBadge", "Overridden in project")}
                                </Tag>
                            </>
                        )}
                    </span>
                </OverviewItemLine>
                <OverviewItemLine small>
                    <span>{prefix.prefixUri}</span>
                </OverviewItemLine>
            </OverviewItemDescription>
            <OverviewItemActions>
                {isWorkspacePrefix ? (
                    <>
                        {onJumpToProjectPrefix && (
                            <IconButton
                                name="item-viewdetails"
                                text={t("PrefixDialog.showProjectOverride", "Show project override")}
                                onClick={onJumpToProjectPrefix}
                            />
                        )}
                        <Icon
                            name="state-locked"
                            tooltipText={t("PrefixDialog.workspacePrefixReadOnly", "Workspace prefix, read-only here")}
                        />
                    </>
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
