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
    /** Null means this particular conversation is deleted or inaccessible. */
    loadConversation?: (
        conversationId: string,
        signal: AbortSignal,
        apiBasePath?: string,
    ) => Promise<ChangeConversationSummary | null>;
    openConversation?: (conversationId: string) => void;
}

export interface ChangeConversationIdentity {
    number: number;
    title?: string;
}

export const ChangeConversationsContext = React.createContext<ReadonlyMap<string, ChangeConversationIdentity>>(
    new Map(),
);
export const RequestChangeConversationsContext = React.createContext<(ids: readonly string[]) => void>(() => {});
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

    const [requestedIds, setRequestedIds] = React.useState<ReadonlySet<string>>(new Set());
    const attemptedIds = React.useRef(new Set<string>());
    const pendingRequests = React.useRef(new Map<string, AbortController>());
    const recentLookupReady = React.useRef(false);
    const [lookupVersion, setLookupVersion] = React.useState(0);
    const requestMetadata = React.useCallback(
        (ids: readonly string[]) => {
            if (!source || !companion?.enabled) return;
            setRequestedIds((previous) => {
                const missing = ids.filter((id) => id && !previous.has(id));
                return missing.length ? new Set([...previous, ...missing]) : previous;
            });
        },
        [source, companion?.enabled, companion?.apiBasePath, userUri],
    );
    const mergeConversations = React.useCallback((summaries: readonly ChangeConversationSummary[]) => {
        const identified = identifyChangeConversations(summaries, numbers.current);
        setConversations((previous) => new Map([...previous, ...identified]));
    }, []);

    React.useLayoutEffect(() => {
        numbers.current.clear();
        setRequestedIds(new Set());
        setConversations(new Map());
    }, [source, companion?.enabled, companion?.apiBasePath, userUri]);

    React.useLayoutEffect(() => {
        recentLookupReady.current = false;
        attemptedIds.current.clear();
        const pending = pendingRequests.current;
        pending.forEach((controller) => controller.abort());
        pending.clear();
        return () => {
            pending.forEach((controller) => controller.abort());
            pending.clear();
        };
    }, [source, companion?.enabled, companion?.apiBasePath, userUri, revision]);

    React.useEffect(() => {
        if (!source || !companion?.enabled) {
            setConversations(new Map());
            return;
        }
        const controller = new AbortController();
        const load = async () => {
            try {
                const summaries = await source.loadConversations(controller.signal, companion.apiBasePath);
                if (!controller.signal.aborted) {
                    mergeConversations(summaries);
                    recentLookupReady.current = true;
                }
            } catch (error: unknown) {
                // A failed refresh does not invalidate the last successful lookup.
                if (!controller.signal.aborted) {
                    recentLookupReady.current = !(error instanceof ChangeConversationAccessError);
                    if (error instanceof ChangeConversationAccessError) setConversations(new Map());
                }
            } finally {
                if (!controller.signal.aborted) setLookupVersion((value) => value + 1);
            }
        };
        void load();
        return () => {
            controller.abort();
        };
    }, [source, companion?.enabled, companion?.apiBasePath, userUri, revision, mergeConversations]);

    React.useEffect(() => {
        if (!source?.loadConversation || !companion?.enabled || !recentLookupReady.current) return;
        for (const id of requestedIds) {
            if (conversations.has(id) || attemptedIds.current.has(id)) continue;
            attemptedIds.current.add(id);
            const controller = new AbortController();
            pendingRequests.current.set(id, controller);
            void source
                .loadConversation(id, controller.signal, companion.apiBasePath)
                .then((summary) => {
                    if (controller.signal.aborted) return;
                    if (summary) {
                        mergeConversations([summary]);
                    } else {
                        setConversations((previous) => {
                            const next = new Map(previous);
                            next.delete(id);
                            return next;
                        });
                    }
                })
                .catch((error: unknown) => {
                    if (!controller.signal.aborted && error instanceof ChangeConversationAccessError) {
                        recentLookupReady.current = false;
                        pendingRequests.current.forEach((pending) => pending.abort());
                        pendingRequests.current.clear();
                        setConversations(new Map());
                    }
                })
                .finally(() => {
                    if (pendingRequests.current.get(id) === controller) pendingRequests.current.delete(id);
                });
        }
    }, [
        source,
        companion?.enabled,
        companion?.apiBasePath,
        userUri,
        requestedIds,
        conversations,
        lookupVersion,
        mergeConversations,
    ]);

    return (
        <OpenChangeConversationContext.Provider value={companion?.enabled ? source?.openConversation : undefined}>
            <RefreshChangeConversationsContext.Provider value={refresh}>
                <RequestChangeConversationsContext.Provider value={requestMetadata}>
                    <ChangeConversationsContext.Provider value={conversations}>
                        {children}
                    </ChangeConversationsContext.Provider>
                </RequestChangeConversationsContext.Provider>
            </RefreshChangeConversationsContext.Provider>
        </OpenChangeConversationContext.Provider>
    );
};
