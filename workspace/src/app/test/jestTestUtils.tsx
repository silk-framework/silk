type TranslateOptions = string | { defaultValue?: string };

export const testTranslate = (key: string, options?: TranslateOptions) =>
    typeof options === "string" ? options : options?.defaultValue ?? key;

export const mockReactI18next = (translate = testTranslate) => {
    const result = Object.assign([translate], {
        t: translate,
        i18n: { language: "en" },
    });
    jest.doMock("react-i18next", () => ({
        useTranslation: () => result,
    }));
};

export const mockI18next = (translate = testTranslate) => {
    jest.doMock("i18next", () => ({
        __esModule: true,
        default: {
            t: translate,
        },
    }));
};

export const createFormGuiElementsModule = () => {
    const React = require("react");
    return {
        Button: ({ children, affirmative, disruptive, ...props }) => <button {...props}>{children}</button>,
        CodeEditor: ({ defaultValue, onChange, id, name, "data-test-id": dataTestId }) => {
            const [value, setValue] = React.useState(defaultValue ?? "");
            React.useEffect(() => {
                setValue(defaultValue ?? "");
            }, [defaultValue]);
            return (
                <textarea
                    id={id}
                    name={name}
                    data-test-id={dataTestId}
                    value={value}
                    onChange={(event) => {
                        setValue(event.target.value);
                        onChange(event.target.value);
                    }}
                />
            );
        },
        FieldItem: ({ children, labelProps, messageText }) => (
            <label htmlFor={labelProps?.htmlFor}>
                <span>{labelProps?.text}</span>
                {children}
                {messageText ? <div>{messageText}</div> : null}
            </label>
        ),
        SimpleDialog: ({ isOpen, title, children, actions }) =>
            isOpen ? (
                <div>
                    <h1>{title}</h1>
                    <div>{children}</div>
                    <div>{actions}</div>
                </div>
            ) : null,
        Switch: ({ checked, onChange, id }) => (
            <input
                id={id}
                type="checkbox"
                checked={checked}
                onChange={(event) => onChange(event.target.checked)}
            />
        ),
        TextArea: ({ value, onChange, id }) => <textarea id={id} value={value} onChange={onChange} />,
        TextField: ({ value, onChange, id, type }) => (
            <input id={id} type={type ?? "text"} value={value} onChange={onChange} />
        ),
    };
};

export const createRuleBlockEditorGuiElementsModule = () => {
    const React = require("react");
    return {
        AlertDialog: ({ isOpen, title, children, actions }) =>
            isOpen ? (
                <div data-testid="alert-dialog">
                    <div>{title}</div>
                    <div>{children}</div>
                    <div>{actions}</div>
                </div>
            ) : null,
        Button: ({ children, onClick, disabled, loading, affirmative, disruptive, tooltip, tooltipProps, ...props }) => (
            <button onClick={onClick} disabled={disabled} data-loading={loading ? "true" : "false"} {...props}>
                {children}
            </button>
        ),
        ContextOverlay: ({ children, content, isOpen }) => (
            <div>
                {children}
                {isOpen ? <div data-testid="context-overlay">{content}</div> : null}
            </div>
        ),
        // Tests render menu items eagerly to avoid coupling assertions to overlay open/close mechanics.
        ContextMenu: ({ togglerElement, children }) => (
            <div>
                {togglerElement}
                <div>{children}</div>
            </div>
        ),
        Icon: ({ name }) => <span>{Array.isArray(name) ? name.join(" ") : name}</span>,
        IconButton: ({ text, name, onClick, disabled }) => (
            <button onClick={onClick} disabled={disabled}>
                {text ?? name}
            </button>
        ),
        MenuItem: ({ text, onClick, disabled }) => (
            <button onClick={onClick} disabled={disabled}>
                {text}
            </button>
        ),
        Notification: ({ children, actions, intent }) => (
            <div data-testid="notification" data-intent={intent ?? ""}>
                <div>{children}</div>
                {actions ? <div>{actions}</div> : null}
            </div>
        ),
        Spacing: () => <div />,
        ToolbarSection: ({ children }) => <div>{children}</div>,
    };
};

export const createRuleNodeMenuGuiElementsModule = () => {
    const React = require("react");
    return {
        Menu: ({ children }) => <div data-testid="menu">{children}</div>,
        MenuDivider: () => <div data-testid="menu-divider" />,
        MenuItem: ({ text, children, onClick, htmlTitle, ...props }) => (
            <button onClick={onClick} {...props}>
                {text ?? children}
            </button>
        ),
    };
};

export const createNodeToolsModule = () => {
    const React = require("react");
    return {
        NodeTools: ({ children, menuFunctionsCallback }) => {
            React.useEffect(() => {
                menuFunctionsCallback?.({ closeMenu: jest.fn() });
            }, [menuFunctionsCallback]);
            return <div data-testid="node-tools">{children}</div>;
        },
    };
};
