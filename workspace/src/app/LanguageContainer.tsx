import React, { Component } from 'react';
import { IntlProvider, addLocaleData } from 'react-intl';
import locale_de from 'react-intl/locale-data/de';
import messages_de from '../translations/de.json';
import { connect } from "react-redux";
import { globalSel } from "./store/ducks/global";

addLocaleData([...locale_de ]);

const messages = {
    'en': null,
    'de': messages_de,
};

interface IProps {
    locale: string;
}

const mapStateToProps = (state) => ({
    locale: globalSel.globalSelector(state).locale
});

class LanguageContainer extends Component<IProps, {}> {
    render() {
        const msg = messages[this.props.locale];
        return (
            <IntlProvider key={this.props.locale} locale={this.props.locale} messages={msg}>
                {this.props.children}
            </IntlProvider>
        );
    }
}

export default connect(mapStateToProps, null)(LanguageContainer);
