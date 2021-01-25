using import String
using import enum

let gl = (import .FFI.glad)
import .io
using import .radlib.core-extensions

spice patch-shader (shader patch)
    shader as:= string
    patch as:= string
    let match? start end = ('match? "^#version \\d\\d\\d\\n" shader)
    if match?
        let head = (lslice shader end)
        let tail = (rslice shader end)
        let result = (.. head patch tail)
        `result
    else
        error "unrecognized shader input"
run-stage;

typedef GPUShaderProgram <:: u32
    fn compile-shader (source kind)
        imply kind i32
        source as:= rawstring

        let handle = (gl.CreateShader (kind as u32))
        gl.ShaderSource handle 1 (&local source) null
        gl.CompileShader handle

        local compilation-status : i32
        gl.GetShaderiv handle gl.GL_COMPILE_STATUS &compilation-status
        if (not compilation-status)
            local log-length : i32
            local message : (array i8 1024)
            gl.GetShaderInfoLog handle (sizeof message) &log-length &message
            io.log "Shader compilation error:"
            io.log (String &message (log-length as usize))
            io.log "\n"
        handle

    fn link-program (vs fs)
        let program = (gl.CreateProgram)
        gl.AttachShader program vs
        gl.AttachShader program fs
        gl.LinkProgram program
        # could make this less copy pastey by abstracting away error logging
        local link-status : i32
        gl.GetProgramiv program gl.GL_LINK_STATUS &link-status
        if (not link-status)
            local log-length : i32
            local message : (array i8 1024)
            gl.GetProgramInfoLog program (sizeof message) &log-length &message
            io.log "Shader program linking error:\n"
            io.log (String &message (log-length as usize))
            io.log "\n"
        # because we preemptively delete the shader stages, they are
            already marked for deletion when the program is dropped.
        gl.DeleteShader fs
        gl.DeleteShader vs
        program

    inline... __typecall (cls)
        bitcast 0 this-type
    case (cls handle)
        bitcast handle this-type
    case (cls vs fs)
        # TODO: move this out of here, maybe we need a variant that takes strings
        let vsource =
            patch-shader
                static-if (constant? vs)
                    static-compile-glsl 420 'vertex (static-typify vs)
                else
                    compile-glsl 420 'vertex (static-typify vs)
                "#extension GL_ARB_shader_storage_buffer_object : require\n"
        let vertex-module =
            compile-shader
                vsource as rawstring
                gl.GL_VERTEX_SHADER
        let fsource =
            static-if (constant? fs)
                (static-compile-glsl 420 'fragment (static-typify fs)) as rawstring
            else
                (compile-glsl 420 'fragment (static-typify fs)) as rawstring
        let fragment-module =
            compile-shader
                fsource
                gl.GL_FRAGMENT_SHADER

        let program = (link-program vertex-module fragment-module)
        bitcast program this-type

    inline __imply (selfT otherT)
        static-if (otherT == (storageof this-type))
            inline (self)
                storagecast (view self)

    inline __drop (self)
        gl.DeleteProgram (storagecast (view self))


enum OpenGLDebugLevel plain
    HIGH
    MEDIUM
    LOW
    NOTIFICATION
run-stage;

fn init ()
    gl.init;

    # log-level is the lowest severity level we care about.
    fn openGL-error-callback (source _type id severity _length message user-param)
        let log-level = OpenGLDebugLevel.LOW
        inline gl-debug-source (source)
            match source
            case gl.GL_DEBUG_SOURCE_API_ARB                (String "API")
            case gl.GL_DEBUG_SOURCE_WINDOW_SYSTEM_ARB      (String "Window System")
            case gl.GL_DEBUG_SOURCE_SHADER_COMPILER_ARB    (String "Shader Compiler")
            case gl.GL_DEBUG_SOURCE_THIRD_PARTY_ARB        (String "Third Party")
            case gl.GL_DEBUG_SOURCE_APPLICATION_ARB        (String "Application")
            case gl.GL_DEBUG_SOURCE_OTHER_ARB              (String "Other")
            default                                        (String "?")

        inline gl-debug-type (type_)
            match type_
            case gl.GL_DEBUG_TYPE_ERROR_ARB                (String "Error")
            case gl.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR_ARB  (String "Deprecated")
            case gl.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR_ARB   (String "Undefined Behavior")
            case gl.GL_DEBUG_TYPE_PORTABILITY_ARB          (String "Portability")
            case gl.GL_DEBUG_TYPE_PERFORMANCE_ARB          (String "Performance")
            case gl.GL_DEBUG_TYPE_OTHER_ARB                (String "Other")
            default                                        (String "?")

        inline gl-debug-severity (severity)
            match severity
            case gl.GL_DEBUG_SEVERITY_HIGH_ARB             (String "High")
            case gl.GL_DEBUG_SEVERITY_MEDIUM_ARB           (String "Medium")
            case gl.GL_DEBUG_SEVERITY_LOW_ARB              (String "Low")
            # case gl.GL_DEBUG_SEVERITY_NOTIFICATION_ARB     (String "Notification")
            default                                        (String "?")

        using OpenGLDebugLevel
        switch severity
        case gl.GL_DEBUG_SEVERITY_HIGH_ARB
        case gl.GL_DEBUG_SEVERITY_MEDIUM_ARB
            static-if (log-level < MEDIUM)
                return;
        case gl.GL_DEBUG_SEVERITY_LOW_ARB
            static-if (log-level < LOW)
                return;
        default
            ;

        io.log "source: %s | type: %s | severity: %s | message: %s\n"
            (gl-debug-source source)
            (gl-debug-type _type)
            (gl-debug-severity severity)
            message
        ;

    # gl.Enable gl.GL_DEBUG_OUTPUT
    gl.Enable gl.GL_BLEND
    gl.BlendFunc gl.GL_SRC_ALPHA gl.GL_ONE_MINUS_SRC_ALPHA
    # gl.Enable gl.GL_MULTISAMPLE
    gl.Enable gl.GL_FRAMEBUFFER_SRGB
    # TODO: add some colors to this
    gl.DebugMessageCallbackARB openGL-error-callback null
    local VAO : gl.GLuint
    gl.GenVertexArrays 1 &VAO
    gl.BindVertexArray VAO

fn clear (color...)
    gl.ClearColor color...
    gl.Clear (gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT)

do
    let
        # types
        GPUShaderProgram

        # interface
        init
        clear
    locals;