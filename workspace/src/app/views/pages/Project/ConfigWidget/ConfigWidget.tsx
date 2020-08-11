import React, { useEffect, useState } from "react";
import PrefixesDialog from "./PrefixesDialog";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { IPrefixState } from "@ducks/workspace/typings";
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
} from "@wrappers/index";
import { useTranslation } from "react-i18next";

const VISIBLE_COUNT = 5;

export const ConfigurationWidget = () => {
    const dispatch = useDispatch();
    const prefixList = useSelector(workspaceSel.prefixListSelector);

    const [visiblePrefixes, setVisiblePrefixes] = useState<IPrefixState[]>([]);
    const [isOpen, setIsOpen] = useState<boolean>(false);
    const configurationWidget = useSelector(workspaceSel.widgetsSelector).configuration;

    const { isLoading } = configurationWidget;

    useEffect(() => {
        dispatch(workspaceOp.fetchProjectPrefixesAsync());
    }, [workspaceOp]);

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
                                                                  and {moreCount} ${t("common.words.more", "more")}
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
                                        name="item-edit"
                                        text={t("widget.FileWidget.edit", "Edit prefix settings")}
                                    />
                                </OverviewItemActions>
                            </OverviewItem>
                        </OverviewItemList>
                        <PrefixesDialog isOpen={isOpen} onCloseModal={handleClose} />
                    </>
                )}
            </CardContent>
        </Card>
    );
};
