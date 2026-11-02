import React from "react";
import { IconButton } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { useLocation } from "react-router";
import { ChangeConversationsContext, OpenChangeConversationContext } from "./ChangeConversationsContext";
import { IChangeEntry } from "./changesRequests";

type ConversationClick = Pick<
    React.MouseEvent<HTMLElement>,
    "button" | "altKey" | "ctrlKey" | "metaKey" | "shiftKey" | "preventDefault"
>;

/** Live hosts select directly; ordinary links remain available. */
export const openSelectedConversation = (
    event: ConversationClick,
    location: Pick<Location, "search" | "reload">,
    conversationId: string,
    openConversation?: (id: string) => void,
): void => {
    if (event.button === 0 && !event.altKey && !event.ctrlKey && !event.metaKey && !event.shiftKey) {
        if (openConversation) {
            event.preventDefault();
            openConversation(conversationId);
        } else if (new URLSearchParams(location.search).get("conversationId") === conversationId) {
            event.preventDefault();
            location.reload();
        }
    }
};

/** Links also support hosts without live selection. */
export const ChangeConversationAction = ({ entry }: { entry: IChangeEntry }) => {
    const [t] = useTranslation();
    const location = useLocation();
    const conversations = React.useContext(ChangeConversationsContext);
    const openConversation = React.useContext(OpenChangeConversationContext);
    const conversationId = entry.conversationId;
    if (!conversationId || !conversations.has(conversationId)) {
        return null;
    }
    const params = new URLSearchParams(location.search);
    params.set("conversationId", conversationId);
    return (
        <IconButton
            data-test-id={`change-open-conversation-${entry.seq}`}
            name="entity-robot"
            small
            text={t("pages.changes.conversation.open")}
            aria-label={t("pages.changes.conversation.open")}
            href={`?${params.toString()}${location.hash}`}
            onClick={(event) => openSelectedConversation(event, window.location, conversationId, openConversation)}
        />
    );
};
