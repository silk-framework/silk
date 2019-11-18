import React, { memo, useCallback } from 'react';
import { Divider, Layout, Menu } from "antd";
import './Header.scss';
import { globalOp, globalSel } from "../../../state/ducks/global";
import { useDispatch, useSelector } from "react-redux";
import { Link } from "react-router-dom";

interface IProps {
    externalRoutes: any;
}

const {SubMenu, Item} = Menu;
const Header = memo<IProps>(({externalRoutes}) => {
    const isPresentableRoute = r => r.menuName;

    const addPluginRoutesInMenu = (route) => {
        const menuItem: any = {
            title: route.menuName,
            key: route.menuName.toLowerCase(),
        };
        if (route.path) {
            menuItem.url = route.path;
        }
        return menuItem
    };

    const generateMenuItems = (pluginMenuData) => {
        const dispatch = useDispatch();
        const onLogout = useCallback(
            () => dispatch(globalOp.logout()),
            [dispatch]
        );

        const menuData = [
            {
                title: 'Dashboard',
                key: 'dashboard',
                url: '/'
            },
            {
                title: 'User',
                key: 'user',
                children: [
                    {
                        title: 'Logout',
                        key: 'logout',
                        onClick: onLogout
                    }
                ]
            },
            ...pluginMenuData,
        ];

        const generateItem = item => {
            const {
                key, title, url,
                icon, disabled,
                onClick = () => {
                }
            } = item;

            if (item.divider) {
                return <Divider key={Math.random()}/>
            }
            if (item.url) {
                return (
                    <Item key={key} disabled={disabled}>
                        <Link to={url}>
                            {icon && <span className={`${icon} icon icon-collapsed-hidden`}/>}
                            <span>{title}</span>
                        </Link>
                    </Item>
                )
            }
            return (
                <Item key={key} disabled={disabled} onClick={onClick}>
                    {icon && <span className={`${icon} icon icon-collapsed-hidden`}/>}
                    <span>{title}</span>
                </Item>
            )
        };

        const generateSubmenu = items => items.map(menuItem => generateItem(menuItem));

        const generateMainMenu = menuItem => {
            if (menuItem.children) {
                return (
                    <SubMenu title={menuItem.title} key={menuItem.key}>
                        {generateSubmenu(menuItem.children)}
                    </SubMenu>
                )
            }
            return generateItem(menuItem)
        };

        return menuData.map(generateMainMenu);
    };

    const pluginMenuData = externalRoutes
        .filter(isPresentableRoute)
        .map(addPluginRoutesInMenu);

    const menu = generateMenuItems(pluginMenuData);

    const isAuth = useSelector(globalSel.isAuthSelector);

    return (
        !isAuth ? null :
            <div className="header">
                <Menu theme="dark"
                      mode="horizontal"
                      defaultSelectedKeys={['dashboard']}
                      style={{lineHeight: '64px'}}
                >
                    {menu}
                </Menu>
            </div>
    )
});

export default Header;
