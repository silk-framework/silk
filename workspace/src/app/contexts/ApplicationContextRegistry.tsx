import React from "react";

export interface ActiveTabContext {
    id: string;
    kind: "registeredTaskView" | "itemLink";
    label?: string;
}

export interface SearchContext {
    text?: string;
    itemType?: string;
    facets?: Array<{
        id: string;
        label?: string;
        values: Array<{ id: string; label?: string }>;
    }>;
}

interface SearchFilters {
    textQuery: string;
    itemType?: string;
}

interface AppliedSearchFacet {
    facetId: string;
    keywordIds: string[];
}

interface AvailableSearchFacet {
    id: string;
    label: string;
    values: Array<{ id: string; label: string }>;
}

export const createSearchContext = (
    filters: SearchFilters,
    appliedFacets: readonly AppliedSearchFacet[],
    availableFacets: readonly AvailableSearchFacet[],
): SearchContext => {
    const facets = appliedFacets.flatMap((appliedFacet) => {
        const availableFacet = availableFacets.find((facet) => facet.id === appliedFacet.facetId);
        if (!availableFacet) {
            return [];
        }
        const values = appliedFacet.keywordIds.flatMap((keywordId) => {
            const value = availableFacet.values.find((candidate) => candidate.id === keywordId);
            return value ? [{ id: value.id, label: value.label }] : [];
        });
        return values.length ? [{ id: availableFacet.id, label: availableFacet.label, values }] : [];
    });
    const text = filters.textQuery.trim();
    return {
        ...(text ? { text } : {}),
        ...(filters.itemType ? { itemType: filters.itemType } : {}),
        ...(facets.length ? { facets } : {}),
    };
};

export type PageContextContribution =
    | {
          kind: "search";
          scope: { view: "workspace" } | { view: "project"; projectId: string };
          search: SearchContext;
      }
    | {
          kind: "activeTaskView";
          scope: { projectId: string; taskId: string };
          pluginId?: string;
          activeTab: ActiveTabContext;
      }
    | {
          kind: "deprecatedPlugins";
          scope: { view: "deprecatedPlugins" };
          totalUsageCount: number;
          selectedPlugin?: {
              id: string;
              label: string;
              usageCount: number;
              deprecationMessage?: string;
          };
      };

export type ForegroundViewContext =
    | {
          kind: "taskView";
          projectId: string;
          projectLabel?: string;
          taskId: string;
          taskType: string;
          taskLabel?: string;
          pluginId?: string;
          activeTab?: ActiveTabContext;
      }
    | {
          kind: "artefactForm";
          operation: "create" | "update";
          artefactKind?: "project" | "task";
          projectId?: string;
          projectLabel?: string;
          taskId?: string;
          taskType?: string;
          taskLabel?: string;
          pluginId?: string;
          pluginLabel?: string;
      };

interface Registration<T> {
    update(value: T): void;
    unregister(): void;
}

interface RegisteredValue<T> {
    token: number;
    value: T;
}

interface ApplicationContextRegistryState {
    pageContributions: readonly PageContextContribution[];
    foregroundViews: readonly ForegroundViewContext[];
}

interface ApplicationContextRegistryActions {
    registerPageContribution(value: PageContextContribution): Registration<PageContextContribution>;
    registerForegroundView(value: ForegroundViewContext): Registration<ForegroundViewContext>;
}

type ApplicationContextRegistryValue = ApplicationContextRegistryState & ApplicationContextRegistryActions;

const noRegistration = <T,>(): Registration<T> => ({ update: () => {}, unregister: () => {} });

const ApplicationContextRegistryStateContext = React.createContext<ApplicationContextRegistryState>({
    foregroundViews: [],
    pageContributions: [],
});
const ApplicationContextRegistryActionsContext = React.createContext<ApplicationContextRegistryActions>({
    registerForegroundView: noRegistration,
    registerPageContribution: noRegistration,
});

export const ApplicationContextRegistryProvider = ({ children }: { children: React.ReactNode }) => {
    const nextToken = React.useRef(0);
    const [pageEntries, setPageEntries] = React.useState<Array<RegisteredValue<PageContextContribution>>>([]);
    const [foregroundEntries, setForegroundEntries] = React.useState<
        Array<RegisteredValue<ForegroundViewContext>>
    >([]);

    const createRegistration = React.useCallback(
        <T,>(value: T, setEntries: React.Dispatch<React.SetStateAction<Array<RegisteredValue<T>>>>): Registration<T> => {
            const token = ++nextToken.current;
            let registered = true;
            setEntries((entries) => [...entries, { token, value }]);
            return {
                update(nextValue) {
                    if (registered) {
                        setEntries((entries) =>
                            entries.map((entry) => (entry.token === token ? { ...entry, value: nextValue } : entry)),
                        );
                    }
                },
                unregister() {
                    if (registered) {
                        registered = false;
                        setEntries((entries) => entries.filter((entry) => entry.token !== token));
                    }
                },
            };
        },
        [],
    );

    const registerPageContribution = React.useCallback(
        (value: PageContextContribution) => createRegistration(value, setPageEntries),
        [createRegistration],
    );
    const registerForegroundView = React.useCallback(
        (value: ForegroundViewContext) => createRegistration(value, setForegroundEntries),
        [createRegistration],
    );
    const state = React.useMemo<ApplicationContextRegistryState>(
        () => ({
            foregroundViews: foregroundEntries.map((entry) => entry.value),
            pageContributions: pageEntries.map((entry) => entry.value),
        }),
        [foregroundEntries, pageEntries],
    );
    const actions = React.useMemo<ApplicationContextRegistryActions>(
        () => ({
            registerForegroundView,
            registerPageContribution,
        }),
        [registerForegroundView, registerPageContribution],
    );

    return (
        <ApplicationContextRegistryActionsContext.Provider value={actions}>
            <ApplicationContextRegistryStateContext.Provider value={state}>
                {children}
            </ApplicationContextRegistryStateContext.Provider>
        </ApplicationContextRegistryActionsContext.Provider>
    );
};

export const useApplicationContextRegistry = (): ApplicationContextRegistryValue => {
    const state = React.useContext(ApplicationContextRegistryStateContext);
    const actions = React.useContext(ApplicationContextRegistryActionsContext);
    return React.useMemo(() => ({ ...state, ...actions }), [actions, state]);
};

const useRegistration = <T,>(
    value: T | undefined,
    register: (value: T) => Registration<T>,
): void => {
    const registration = React.useRef<Registration<T>>();

    React.useEffect(() => {
        if (value) {
            if (registration.current) {
                registration.current.update(value);
            } else {
                registration.current = register(value);
            }
        } else {
            registration.current?.unregister();
            registration.current = undefined;
        }
    }, [register, value]);

    React.useEffect(
        () => () => {
            registration.current?.unregister();
            registration.current = undefined;
        },
        [],
    );
};

export const usePageContextContribution = (value: PageContextContribution | undefined): void => {
    const { registerPageContribution } = React.useContext(ApplicationContextRegistryActionsContext);
    useRegistration(value, registerPageContribution);
};

export const useForegroundViewContext = (value: ForegroundViewContext | undefined): void => {
    const { registerForegroundView } = React.useContext(ApplicationContextRegistryActionsContext);
    useRegistration(value, registerForegroundView);
};
