import React from "react";
import { useTranslation } from "react-i18next";
import {
    ContentBlobToggler,
    ContextMenu,
    ElapsedDateTimeDisplay,
    ElapsedDateTimeDisplayUnits,
    Icon,
    IconButton,
    MenuItem,
    NotAvailable,
    Spacing,
    TableCell,
    TableRow,
    Tag,
    TagList,
} from "@eccenca/gui-elements";
import { getDateData } from "../../shared/Metadata/Metadata";
import { IChangeDetail, IChangeEntry } from "./changesRequests";
import { changeKind, hasInverse, kindIntent, linkIcon, userDisplayName } from "./changeListUtils";

interface IProps {
    entry: IChangeEntry;
    /** Why the entry cannot be reverted as the project is now, if the server said so. */
    conflict?: string;
    /** The entries to revert to return to the state before the entry; left out for the latest entry, which has none. */
    revertBackEntries?: IChangeEntry[];
    /** Asks to revert the entry alone. */
    onRevert: () => void;
    /** Asks to revert back to before the entry. */
    onRevertBack: () => void;
}

/** How many detail lines an entry shows before the rest is behind a 'more' link, as the tag list does it. */
const DETAILS_PREVIEW_LIMIT = 6;

/** A change as a row of the change list: when and by whom, what changed, its links and its revert actions. */
const ChangeRow = ({ entry, conflict, revertBackEntries, onRevert, onRevertBack }: IProps) => {
    const [t] = useTranslation();

    /** A value of a detail: the previous one marked as gone, the new one as current; an empty value spelled out. */
    const detailValue = (text: string, intent: "danger" | "success"): React.ReactNode =>
        text === "" ? (
            <NotAvailable label={t("pages.changes.emptyValue")} tooltip={t("pages.changes.emptyValueTooltip")} />
        ) : (
            <Tag small emphasis="weak" intent={intent}>
                {text}
            </Tag>
        );

    /** A detail as one line: the label, then before and after, one of them for an addition or removal, nothing when the label says it all. */
    const detailLine = (detail: IChangeDetail): React.ReactNode => {
        if (detail.before != null && detail.after != null) {
            return (
                <>
                    {detail.label}: {detailValue(detail.before, "danger")} → {detailValue(detail.after, "success")}
                </>
            );
        } else if (detail.after != null) {
            return (
                <>
                    {detail.label}: {detailValue(detail.after, "success")} {t("pages.changes.detailAdded")}
                </>
            );
        } else if (detail.before != null) {
            return (
                <>
                    {detail.label}: {detailValue(detail.before, "danger")} {t("pages.changes.detailRemoved")}
                </>
            );
        } else {
            return detail.label;
        }
    };

    /** The details of the entry, one per line, cut to the first `limit` when given. */
    const detailLines = (limit: number = entry.details.length): React.ReactNode =>
        entry.details.slice(0, limit).map((detail, index) => (
            <div key={index} data-test-id={`change-detail-${entry.seq}-${index}`}>
                <small>{detailLine(detail)}</small>
            </div>
        ));

    const translateUnits = (unit: ElapsedDateTimeDisplayUnits) => t("common.units." + unit, unit);

    /** Relative within the last week, the date beyond, as the metadata panel shows it; the exact time on hover. */
    const timestamp = (isoDate: string): React.ReactNode => {
        const days = (Date.now() - new Date(isoDate).getTime()) / 1000 / 60 / 60 / 24;
        return days < 7 ? (
            <ElapsedDateTimeDisplay
                dateTime={isoDate}
                prefix={t("Metadata.prefixAgo")}
                suffix={t("Metadata.suffixAgo")}
                translateUnits={translateUnits}
            />
        ) : (
            <span title={new Date(isoDate).toLocaleString()}>{t("Metadata.dateFormat", getDateData(isoDate))}</span>
        );
    };

    /** Why the entry cannot be reverted, if it cannot. */
    const revertBlocker = (): string | undefined => {
        if (entry.revertedBy != null) {
            return t("pages.changes.revert.alreadyReverted", { seq: entry.revertedBy });
        } else if (entry.fulfilledBy != null) {
            return t("pages.changes.revert.fulfilled", { seq: entry.fulfilledBy });
        } else if (!entry.revertible) {
            return t("pages.changes.revert.notRevertible");
        } else if (conflict != null) {
            return t("pages.changes.revert.conflict", { reason: conflict });
        } else {
            return undefined;
        }
    };

    /** The revert actions of the entry, behind a menu so that none is hit by accident: the entry alone, or back to before it. */
    const revertMenu = (): React.ReactNode => {
        const items = [
            <MenuItem
                key="revert"
                data-test-id={`change-revert-btn-${entry.seq}`}
                icon="operation-undo"
                intent="danger"
                text={t("pages.changes.revert.action")}
                htmlTitle={revertBlocker()}
                disabled={!hasInverse(entry) || conflict != null}
                onClick={onRevert}
            />,
        ];
        if (revertBackEntries) {
            items.push(
                <MenuItem
                    key="revertBack"
                    data-test-id={`change-revert-back-btn-${entry.seq}`}
                    icon="operation-undo"
                    intent="danger"
                    text={t("pages.changes.revertBack.action")}
                    disabled={!revertBackEntries.some(hasInverse)}
                    onClick={onRevertBack}
                />,
            );
        }
        return (
            <ContextMenu
                data-test-id={`change-menu-${entry.seq}`}
                togglerText={t("common.action.moreOptions", "Show more options")}
                togglerSize="small"
            >
                {items}
            </ContextMenu>
        );
    };

    return (
        <TableRow className={entry.unreviewed ? "diapp-changes__row--unreviewed" : undefined}>
            <TableCell alignVertical="middle">{entry.seq}</TableCell>
            <TableCell alignVertical="middle">{timestamp(entry.timestamp)}</TableCell>
            <TableCell alignVertical="middle">
                {entry.user && <span title={entry.user}>{userDisplayName(entry.user)}</span>}
                {entry.origin && (
                    <span
                        data-test-id={`change-agent-${entry.seq}`}
                        title={`${t("pages.changes.originTooltip")}: ${entry.origin}`}
                    >
                        {" "}
                        <Icon name="operation-ai-generate" small />
                    </span>
                )}
            </TableCell>
            <TableCell alignVertical="middle">
                <div title={entry.type}>{entry.summary}</div>
                {entry.details.length <= DETAILS_PREVIEW_LIMIT ? (
                    detailLines()
                ) : (
                    <ContentBlobToggler
                        data-test-id={`change-details-toggler-${entry.seq}`}
                        previewContent={detailLines(DETAILS_PREVIEW_LIMIT)}
                        fullviewContent={detailLines()}
                        toggleExtendText={t("common.words.more", "more")}
                        toggleReduceText={t("common.words.less", "less")}
                    />
                )}
                <Spacing size="tiny" />
                <div>
                    <TagList>
                        <Tag small intent={kindIntent[changeKind(entry.type)]} htmlTitle={entry.type}>
                            {t(`pages.changes.kind.${changeKind(entry.type)}`)}
                        </Tag>
                        {entry.unreviewed && (
                            <Tag small intent="warning" htmlTitle={t("pages.changes.unreviewedTooltip")}>
                                {t("pages.changes.unreviewed")}
                            </Tag>
                        )}
                        {entry.reverts != null && (
                            <Tag small>{t("pages.changes.revertsTag", { seq: entry.reverts })}</Tag>
                        )}
                        {entry.revertedBy != null && (
                            <Tag small>{t("pages.changes.revertedByTag", { seq: entry.revertedBy })}</Tag>
                        )}
                        {entry.fulfilledBy != null && (
                            <Tag small>{t("pages.changes.fulfilledTag", { seq: entry.fulfilledBy })}</Tag>
                        )}
                    </TagList>
                </div>
            </TableCell>
            <TableCell alignVertical="middle">
                {/* Every link opens in a new tab, so the review keeps its place */}
                {entry.links.map((link) => (
                    <IconButton
                        key={link.id}
                        data-test-id={`change-link-${entry.seq}-${link.id}`}
                        name={linkIcon(link.id)}
                        small
                        text={link.label}
                        href={link.path}
                        target="_blank"
                        rel="noopener noreferrer"
                    />
                ))}
                {revertMenu()}
            </TableCell>
        </TableRow>
    );
};

export default ChangeRow;
