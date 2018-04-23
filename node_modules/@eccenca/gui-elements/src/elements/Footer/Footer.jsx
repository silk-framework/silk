import React from 'react';
import PropTypes from 'prop-types';

import Version from '../Version/Version';

const Footer = props => {
    const year = new Date().getFullYear();

    const workspace = props.workspace ? (
        <div className="mdl-mini-footer__left-section">
            Workspace: {props.workspace}
        </div>
    ) : (
        false
    );

    return (
        <div className="ecc-component-footer">
            <footer className="mdl-mini-footer">
                {workspace}
                <div className="mdl-mini-footer__right-section">
                    <div className="mdl-logo">
                        <Version version={props.version} />
                        &copy; {year}
                    </div>
                    <ul className="mdl-mini-footer__link-list">
                        <li>
                            <a href={props.companyUrl} target="_blank">
                                {props.company}
                            </a>
                        </li>
                    </ul>
                </div>
            </footer>
        </div>
    );
};

Footer.propTypes = {
    company: PropTypes.string.isRequired,
    version: PropTypes.string.isRequired,
    companyUrl: PropTypes.string.isRequired,
    workspace: PropTypes.string,
};

export default Footer;
