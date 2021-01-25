load-library (module-dir .. "/../libgame.so")
load-library "libglfw.so"
run-stage;

import .window
import .gl
let _gl = (import .FFI.glad)

window.init;
gl.init;

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

            gl_Position = (vec4 (quad @ gl_VertexID) 0 1)

        fn frag ()
            out fcolor : vec4
                location = 0
            fcolor = (vec4 1)

        _ vertex frag

global shader-program =
    gl.GPUShaderProgram default-vshader default-fshader

_gl.UseProgram shader-program

while (not (window.closed?))
    window.poll-events;
    gl.clear 0.017 0.017 0.017 1.0
    _gl.DrawArrays _gl.GL_TRIANGLES 0 6
    window.flip;
