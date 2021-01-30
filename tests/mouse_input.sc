# Created by inigo quilez - iq/2013
# License Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.

# Shows how to use the mouse input (only left button supported):
#
#      mouse.xy  = mouse position during last button down
#  abs(mouse.zw) = mouse position during last button click
# sign(mouze.z)  = button is down
# sign(mouze.w)  = button is clicked

# Source: https://www.shadertoy.com/view/Mss3zH
# Translated to Scopes by Westerbly (radgeRayden) Snaydley

fn distanceToSegment (a b p)
    let pa = (p - a)
    let ba = (b - a)
    let h = (clamp ((dot pa ba) / (dot ba ba)) 0.0 1.0)
    length (pa - ba * h)

p   := fragCoord / iResolution.x
cen := 0.5 * iResolution.xy / iResolution.x
m   := iMouse / iResolution.x
local col : vec3

if (m.z > 0.0) # button is down
    let d = (distanceToSegment m.xy (abs m.zw) p)
    col = (mix col (vec3 1 1 0) (1.0 - (smoothstep .004 .008 d)))

if (m.w > 0.0) # button click
    col = (mix col (vec3 1) (1.0 - (smoothstep 0.1 0.105 (length (p - cen)))))

col = (mix col (vec3 1 0 0) (1.0 - (smoothstep .03 .035 (length (p - m.xy)))))
col = (mix col (vec3 0 0 1) (1.0 - (smoothstep .03 .035 (length (p - (abs m.zw))))))

fragColor = (vec4 col 1)