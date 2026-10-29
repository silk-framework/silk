import React from "react";

type TranslateOptions = string | { defaultValue?: string };
type GenericProps = Record<string, unknown>;
type IntrinsicTag = keyof React.JSX.IntrinsicElements;

interface ButtonMockProps extends GenericProps {
    children?: React.ReactNode;
    onClick?: React.MouseEventHandler<HTMLButtonElement>;
    text?: React.ReactNode;
    name?: string;
    loading?: boolean;
    includeLoadingState?: boolean;
}

interface CodeEditorMockProps {
    defaultValue?: string;
    onChange: (value: string) => void;
    id?: string;
    name?: string;
    "data-test-id"?: string;
}

interface SimpleDialogMockOptions {
    respectIsOpen?: boolean;
}

interface SimpleDialogMockProps {
    isOpen?: boolean;
    title?: React.ReactNode;
    children?: React.ReactNode;
    actions?: React.ReactNode;
}

interface TextFieldMockOptions {
    includePlaceholder?: boolean;
    includeTestId?: boolean;
    includeType?: boolean;
}

interface TextFieldMockProps {
    value?: string | number;
    onChange?: React.ChangeEventHandler<HTMLInputElement>;
    id?: string;
    type?: string;
    placeholder?: string;
    "data-test-id"?: string;
}

interface ChildrenOnlyProps {
    children?: React.ReactNode;
}

interface ClickableContainerProps extends ChildrenOnlyProps {
    onClick?: React.MouseEventHandler<HTMLElement>;
}

interface FieldItemMockOptions {
    wrapper?: "div" | "label";
    helperTextProp?: string;
    useHtmlFor?: boolean;
}

interface FieldItemMockProps extends GenericProps {
    children?: React.ReactNode;
    labelProps?: {
        text?: React.ReactNode;
        htmlFor?: string;
    };
}

const testTranslate = (key: string, options?: TranslateOptions) =>
    typeof options === "string" ? options : (options?.defaultValue ?? key);

const omitUnsupportedDomProps = (props: GenericProps) => {
    const {
        addSpacing,
        "data-test-id": dataTestId,
        affirmative,
        boxed,
        canGrow,
        canShrink,
        description,
        disruptive,
        elevated,
        hasDivider,
        hasSpacing,
        hideOverflow,
        intent,
        interactive,
        large,
        medium,
        minimal,
        noWrap,
        rightIcon,
        singleColumn,
        size,
        small,
        title,
        tooltip,
        tooltipProps,
        useAbsoluteSpace,
        verticalStretchable,
        verticalStretched,
        ...domProps
    } = props;
    return dataTestId ? { ...domProps, "data-testid": dataTestId } : domProps;
};

const createButtonMock =
    (transformProps: (props: ButtonMockProps) => ButtonMockProps = (props) => props) =>
    ({ children, ...props }: ButtonMockProps) => {
        const { text, onClick, loading, includeLoadingState = false, ...buttonProps } = transformProps(props);
        return (
            <button
                onClick={onClick}
                data-loading={includeLoadingState ? (loading ? "true" : "false") : undefined}
                {...omitUnsupportedDomProps(buttonProps)}
            >
                {children ?? text}
            </button>
        );
    };

const createCodeEditorMock =
    (React: typeof import("react")) =>
    ({ defaultValue, onChange, id, name, "data-test-id": dataTestId }: CodeEditorMockProps) => {
        const [value, setValue] = React.useState(defaultValue ?? "");
        React.useEffect(() => {
            setValue(defaultValue ?? "");
        }, [defaultValue]);
        return (
            <textarea
                id={id}
                name={name}
                data-testid={dataTestId}
                value={value}
                onChange={(event) => {
                    setValue(event.target.value);
                    onChange(event.target.value);
                }}
            />
        );
    };

const createSimpleDialogMock =
    ({ respectIsOpen }: SimpleDialogMockOptions = { respectIsOpen: false }) =>
    ({ isOpen, title, children, actions }: SimpleDialogMockProps) =>
        respectIsOpen && !isOpen ? null : (
            <div>
                <h1>{title}</h1>
                <div>{children}</div>
                <div>{actions}</div>
            </div>
        );

const createTextFieldMock =
    ({ includePlaceholder = false, includeTestId = false, includeType = false }: TextFieldMockOptions = {}) =>
    ({ value, onChange, id, type, placeholder, "data-test-id": dataTestId }: TextFieldMockProps) => (
        <input
            id={id}
            type={includeType ? (type ?? "text") : undefined}
            value={value}
            onChange={onChange}
            placeholder={includePlaceholder ? placeholder : undefined}
            data-testid={includeTestId ? dataTestId : undefined}
        />
    );

const createDivPassthroughMock =
    (tag: IntrinsicTag = "div") =>
    ({ children, ...props }: ChildrenOnlyProps & GenericProps) => {
        return React.createElement(tag, omitUnsupportedDomProps(props), children);
    };

