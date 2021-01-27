load-library (module-dir .. "/../libgame.so")
load-library "libglfw.so"
run-stage;

using import struct
using import glm
import .window
import .gl
import .wrapper
import .date

let file-watcher = (import .radlib.file-watcher)
let _gl = (import .FFI.glad)
let glfw = (import .FFI.glfw)

window.init;
gl.init;

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

global shader-program = (wrapper.default-shader)
_gl.UseProgram shader-program

global uniforms :
    struct UniformLocations
        iResolution : i32
        iTime : i32
        iTimeDelta : i32
        iFrame : i32
        iMouse : i32
        iDate : i32

fn update-shader ()
    shader-program = (wrapper.wrap-shader "test.sc" shader-scope)

    _gl.UseProgram shader-program
    uniforms.iResolution =
        _gl.GetUniformLocation shader-program "iResolution"
    uniforms.iTime =
        _gl.GetUniformLocation shader-program "iTime"
    uniforms.iTimeDelta =
        _gl.GetUniformLocation shader-program "iTimeDelta"
    uniforms.iFrame =
        _gl.GetUniformLocation shader-program "iFrame"
    uniforms.iMouse =
        _gl.GetUniformLocation shader-program "iMouse"
    uniforms.iDate =
        _gl.GetUniformLocation shader-program "iDate"

fn update-callback ()
    try (update-shader)
    except (ex) ('dump ex)
update-callback;
glfw.SetTime 0:f64

using file-watcher
global fw = (FileWatcher)
'watch-file fw "test.sc" (EventKind.MODIFIED) update-callback

global last-frame-time : f32
global frame-count : u32
global mouse-drag-start : vec2 -999 -999
global mouse-current-drag : vec2 -999 -999

fn mouse-position ()
    let wwidth wheight = (window.size)
    local mousex : f64
    local mousey : f64
    glfw.GetCursorPos window.main-window &mousex &mousey
    # adjust for 0,0 at bottom-left
    mousey = ((wheight as f64) - mousey)

    mousex = (clamp (mousex as f32) 0.0 (wwidth as f32))
    mousey = (clamp (mousey as f32) 0.0 (wheight as f32))
    _ mousex mousey

glfw.SetMouseButtonCallback window.main-window
    fn "mouse-button-callback" (window button action mods)
        if ((button == glfw.GLFW_MOUSE_BUTTON_1) and (action == glfw.GLFW_PRESS))
            mouse-drag-start = (vec2 (mouse-position))

while (not (window.closed?))
    # reset "mouse pressed" event after end of previous frame
    # the callback that changes the mouse state is called in poll-events
    mouse-drag-start.y = (- (abs mouse-drag-start.y))

    window.poll-events;
    'poll-events fw

    let wwidth wheight = (window.size)
    _gl.Viewport 0 0 wwidth wheight

    # update input data
    let current-time = ((glfw.GetTime) as f32)
    let delta-time = (current-time - last-frame-time)
    last-frame-time = current-time
    frame-count += 1

    let mouse-button-state = (glfw.GetMouseButton window.main-window glfw.GLFW_MOUSE_BUTTON_1)
    if (mouse-button-state == glfw.GLFW_PRESS)
        mouse-current-drag = (vec2 (mouse-position))
    else
        # set "mouse down" status
        mouse-drag-start.x = (- (abs mouse-drag-start.x))

    let cur-date = (date.get-date)

    # update uniforms
    # ================================================================================
    using import glm
    _gl.Uniform3f uniforms.iResolution (wwidth as f32) (wheight as f32) 1
    _gl.Uniform1f uniforms.iTime current-time
    _gl.Uniform1f uniforms.iTimeDelta delta-time
    _gl.Uniform1f uniforms.iFrame (frame-count as f32)

    _gl.Uniform4f uniforms.iMouse
        mouse-current-drag.x
        mouse-current-drag.y
        mouse-drag-start.x
        mouse-drag-start.y

    _gl.Uniform4f uniforms.iDate
        cur-date.year as f32
        cur-date.month as f32
        cur-date.day as f32
        cur-date.second

    # =================================================================================
    gl.clear 0.017 0.017 0.017 1.0
    _gl.DrawArrays _gl.GL_TRIANGLES 0 6
    window.flip;
