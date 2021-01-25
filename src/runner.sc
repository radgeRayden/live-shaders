load-library (module-dir .. "/../libgame.so")
load-library "libglfw.so"
run-stage;

import .window
import .gl

window.init;
gl.init;

let default-vshader default-fshader =
    do
        using import glsl
        using import glm
        fn vertex ()
            let idx = gl_VertexID
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

while (not (window.closed?))
    window.poll-events;
    gl.clear 0.017 0.017 0.017 1.0
    window.flip;
