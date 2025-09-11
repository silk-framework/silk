import React, { useEffect, useState } from "react";
import PrefixesDialog from "./PrefixesDialog";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { IPrefixDefinition } from "@ducks/workspace/typings";
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
import { AppDispatch } from "store/configureStore";

const VISIBLE_COUNT = 5;

/** The project namespace prefix management widget that allows adding, updating and removing namespace prefixes. */
export const ProjectNamespacePrefixManagementWidget = () => {
    const dispatch = useDispatch<AppDispatch>();
    const prefixList = useSelector(workspaceSel.prefixListSelector);

    const [visiblePrefixes, setVisiblePrefixes] = useState<IPrefixDefinition[]>([]);
    const [isOpen, setIsOpen] = useState<boolean>(false);
    const configurationWidget = useSelector(workspaceSel.widgetsSelector).configuration;
    const projectId = useSelector(commonSel.currentProjectIdSelector);

    const { isLoading } = configurationWidget;

    useHotKey({
        hotkey: "e p",
        handler: () => {
            handleOpen();
            return false;
        },
    });

    useEffect(() => {
        if (projectId) {
            dispatch(workspaceOp.fetchProjectPrefixesAsync(projectId));
        }
    }, [workspaceOp, projectId]);

    useEffect(() => {
        const visibleItems = prefixList.slice(0, VISIBLE_COUNT);
        setVisiblePrefixes(visibleItems);
    }, [prefixList]);

    const getFullSizeOfList = () => Object.keys(prefixList).length;
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
                                existingPrefixes={new Set(prefixList.map((p) => p.prefixName))}
                            />
                        )}
                    </>
                )}
            </CardContent>
        </Card>
    );
};
