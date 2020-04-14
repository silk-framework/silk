/*
    provides a simple interface for dialogs using modals with a card inside
*/

import React from 'react';
import {
    Card,
    CardHeader,
    CardTitle,
    CardContent,
    CardActions,
} from "./../Card";
import Divider from "./../Separation/Divider";
import Modal from './Modal';

function SimpleDialog({
    children,
    canOutsideClickClose=false,
    canEscapeKeyClose=false,
    title='',
    actions=<></>,
    hasBorder=false,
    preventSimpleClosing=false,
    ...otherProps
}: any) {

    return (
        <Modal
            {...otherProps}
            canOutsideClickClose={canOutsideClickClose || !preventSimpleClosing}
            canEscapeKeyClose={canEscapeKeyClose || !preventSimpleClosing}
        >
            <Card>
                {
                    title && <CardHeader>
                        <CardTitle>{title}</CardTitle>
                    </CardHeader>
                }
                { hasBorder && <Divider /> }
                <CardContent>
                    {children}
                </CardContent>
                { hasBorder && <Divider /> }
                {
                    actions && <CardActions inverseDirection>
                        {actions}
                    </CardActions>
                }
            </Card>
        </Modal>
    );
};

export default SimpleDialog;
