import React from "react";
import { Button, ContextMenu, IconButton, MenuItem, OverflowText } from "@eccenca/gui-elements";
import { ValidIconName } from "@eccenca/gui-elements/src/components/Icon/canonicalIconNames";

interface IActionBasicProps {
    text: string;
    icon?: ValidIconName;
    affirmative?: boolean;
    disruptive?: boolean;
    disabled?: boolean;
    "data-test-id"?: string;
}

export interface IActionButtonItemProps extends IActionBasicProps {
    actionHandler: (event: React.MouseEvent<HTMLElement>) => void;
    loading?: boolean;
}

interface IActionsMenuActionItemProps extends IActionBasicProps {
    actionHandler: (event: React.MouseEvent<HTMLElement>) => void;
}

interface IActionsMenuParentItemProps extends IActionBasicProps {
    subitems: IActionsMenuActionItemProps[];
}

export type TActionsMenuItem = IActionsMenuActionItemProps | IActionsMenuParentItemProps;

export interface IActionsMenuProps extends React.HTMLAttributes<HTMLDivElement> {
    actionPrimary?: IActionButtonItemProps;
    actionsSecondary?: IActionButtonItemProps[];
    actionsFullMenu?: TActionsMenuItem[];
}

export function ActionsMenu({ actionPrimary, actionsSecondary, actionsFullMenu }: IActionsMenuProps) {
    const renderMenuItems = (items) => {
        return items.map((item, index) => {
            const { text, icon, actionHandler, disabled, subitems, ...otherProps } = item;
            return subitems && subitems.length > 0 ? (
                <MenuItem
                    {...otherProps}
                    icon={icon}
                    key={"menuitem_" + index}
                    text={
                        <OverflowText>{text}</OverflowText>
                        /* FIXME: CMEM-3742: change this OverflowText later to a multiline=false option on MenuItem, seems to be a new one*/
                    }
                >
                    {renderMenuItems(subitems)}
                </MenuItem>
            ) : (
                <MenuItem
                    {...otherProps}
                    icon={icon}
                    key={"menuitem_" + index}
                    text={<OverflowText>{text}</OverflowText>}
                    onClick={actionHandler}
                    disabled={disabled ? true : false}
                />
            );
        });
    };

    return (
        <>
            {actionPrimary && (
                <Button
                    icon={actionPrimary.icon}
                    text={actionPrimary.text}
                    key={actionPrimary.text}
                    affirmative={actionPrimary.affirmative ? true : false}
                    disruptive={actionPrimary.disruptive ? true : false}
                    disabled={actionPrimary.disabled ? true : false}
                    outlined={true}
                    onClick={actionPrimary.actionHandler}
                    data-test-id={!!actionPrimary["data-test-id"] ? actionPrimary["data-test-id"] : false}
                />
            )}
            {actionsSecondary &&
                actionsSecondary.length > 0 &&
                actionsSecondary.map((actionItem) => {
                    const { icon, text, affirmative, disruptive, disabled, actionHandler, ...otherProps } = actionItem;
                    return (
                        <IconButton
                            {...otherProps}
                            name={icon ? icon : "undefined"}
                            text={text}
                            key={text}
                            affirmative={affirmative ? true : false}
                            disruptive={disruptive ? true : false}
                            disabled={disabled ? true : false}
                            onClick={actionHandler}
                        />
                    );
                })}
            {actionsFullMenu && actionsFullMenu.length > 0 && (
                <ContextMenu data-test-id={"header-context-menu-btn"}>{renderMenuItems(actionsFullMenu)}</ContextMenu>
            )}
        </>
    );
}
