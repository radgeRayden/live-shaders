# Parses and compile a module wrapped as a GLSL shader.
# Code largely copied from core.sc

fn wrap-module (expr eval-scope)
    let ModuleFunctionType = (pointer (raises (function Value) Error))
    let StageFunctionType = (pointer (raises (function CompileStage) Error))
    let expr-anchor = ('anchor expr)
    let f =
        do
            hide-traceback;
            sc_eval expr-anchor (expr as list) eval-scope
    loop (f expr-anchor = f expr-anchor)
        # wrap in a function and typify so we can decide whether this is a compile stage or module
        let wrapf =
            spice-quote
                fn "exec-module-stage" ()
                    raising Error
                    hide-traceback;
                    wrap-if-not-run-stage
                        spice-unquote
                            'tag `(f) expr-anchor
        let path =
            .. ((sc_anchor_path expr-anchor) as string) ":"
                tostring `[(sc_anchor_lineno expr-anchor)]
        sc_template_set_name wrapf (Symbol path)
        let typified-wrapf = (sc_typify_template wrapf 0 (undef TypeArrayPointer))

        if (('typeof typified-wrapf) == StageFunctionType)
            # compile stages are immediately executed as usual. This way we can do some configuration in the
            # shader "header", as well as making ad-hoc macros available.
            let f =
                do
                    hide-traceback;
                    sc_compile typified-wrapf
                        compile-flag-cache
                        # can't use this flag yet because it breaks code
                        #| compile-flag-cache compile-flag-O2
            let fptr = (f as StageFunctionType)
            let result =
                do
                    hide-traceback;
                    fptr;
            let result = (bitcast result Value)
            repeat result ('anchor result)
        # if this is a module, we want to delay compilation so it can be turned into a shader.
        # For this purpose we wrap it as a more suitable closure, that doesn't raise Error nor returns anything.
        # We then return the template, so it can be forwarded to the shader constructor.
        else
            let wrapf =
                spice-quote
                    fn "exec-module-stage" ()
                        spice-unquote
                            'tag `(f) expr-anchor
            break (sc_prove wrapf)

fn _load-module (module-name module-path scope)
    if (not (sc_is_file module-path))
        hide-traceback;
        error
            .. "no such module: " module-path
    let module-path = (sc_realpath module-path)
    let module-dir = (sc_dirname module-path)
    let expr =
        do
            hide-traceback;
            sc_parse_from_path module-path
    let eval-scope =
        'bind-symbols
            scope
            main-module? = true
            module-path = module-path
            module-dir = module-dir
            module-name = module-name

    try
        hide-traceback;
        wrap-module expr (Scope eval-scope)
    except (err)
        hide-traceback;
        error@+ err unknown-anchor
            "while loading module " .. module-path

fn wrap-shader (name path scope)
    (_load-module name path scope) as Closure

do
    let wrap-shader
    locals;