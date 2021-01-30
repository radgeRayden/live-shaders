using import struct
using import glm

import .gl
import .utils
let _gl = (import .FFI.glad)

let default-vshader default-fshader =
    do
        using import glsl
        using import glm
        fn vertex ()
            let tl bl br tr =
                # 0 -- 3
                # |    |
                # 1 -- 2
                vec2 -1  1
                vec2 -1 -1
                vec2  1 -1
                vec2  1  1
            local quad =
                arrayof vec2 tl bl br br tr tl
            position := quad @ gl_VertexID

            uniform iResolution : vec3
            out fragCoord : vec2
                location = 0

            fragCoord = (iResolution.xy * ((position + 1) / 2))
            gl_Position = (vec4 position 0 1)

        fn frag ()
            in fragCoord : vec2
                location = 0
            out fragColor : vec4
                location = 0

            uniform iResolution : vec3
            uniform iTime : f32
            uniform iTimeDelta : f32
            uniform iFrame : f32
            uniform iMouse : vec4
            uniform iDate : vec4
            fragColor = (vec4 0.017 0.017 0.017 1)

        _ vertex frag

let shader-scope =
    ..
        import glsl
        import glm
        do
            using import glsl
            using import glm
            do
                spice-quote
                    in fragCoord : vec2
                        location = 0
                    out fragColor : vec4
                        location = 0

                    uniform iResolution : vec3
                    uniform iTime : f32
                    uniform iTimeDelta : f32
                    uniform iFrame : f32
                    uniform iMouse : vec4
                    uniform iDate : vec4
                locals;
        sc_get_globals;

run-stage;

global shader-program : gl.GPUShaderProgram

global uniform-locations :
    struct UniformLocations
        iResolution : i32
        iTime : i32
        iTimeDelta : i32
        iFrame : i32
        iMouse : i32
        iDate : i32

struct Uniforms
    iResolution : vec3
    iTime : f32
    iTimeDelta : f32
    iFrame : f32
    iMouse : vec4
    iDate : vec4

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

fn wrap-shader (path scope)
    if (not (sc_is_file path))
        error (.. "file not found: " path)

    # get shader extension
    let match? start end = ('match? "\\.[A-Za-z]+$" path)
    let shader-kind =
        if match?
            # +1 to skip the dot
            let ext = (rslice path (start + 1))
            match ext
            case "frag"
                'glsl
            case "glsl"
                'glsl
            case "sc"
                'scopes
            default
                error "unrecognized file extension. Must be one of: .glsl .frag .sc"
        else
            error "invalid file path"
    if (shader-kind == 'scopes)
        gl.GPUShaderProgram default-vshader
            (_load-module path path scope) as Closure
    else
        let src = (utils.read-file path)
        gl.GPUShaderProgram default-vshader src

fn update-shader (path)
    shader-program = (wrap-shader path shader-scope)

    _gl.UseProgram shader-program
    uniform-locations.iResolution =
        _gl.GetUniformLocation shader-program "iResolution"
    uniform-locations.iTime =
        _gl.GetUniformLocation shader-program "iTime"
    uniform-locations.iTimeDelta =
        _gl.GetUniformLocation shader-program "iTimeDelta"
    uniform-locations.iFrame =
        _gl.GetUniformLocation shader-program "iFrame"
    uniform-locations.iMouse =
        _gl.GetUniformLocation shader-program "iMouse"
    uniform-locations.iDate =
        _gl.GetUniformLocation shader-program "iDate"

fn update-uniforms (uniforms)
    _gl.Uniform3f uniform-locations.iResolution (unpack uniforms.iResolution)
    _gl.Uniform1f uniform-locations.iTime uniforms.iTime
    _gl.Uniform1f uniform-locations.iTimeDelta uniforms.iTimeDelta
    _gl.Uniform1f uniform-locations.iFrame uniforms.iFrame
    _gl.Uniform4f uniform-locations.iMouse (unpack uniforms.iMouse)
    _gl.Uniform4f uniform-locations.iDate (unpack uniforms.iDate)

fn init ()
    shader-program = (gl.GPUShaderProgram default-vshader default-fshader)
    _gl.UseProgram shader-program

do
    let update-shader update-uniforms Uniforms init
    locals;