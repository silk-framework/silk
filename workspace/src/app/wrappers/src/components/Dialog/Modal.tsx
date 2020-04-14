/*
    we only use Dialog as pre-configured and enhanced Overlay, it is
    recommended to use Card elements inside.
*/

import React from 'react';
import {
    Overlay as BlueprintOverlay,
    Classes as BlueprintClassNames,
} from "@blueprintjs/core";
import { Card } from "./../Card";

function Modal({
    children,
    className='',
    overlayClassName='',
    size="regular", // tiny, small, regular, large, fullscreen
    canOutsideClickClose=false,
    canEscapeKeyClose=false,
    preventBackdrop=false,
    ...otherProps
}: any) {

    const alteredChildren = React.Children.map(children, (child, index) => {
        if (child.type === Card) {
            return React.cloneElement(
                child,
                {
                    isOnlyLayout: true,
                    elevation: 4
                }
            );
        }

        return child;
    });

    return (
        <BlueprintOverlay
            {...otherProps}
            className={
                overlayClassName
            }
            backdropClassName={'ecc-dialog__backdrop'}
            canOutsideClickClose={canOutsideClickClose}
            canEscapeKeyClose={canEscapeKeyClose}
            hasBackdrop={!preventBackdrop}
        >
            <div className={BlueprintClassNames.DIALOG_CONTAINER}>
                <section
                    className={
                        'ecc-dialog__wrapper' +
                        (typeof size === 'string' ? ' ' + 'ecc-dialog__wrapper--' + size : '') +
                        (className ? ' ' + className : '')
                    }
                >
                    {alteredChildren}
                </section>
            </div>
        </BlueprintOverlay>
    );
};

export default Modal;
