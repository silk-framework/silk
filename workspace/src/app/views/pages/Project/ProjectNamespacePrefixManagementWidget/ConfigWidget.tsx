import React, { useEffect, useState } from "react";
import PrefixesDialog from "./PrefixesDialog";
import { useSelector } from "react-redux";
import { IPrefixDefinition, IDetailedProjectPrefixes } from "@ducks/workspace/typings";
import Loading from "../../../shared/Loading";

import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    Divider,
    IconButton,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDescription,
    OverviewItemLine,
    OverviewItemList,
} from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { commonSel } from "@ducks/common";
import useHotKey from "../../../../views/shared/HotKeyHandler/HotKeyHandler";
import { requestDetailedProjectPrefixes } from "@ducks/workspace/requests";

const VISIBLE_COUNT = 5;

interface IPrefixLists {
    effectivePrefixes: IPrefixDefinition[];
    projectPrefixes: IPrefixDefinition[];
    workspacePrefixes: IPrefixDefinition[];
}

const emptyPrefixLists: IPrefixLists = {
    effectivePrefixes: [],
    projectPrefixes: [],
    workspacePrefixes: [],
};

const formatPrefixMap = (prefixes: Record<string, string>): IPrefixDefinition[] =>
    Object.keys(prefixes)
        .sort((left, right) => left.localeCompare(right))
        .map((prefixName) => ({
            prefixName,
            prefixUri: prefixes[prefixName],
        }));

const formatDetailedPrefixLists = (prefixes: IDetailedProjectPrefixes): IPrefixLists => ({
    effectivePrefixes: formatPrefixMap({
        ...prefixes.workspacePrefixes,
        ...prefixes.projectPrefixes,
    }),
    projectPrefixes: formatPrefixMap(prefixes.projectPrefixes),
    workspacePrefixes: formatPrefixMap(prefixes.workspacePrefixes),
});

/** The project namespace prefix management widget that allows adding, updating and removing namespace prefixes. */
export const ProjectNamespacePrefixManagementWidget = () => {
    const [prefixLists, setPrefixLists] = useState<IPrefixLists>(emptyPrefixLists);
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [isOpen, setIsOpen] = useState<boolean>(false);
    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const visiblePrefixes = prefixLists.effectivePrefixes.slice(0, VISIBLE_COUNT);

    useHotKey({
        hotkey: "e p",
        handler: () => {
            handleOpen();
            return false;
        },
    });

    const refreshPrefixes = React.useCallback(async (): Promise<IDetailedProjectPrefixes> => {
        if (!projectId) {
            setPrefixLists(emptyPrefixLists);
            return {
                projectPrefixes: {},
                workspacePrefixes: {},
            };
        }
        setIsLoading(true);
        try {
            const { data } = await requestDetailedProjectPrefixes(projectId);
            setPrefixLists(formatDetailedPrefixLists(data));
            return data;
        } finally {
            setIsLoading(false);
        }
    }, [projectId]);

    useEffect(() => {
        if (projectId) {
            void refreshPrefixes();
        } else {
            setPrefixLists(emptyPrefixLists);
        }
    }, [projectId, refreshPrefixes]);

    const getFullSizeOfList = () => prefixLists.effectivePrefixes.length;
    const handleOpen = () => setIsOpen(true);
    const handleClose = () => setIsOpen(false);

    const moreCount = getFullSizeOfList() - VISIBLE_COUNT;
    const [t] = useTranslation();

    return (
        <Card>
            <CardHeader>
                <CardTitle>
                    <h2>{t("widget.ConfigWidget.title", "Configuration")} </h2>
                </CardTitle>
            </CardHeader>
            <Divider />
            <CardContent>
                {isLoading ? (
                    <Loading description={t("widget.ConfigWidget.loading", "Loading configuration list.")} />
                ) : (
                    <>
                        <OverviewItemList hasSpacing hasDivider>
                            <OverviewItem>
                                <OverviewItemDescription>
                                    <OverviewItemLine>
                                        <strong>
                                            {t("widget.ConfigWidget.prefix", { count: 2 })}&nbsp;({getFullSizeOfList()})
                                        </strong>
                                    </OverviewItemLine>
                                    <OverviewItemLine small>
                                        <span>
                                            {visiblePrefixes.map((o, index) => (
                                                <span key={index}>
                                                    {o.prefixName}
                                                    {index < visiblePrefixes.length - 1
                                                        ? ", "
                                                        : moreCount > 0 && (
                                                              <>
                                                                  {" "}
                                                                  {t("common.words.and")} {moreCount}{" "}
                                                                  {t("common.words.more", "more")}
                                                              </>
                                                          )}
                                                </span>
                                            ))}
                                        </span>
                                    </OverviewItemLine>
                                </OverviewItemDescription>
                                <OverviewItemActions>
                                    <IconButton
                                        onClick={handleOpen}
                                        data-test-id={"open-project-prefix-mgmt-btn"}
                                        name="item-edit"
                                        text={t("widget.FileWidget.edit", "Edit prefix settings")}
                                    />
                                </OverviewItemActions>
                            </OverviewItem>
                        </OverviewItemList>
                        {projectId && (
                            <PrefixesDialog
                                projectId={projectId}
                                isOpen={isOpen}
                                onCloseModal={handleClose}
                                projectPrefixes={prefixLists.projectPrefixes}
                                workspacePrefixes={prefixLists.workspacePrefixes}
                                refreshPrefixes={refreshPrefixes}
                            />
                        )}
                    </>
                )}
            </CardContent>
        </Card>
    );
};
