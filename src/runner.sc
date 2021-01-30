load-library (module-dir .. "/../libgame.so")
load-library "libglfw.so"

let name argc argv = (script-launch-args)
assert (argc > 0) "no source file provided"
let srcpath = (string (argv @ 0))

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
wrapper.init;

fn update-callback ()
    try (wrapper.update-shader srcpath)
    except (ex) ('dump ex)
update-callback;
glfw.SetTime 0:f64

using file-watcher
global fw = (FileWatcher)
'watch-file fw srcpath (EventKind.MODIFIED) update-callback

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
    let uniforms =
        wrapper.Uniforms
            iResolution = (vec3 wwidth wheight 1)
            iTime = current-time
            iTimeDelta = delta-time
            iFrame = (frame-count as f32)
            iMouse = (vec4 mouse-current-drag mouse-drag-start)
            iDate = (vec4 (unpack cur-date))
    wrapper.update-uniforms uniforms

    # =================================================================================
    gl.clear 0.017 0.017 0.017 1.0
    _gl.DrawArrays _gl.GL_TRIANGLES 0 6
    window.flip;
