import React from "react";
import { Button, Icon, Tag } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { ChangeConversationsContext } from "./ChangeConversationsContext";
import { IChangeEntry } from "./changesRequests";

export const CONVERSATION_FACET = "changeConversation";

const useConversationLabel = (id: string) => {
    const [t] = useTranslation();
    const conversation = React.useContext(ChangeConversationsContext).get(id);
    const label = conversation
        ? t("pages.changes.conversation.badge", { number: conversation.number })
        : t("pages.changes.conversation.unavailableBadge");
    const fullTitle =
        conversation?.title ||
        (conversation ? t("pages.changes.conversation.untitled") : t("pages.changes.conversation.unavailable"));
    const title = fullTitle.length > 80 ? `${fullTitle.slice(0, 80)}…` : fullTitle;
    return { label, title: `${title}\n${t("pages.changes.conversation.id", { id })}` };
};

/** Facet labels and row badges share the same temporary identity, but never use it as a filter value. */
export const ChangeConversationLabel = ({ conversationId }: { conversationId: string }) => {
    const { label, title } = useConversationLabel(conversationId);
    return (
        <Tag small htmlTitle={`${label}: ${title}`}>
            {label}
        </Tag>
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
    const title = `${conversation.title}\n${t("pages.changes.originTooltip")}: ${entry.origin}`;
    return (
        <span data-test-id={`change-agent-${entry.seq}`} title={title}>
            <Button
                data-test-id={`change-conversation-${entry.seq}`}
                small
                minimal
                active={selected}
                aria-pressed={selected ?? false}
                aria-label={t("pages.changes.conversation.filter", { label: conversation.label })}
                icon="operation-ai-generate"
                text={conversation.label}
                onClick={() => onSelect?.(entry.conversationId)}
                tooltip={title}
            />
        </span>
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
        <span data-test-id={`change-agent-${entry.seq}`} title={`${t("pages.changes.originTooltip")}: ${entry.origin}`}>
            <Icon name="operation-ai-generate" small />
        </span>
    );
};
