import { NotAvailable } from '@eccenca/gui-elements';
import { URIInfo } from './URIInfo';
import React from 'react';

export const ThingDescription = ({id}) => {
    const fallbackInfo = (
        <NotAvailable
            inline
            label="No description available."
        />
    );
    return <URIInfo uri={id} field="description" fallback={fallbackInfo}/>;
};
