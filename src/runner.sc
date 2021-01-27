load-library (module-dir .. "/../libgame.so")
load-library "libglfw.so"
run-stage;

using import struct
import .window
import .gl
import .wrapper

let file-watcher = (import .radlib.file-watcher)
let _gl = (import .FFI.glad)
let glfw = (import .FFI.glfw)

window.init;
gl.init;

let shader-scope =
    ..
        (sc_get_globals)
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
                locals;

run-stage;

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
            out fcolor : vec4
                location = 0
            fcolor = (vec4 0.017 0.017 0.017 1)

        _ vertex frag

global shader-program =
    gl.GPUShaderProgram default-vshader default-fshader

global uniforms :
    struct UniformLocations
        iResolution : i32
        iTime : i32
        iTimeDelta : i32

fn update-shader ()
    let frag = (wrapper.wrap-shader "test" "test.sc" shader-scope)
    shader-program = (gl.GPUShaderProgram default-vshader frag)

    _gl.UseProgram shader-program
    uniforms.iResolution =
        _gl.GetUniformLocation shader-program "iResolution"
    uniforms.iTime =
        _gl.GetUniformLocation shader-program "iTime"
    uniforms.iTimeDelta =
        _gl.GetUniformLocation shader-program "iTimeDelta"

update-shader;
glfw.SetTime 0:f64

using file-watcher
global fw = (FileWatcher)
'watch-file fw "test.sc" (EventKind.MODIFIED)
    fn "callback" ()
        try
            update-shader;
            ;
        except (ex)
            'dump ex

global last-frame-time : f32
while (not (window.closed?))
    window.poll-events;
    'poll-events fw

    let wwidth wheight = (window.size)
    _gl.Viewport 0 0 wwidth wheight

    let current-time = ((glfw.GetTime) as f32)
    let delta-time = (current-time - last-frame-time)
    last-frame-time = current-time

    # update uniforms
    using import glm
    _gl.Uniform3f uniforms.iResolution (wwidth as f32) (wheight as f32) 1
    _gl.Uniform1f uniforms.iTime current-time
    _gl.Uniform1f uniforms.iTimeDelta delta-time

    gl.clear 0.017 0.017 0.017 1.0
    _gl.DrawArrays _gl.GL_TRIANGLES 0 6
    window.flip;
