import React from "react";
import { workspaceOp } from "@ducks/workspace";
import { batch, useDispatch, useSelector } from "react-redux";
import { Mousetrap } from "../../../views/shared/HotKeyHandler/HotKeyHandler";
import { SERVE_PATH } from "../../../constants/path";
import { routerOp } from "@ducks/router";
import { commonOp, commonSel } from "@ducks/common";
import { DATA_TYPES } from "../../../constants";
import { uppercaseFirstChar } from "../../../utils/transformers";
import { useTranslation } from "react-i18next";

export const useKeyboardHeaderShortcuts = () => {
    const dispatch = useDispatch();
    const [t] = useTranslation();
    const projectId = useSelector(commonSel.currentProjectIdSelector);

    const focusOnSearchBar = React.useCallback(() => {
        const searchbar = document.querySelector("[data-test-id='search-bar']") as HTMLInputElement;
        if (searchbar) {
            searchbar.focus();
        }
    }, []);

    const handlePageNavigation = React.useCallback(
        (filter: string) => {
            batch(() => {
                if (projectId && filter !== "project") {
                    dispatch(routerOp.goToPage(`${SERVE_PATH}/projects/${projectId}`));
                } else if (projectId && filter === "project") {
                    dispatch(routerOp.goToPage(SERVE_PATH));
                }

                dispatch(
                    workspaceOp.applyFiltersOp({
                        itemType: filter,
                    })
                );
                dispatch(workspaceOp.changePageOp(1));
            });
            focusOnSearchBar();
            return false;
        },
        [projectId]
    );

    const headerShortcuts = [
        {
            hotKey: "g h",
            handler: () => {
                dispatch(routerOp.goToPage(SERVE_PATH));
                return false;
            },
        },
        {
            hotKey: "g p",
            handler: () => handlePageNavigation("project"),
        },
        {
            hotKey: "g w",
            handler: () => handlePageNavigation("workflow"),
        },
        {
            hotKey: "g d",
            handler: () => handlePageNavigation("dataset"),
        },
        {
            hotKey: "g t",
            handler: () => handlePageNavigation("transform"),
        },
        {
            hotKey: "g l",
            handler: () => handlePageNavigation("linking"),
        },
        {
            hotKey: "g o",
            handler: () => handlePageNavigation("task"),
        },

        {
            hotKey: "g a",
            handler: () => {
                dispatch(
                    routerOp.goToPage(`${SERVE_PATH}/activities?page=1&limit=25&sortBy=recentlyUpdated&sortOrder=ASC`)
                );
                focusOnSearchBar();
                return false;
            },
        },
        {
            hotKey: "c p",
            handler: () => {
                dispatch(
                    commonOp.selectArtefact({
                        key: DATA_TYPES.PROJECT,
                        title: uppercaseFirstChar(t("common.dataTypes.project")),
                        description: t(
                            "common.dataTypes.projectDesc",
                            "Projects let you group related items. All items that depend on each other need to be in the same project."
                        ),
                    })
                );
                return false;
            },
        },
        {
            hotKey: "c w",
            handler: () => {
                dispatch(
                    commonOp.createNewTask({
                        selectedDType: "workflow",
                        newTaskPreConfiguration: { taskPluginId: "workflow" },
                    })
                );
                return false;
            },
        },
        {
            hotKey: "c d",
            handler: () => {
                dispatch(
                    commonOp.createNewTask({
                        selectedDType: "dataset",
                        newTaskPreConfiguration: { taskPluginId: "dataset" },
                    })
                );
                return false;
            },
        },
        {
            hotKey: "c t",
            handler: () => {
                dispatch(
                    commonOp.createNewTask({
                        selectedDType: "transform",
                        newTaskPreConfiguration: { taskPluginId: "transform" },
                    })
                );
                return false;
            },
        },
        {
            hotKey: "c l",
            handler: () => {
                dispatch(
                    commonOp.createNewTask({
                        selectedDType: "linking",
                        newTaskPreConfiguration: { taskPluginId: "linking" },
                    })
                );
                return false;
            },
        },
        {
            hotKey: "c o",
            handler: () => {
                dispatch(commonOp.setSelectedArtefactDType("task"));
                return false;
            },
        },
        {
            hotKey: "c n",
            handler: () => {
                dispatch(commonOp.setSelectedArtefactDType("all"));
                return false;
            },
        },
    ];

    React.useEffect(() => {
        //bind shortcuts
        headerShortcuts.forEach((shortcut) => {
            Mousetrap.bind(shortcut.hotKey, shortcut.handler);
        });

        //unbind shortcuts
        return () =>
            headerShortcuts.forEach((shortcut) => {
                Mousetrap.unbind(shortcut.hotKey, shortcut.handler);
            });
    }, [projectId]);
};
