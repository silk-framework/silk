import React from "react";
import { Button, Icon, Tag, Tooltip } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { useLocation } from "react-router";
import { HIGHLIGHT_CONVERSATION_PARAM } from "../../../constants/queryParams";
import { ChangeConversationsContext } from "./ChangeConversationsContext";
import { IChangeEntry } from "./changesRequests";

export const CONVERSATION_FACET = "changeConversation";

const SourceTooltip = ({
    content,
    children,
}: {
    content: string;
    children: React.ReactElement<{ "aria-describedby"?: string }>;
}) => {
    const id = React.useId();
    return (
        <Tooltip
            content={
                <span id={id} role="tooltip" style={{ whiteSpace: "pre-wrap" }}>
                    {content}
                </span>
            }
            usePlaceholder={false}
        >
            {React.cloneElement(children, { "aria-describedby": id })}
        </Tooltip>
    );
};

const useConversationLabel = (id: string) => {
    const [t] = useTranslation();
    const conversation = React.useContext(ChangeConversationsContext).get(id);
    const location = useLocation();
    const highlighted = new URLSearchParams(location.search).get(HIGHLIGHT_CONVERSATION_PARAM) === id;
    const label = conversation
        ? t("pages.changes.conversation.badge", { number: conversation.number })
        : t("pages.changes.conversation.unavailableBadge");
    const conversationLabel =
        conversation?.title ||
        (conversation ? t("pages.changes.conversation.untitled") : t("pages.changes.conversation.unavailable"));
    return {
        label,
        highlightClass: highlighted ? "diapp-changes__conversation--highlighted" : undefined,
        tooltip: `${t("pages.changes.conversation.label", { label: conversationLabel })}\n\n${t("pages.changes.conversation.id", { id })}`,
    };
};

/** Facet labels and row badges share the same temporary identity, but never use it as a filter value. */
export const ChangeConversationLabel = ({ conversationId }: { conversationId: string }) => {
    const { label, tooltip, highlightClass } = useConversationLabel(conversationId);
    return (
        <SourceTooltip content={tooltip}>
            <span tabIndex={0}>
                <Tag className={highlightClass} aria-current={highlightClass ? true : undefined}>
                    {label}
                </Tag>
            </span>
        </SourceTooltip>
    );
};

const ConversationSource = ({
    entry,
    selected,
    onSelect,
}: {
    entry: IChangeEntry & { conversationId: string };
    selected?: boolean;
    onSelect?: (id: string) => void;
}) => {
    const [t] = useTranslation();
    const conversation = useConversationLabel(entry.conversationId);
    return (
        <SourceTooltip content={conversation.tooltip}>
            <Button
                data-test-id={`change-conversation-${entry.seq}`}
                small
                minimal
                active={selected}
                className={conversation.highlightClass}
                aria-current={conversation.highlightClass ? true : undefined}
                aria-pressed={selected ?? false}
                aria-label={t("pages.changes.conversation.filter", { label: conversation.label })}
                icon={<Icon name="operation-ai-generate" small aria-hidden="true" />}
                text={conversation.label}
                onClick={() => onSelect?.(entry.conversationId)}
            />
        </SourceTooltip>
    );
};

export const ChangeSource = ({
    entry,
    selected,
    onSelect,
}: {
    entry: IChangeEntry;
    selected?: boolean;
    onSelect?: (id: string) => void;
}) => {
    const [t] = useTranslation();
    if (!entry.origin) {
        return null;
    }
    if (entry.conversationId) {
        return (
            <ConversationSource
                entry={{ ...entry, conversationId: entry.conversationId }}
                selected={selected}
                onSelect={onSelect}
            />
        );
    }
    return (
        <SourceTooltip content={`${t("pages.changes.originTooltip")}: ${entry.origin}`}>
            <span
                data-test-id={`change-agent-${entry.seq}`}
                role="img"
                aria-label={t("pages.changes.agentSource")}
                tabIndex={0}
            >
                <Icon name="operation-ai-generate" small aria-hidden="true" />
            </span>
        </SourceTooltip>
    );
};
