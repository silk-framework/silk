import React from "react";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { pluginRegistry, SUPPORTED_PLUGINS } from "../../plugins/PluginRegistry";
import { ChangeConversationAccessError } from "./ChangeConversationAccessError";

export interface ChangeConversationSummary {
    id: string;
    label?: string | null;
    summary?: string | null;
}

/** An optional host supplies only the conversations visible to the logged-in user. */
export interface ChangeConversationSource {
    loadConversations: (signal: AbortSignal, apiBasePath?: string) => Promise<readonly ChangeConversationSummary[]>;
    openConversation?: (conversationId: string) => void;
}

export interface ChangeConversationIdentity {
    number: number;
    title?: string;
}

export const ChangeConversationsContext = React.createContext<ReadonlyMap<string, ChangeConversationIdentity>>(
    new Map(),
);
export const RefreshChangeConversationsContext = React.createContext<() => void>(() => {});
export const OpenChangeConversationContext = React.createContext<((id: string) => void) | undefined>(undefined);

/** Page-local identities survive list reordering and deletion; removed conversations leave a gap. */
export function identifyChangeConversations(
    conversations: readonly ChangeConversationSummary[],
    numbers: Map<string, number>,
): ReadonlyMap<string, ChangeConversationIdentity> {
    return new Map(
        conversations.map((conversation) => {
            let number = numbers.get(conversation.id);
            if (number === undefined) {
                number = numbers.size + 1;
                numbers.set(conversation.id, number);
            }
            return [
                conversation.id,
                {
                    number,
                    title: conversation.label?.trim() || conversation.summary?.trim() || undefined,
                },
            ];
        }),
    );
}

/** Mounted for one journal page, independently of its search, filters and pagination. */
export const ChangeConversationsProvider = ({ children }: { children: React.ReactNode }) => {
    const { companion, userUri } = useSelector(commonSel.initialSettingsSelector);
    const source = pluginRegistry.pluginComponent<ChangeConversationSource>(SUPPORTED_PLUGINS.DI_CHANGE_CONVERSATIONS);
    const numbers = React.useRef(new Map<string, number>());
    const [revision, setRevision] = React.useState(0);
    const refresh = React.useCallback(() => setRevision((value) => value + 1), []);
    const [conversations, setConversations] = React.useState<ReadonlyMap<string, ChangeConversationIdentity>>(
        new Map(),
    );

    React.useLayoutEffect(() => {
        numbers.current.clear();
        setConversations(new Map());
    }, [source, companion?.enabled, companion?.apiBasePath, userUri]);

    React.useEffect(() => {
        if (!source || !companion?.enabled) {
            setConversations(new Map());
            return;
        }
        let controller: AbortController | undefined;
        const load = async () => {
            controller?.abort();
            const current = new AbortController();
            controller = current;
            try {
                const summaries = await source.loadConversations(current.signal, companion.apiBasePath);
                if (!current.signal.aborted) {
                    setConversations(identifyChangeConversations(summaries, numbers.current));
                }
            } catch (error: unknown) {
                // A failed refresh does not invalidate the last successful lookup.
                if (!current.signal.aborted && error instanceof ChangeConversationAccessError) {
                    setConversations(new Map());
                }
            }
        };
        const onFocus = () => {
            void load();
        };
        void load();
        window.addEventListener("focus", onFocus);
        return () => {
            controller?.abort();
            window.removeEventListener("focus", onFocus);
        };
    }, [source, companion?.enabled, companion?.apiBasePath, userUri, revision]);

    return (
        <OpenChangeConversationContext.Provider value={companion?.enabled ? source?.openConversation : undefined}>
            <RefreshChangeConversationsContext.Provider value={refresh}>
                <ChangeConversationsContext.Provider value={conversations}>
                    {children}
                </ChangeConversationsContext.Provider>
            </RefreshChangeConversationsContext.Provider>
        </OpenChangeConversationContext.Provider>
    );
};
