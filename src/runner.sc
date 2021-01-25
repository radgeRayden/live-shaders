load-library (module-dir .. "/../libgame.so")
load-library "libglfw.so"
run-stage;

import .window
import .gl
import .wrapper

let file-watcher = (import .radlib.file-watcher)
let _gl = (import .FFI.glad)

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

            uniform iResolution : vec3
            out fragCoord : vec2
            gl_Position = (vec4 (quad @ gl_VertexID) 0 1)

        fn frag ()
            out fcolor : vec4
                location = 0
            fcolor = (vec4 1)

        _ vertex frag

global shader-program =
    gl.GPUShaderProgram default-vshader default-fshader

fn update-shader ()
    let frag = (wrapper.wrap-shader "test" "test.sc" shader-scope)
    shader-program = (gl.GPUShaderProgram default-vshader frag)
    _gl.UseProgram shader-program

update-shader;

using file-watcher
global fw = (FileWatcher)
'watch-file fw "test.sc" (EventKind.MODIFIED)
    fn "callback" ()
        try
            update-shader;
            ;
        except (ex)
            'dump ex

while (not (window.closed?))
    window.poll-events;
    'poll-events fw
    gl.clear 0.017 0.017 0.017 1.0
    _gl.DrawArrays _gl.GL_TRIANGLES 0 6
    window.flip;
