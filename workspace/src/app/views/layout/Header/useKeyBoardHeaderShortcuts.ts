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
import { useInitFrontend } from "../../pages/MappingEditor/api/silkRestApi.hooks";
import { absoluteProjectPath } from "../../../utils/routerUtils";
import { ModalContext } from "@eccenca/gui-elements/src/components/Dialog/ModalContext";

export const useKeyboardHeaderShortcuts = () => {
    const dispatch = useDispatch();
    const [t] = useTranslation();
    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const modalContext = React.useContext(ModalContext)
    const modalIsOpen = React.useRef(false)
    modalIsOpen.current = !!modalContext.openModalStack
    const currentProjectId = React.useRef<string | undefined>(undefined)
    currentProjectId.current = projectId;

    const focusOnSearchBar = React.useCallback(() => {
        const searchbar = document.querySelector("[data-test-id='search-bar']") as HTMLInputElement;
        if (searchbar) {
            searchbar.focus();
        }
    }, []);

    // Allows to disable a handler based on e.g. the modal state
    const onOffHandler = React.useCallback(<T>(handler: () => T): () => T | undefined => () => {
        if(modalIsOpen.current) {
            return
        } else {
            return handler()
        }
    } ,[])

    const handlePageNavigation = React.useCallback(
        (filter: string) => {
            batch(() => {
                if (currentProjectId.current && filter !== "project") {
                    dispatch(routerOp.goToPage(absoluteProjectPath(currentProjectId.current)));
                } else if (currentProjectId.current && filter === "project") {
                    dispatch(routerOp.goToPage(SERVE_PATH));
                }

                dispatch(
                    workspaceOp.applyFiltersOp({
                        itemType: filter,
                    }),
                );
                dispatch(workspaceOp.changePageOp(1));
            });
            focusOnSearchBar();
            return false;
        },
        [],
    );

    const headerShortcuts = React.useMemo(() => [
        {
            hotKey: "g h",
            handler: onOffHandler(() => {
                dispatch(routerOp.goToPage(SERVE_PATH));
                return false;
            }),
        },
        {
            hotKey: "g p",
            handler: onOffHandler(() => handlePageNavigation("project")),
        },
        {
            hotKey: "g w",
            handler: onOffHandler(() => handlePageNavigation("workflow")),
        },
        {
            hotKey: "g d",
            handler: onOffHandler(() => handlePageNavigation("dataset")),
        },
        {
            hotKey: "g t",
            handler: onOffHandler(() => handlePageNavigation("transform")),
        },
        {
            hotKey: "g l",
            handler: onOffHandler(() => handlePageNavigation("linking")),
        },
        {
            hotKey: "g o",
            handler: onOffHandler(() => handlePageNavigation("task")),
        },

        {
            hotKey: "g a",
            handler: onOffHandler(() => {
                dispatch(
                    routerOp.goToPage(`${SERVE_PATH}/activities?page=1&limit=25&sortBy=recentlyUpdated&sortOrder=ASC`),
                );
                focusOnSearchBar();
                return false;
            }),
        },
        {
            hotKey: "c p",
            handler: onOffHandler(() => {
                dispatch(
                    commonOp.selectArtefact({
                        key: DATA_TYPES.PROJECT,
                        title: uppercaseFirstChar(t("common.dataTypes.project")),
                        description: t(
                            "common.dataTypes.projectDesc",
                            "Projects let you group related items. All items that depend on each other need to be in the same project.",
                        ),
                    }),
                );
                return false;
            }),
        },
        {
            hotKey: "c w",
            handler: onOffHandler(() => {
                dispatch(
                    commonOp.createNewTask({
                        selectedDType: "workflow",
                        newTaskPreConfiguration: { taskPluginId: "workflow" },
                    }),
                );
                return false;
            }),
        },
        {
            hotKey: "c d",
            handler: onOffHandler(() => {

                dispatch(
                    commonOp.createNewTask({
                        selectedDType: "dataset",
                        newTaskPreConfiguration: { taskPluginId: "dataset" },
                    }),
                );
                return false;
            }),
        },
        {
            hotKey: "c t",
            handler: onOffHandler(() => {
                dispatch(
                    commonOp.createNewTask({
                        selectedDType: "transform",
                        newTaskPreConfiguration: { taskPluginId: "transform" },
                    }),
                );
                return false;
            }),
        },
        {
            hotKey: "c l",
            handler: onOffHandler(() => {
                dispatch(
                    commonOp.createNewTask({
                        selectedDType: "linking",
                        newTaskPreConfiguration: { taskPluginId: "linking" },
                    }),
                );
                return false;
            }),
        },
        {
            hotKey: "c o",
            handler: onOffHandler(() => {
                dispatch(commonOp.setSelectedArtefactDType("task"));
                return false;
            }),
        },
        {
            hotKey: "c n",
            handler: onOffHandler(() => {
                dispatch(commonOp.setSelectedArtefactDType("all"));
                return false;
            }),
        },
    ], [handlePageNavigation]);

    React.useEffect(() => {
        //bind shortcuts
        headerShortcuts.forEach((shortcut) => {
            Mousetrap.bind(shortcut.hotKey, shortcut.handler);
        });

        //unbind shortcuts
        return () =>
            headerShortcuts.forEach((shortcut) => {
                // FIXME: This is NOT working as expected! It is not possible to remove a specific handler from a hot key sequence
                Mousetrap.unbind(shortcut.hotKey, shortcut.handler);
            })
    }, []);
};