const createChildrenOnlyMock =
    (tag: IntrinsicTag = "div") =>
    ({ children }: ChildrenOnlyProps) =>
        React.createElement(tag, undefined, children);

const createClickableContainerMock =
    (tag: IntrinsicTag = "div") =>
    ({ children, onClick }: ClickableContainerProps) =>
        React.createElement(tag, { onClick }, children);

const createFieldItemMock =
    ({ wrapper = "div", helperTextProp, useHtmlFor = false }: FieldItemMockOptions = {}) =>
    ({ children, labelProps, ...props }: FieldItemMockProps) => {
        const helperText = helperTextProp ? (props[helperTextProp] as React.ReactNode) : undefined;
        const domProps = { ...props };
        if (helperTextProp) {
            delete domProps[helperTextProp];
        }
        return React.createElement(
            wrapper,
            wrapper === "div"
                ? omitUnsupportedDomProps(domProps)
                : { htmlFor: useHtmlFor ? labelProps?.htmlFor : undefined },
            <>
                {labelProps?.text ? <label>{labelProps.text}</label> : null}
                {children}
                {helperText ? <div>{helperText}</div> : null}
            </>,
        );
    };

const createFieldSetMock =
    () =>
    ({ children, title, ...props }) => (
        <fieldset {...omitUnsupportedDomProps(props)}>
            {title ? <legend>{title}</legend> : null}
            {children}
        </fieldset>
    );

const createCheckboxMock =
    () =>
    ({ checked, onChange, id }) => (
        <input id={id} type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />
    );

const createTextAreaMock =
    () =>
    ({ value, onChange, id }) => <textarea id={id} value={value} onChange={onChange} />;

const createSearchFieldMock =
    () =>
    ({ value, onChange, emptySearchInputMessage }) => (
        <input aria-label={emptySearchInputMessage} value={value} onChange={onChange} />
    );

const createTagMock =
    () =>
    ({ children, onClick, onRemove, intent, ...props }) => (
        <span>
            <button onClick={onClick} data-intent={intent} {...omitUnsupportedDomProps(props)}>
                {children}
            </button>
            {onRemove ? <button onClick={onRemove}>remove</button> : null}
        </span>
    );

const createAlertDialogMock =
    () =>
    ({ isOpen, title, children, actions }) =>
        isOpen ? (
            <div data-testid="alert-dialog">
                <div>{title}</div>
                <div>{children}</div>
                <div>{actions}</div>
            </div>
        ) : null;

const createContextOverlayMock =
    () =>
    ({ children, content, isOpen }) => (
        <div>
            {children}
            {isOpen ? <div data-testid="context-overlay">{content}</div> : null}
        </div>
    );

const createContextMenuMock =
    () =>
    ({ togglerElement, children }) => (
        <div>
            {togglerElement}
            <div>{children}</div>
        </div>
    );

const createIconMock =
    () =>
    ({ name }) => <span>{Array.isArray(name) ? name.join(" ") : name}</span>;

const createMenuItemMock =
    ({ supportChildren = false } = {}) =>
    ({ text, children, onClick, disabled, htmlTitle, ...props }) => (
        <button onClick={onClick} disabled={disabled} {...omitUnsupportedDomProps(props)}>
            {supportChildren ? (text ?? children) : text}
        </button>
    );

const createNotificationMock =
    () =>
    ({ children, actions, intent }) => (
        <div data-testid="notification" data-intent={intent ?? ""}>
            <div>{children}</div>
            {actions ? <div>{actions}</div> : null}
        </div>
    );

const createClassNamesMock = () => ({
    Intent: {
        ACCENT: "eccgui-intent--accent",
    },
});

const createFragmentMock =
    () =>
    ({ children }) => <>{children}</>;

const mockReactI18next = (translate = testTranslate) => {
    const result = Object.assign([translate], {
        t: translate,
        i18n: { language: "en" },
    });
    jest.doMock("react-i18next", () => ({
        useTranslation: () => result,
    }));
};

const mockI18next = (translate = testTranslate) => {
    jest.doMock("i18next", () => ({
        __esModule: true,
        default: {
            t: translate,
        },
    }));
};

const jestTestUtils = {
    testTranslate,
    omitUnsupportedDomProps,
    createButtonMock,
    createCodeEditorMock,
    createSimpleDialogMock,
    createTextFieldMock,
    createDivPassthroughMock,
    createChildrenOnlyMock,
    createClickableContainerMock,
    createFieldItemMock,
    createFieldSetMock,
    createCheckboxMock,
    createTextAreaMock,
    createSearchFieldMock,
    createTagMock,
    createAlertDialogMock,
    createContextOverlayMock,
    createContextMenuMock,
    createIconMock,
    createMenuItemMock,
    createNotificationMock,
    createClassNamesMock,
    createFragmentMock,
    mockReactI18next,
    mockI18next,
};

export default jestTestUtils;
