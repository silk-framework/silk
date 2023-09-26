import React from "react";
import { workspaceOp } from "@ducks/workspace";
import { useDispatch, useSelector } from "react-redux";
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
    const { artefactsList } = useSelector(commonSel.artefactModalSelector);

    const focusOnSearchBar = React.useCallback(() => {
        const searchbar = document.querySelector("[data-test-id='search-bar']") as HTMLInputElement;
        if (searchbar) {
            searchbar.focus();
        }
    }, []);

    const handlePageNavigation = React.useCallback((filter: string) => {
        dispatch(
            workspaceOp.applyFiltersOp({
                itemType: filter,
            })
        );
        dispatch(workspaceOp.changePageOp(1));
        focusOnSearchBar();
        return false;
    }, []);

    const headerShortcuts = [
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
                dispatch(commonOp.selectArtefact(artefactsList.find((a) => a.key === DATA_TYPES.WORKFLOW)));
                return false;
            },
        },
        {
            hotKey: "c d",
            handler: () => {
                dispatch(commonOp.selectArtefact(artefactsList.find((a) => a.key === DATA_TYPES.DATASET)));
                return false;
            },
        },
        {
            hotKey: "c t",
            handler: () => {
                dispatch(commonOp.selectArtefact(artefactsList.find((a) => a.key === DATA_TYPES.TRANSFORM)));
                return false;
            },
        },
        {
            hotKey: "c l",
            handler: () => {
                dispatch(commonOp.selectArtefact(artefactsList.find((a) => a.key === "linking")));
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
        dispatch(
            commonOp.fetchArtefactsListAsync({
                textQuery: "",
            })
        );
    }, []);

    React.useEffect(() => {
        if (artefactsList.length) {
            //bind shortcuts
            headerShortcuts.forEach((shortcut) => {
                Mousetrap.bind(shortcut.hotKey, shortcut.handler);
            });

            //unbind shortcuts
            return () =>
                headerShortcuts.forEach((shortcut) => {
                    Mousetrap.unbind(shortcut.hotKey, shortcut.handler);
                });
        }
    }, [artefactsList]);
};
