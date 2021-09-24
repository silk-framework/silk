import React from 'react';
import { Button } from '@gui-elements/legacy-replacements';
import {Link} from "@gui-elements/index";

const ComplexEditButton = ({ onClick, href, tooltip, asLink }) => {
    let externalHref = {};
    if (href) {
        externalHref = {
            href
        }
    }
    if(asLink) {
        return <Link
            data-test-id={"complex-rule-edit-button"}
            href={href}
            onClick={onClick}
        >{tooltip}</Link>
    } else {
        return <Button
            data-test-id={"complex-rule-edit-button"}
            raised
            iconName="edit"
            className="ecc-silk-mapping__ruleseditor__actionrow-complex-edit"
            onClick={onClick}
            tooltip={tooltip}
            {...externalHref}
        />
    }
};

export default ComplexEditButton;
