#
    // "ShaderToy Tutorial - Ray Marching for Dummies!"
    // by Martijn Steinrucken aka BigWings/CountFrolic - 2018
    // License Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.
    //
    // This shader is part of a tutorial on YouTube
    // https://youtu.be/PGtv-dBi2wE

    Scopes version by Westerbly (radgeRayden) Snaydley.
let MAX_STEPS = 100
let MAX_DIST = 100
let SURF_DIST = .01

fn GetDist (p)
    let s = (vec4 0 1 6 0.25)
    let plane-dist = p.y
    sphere-dist := (length (p - s.xyz)) - s.w

    let d = (min sphere-dist plane-dist)

fn RayMarch (ro rd)
    fold (d0 = 0:f32) for i in (range MAX_STEPS)
        p := ro + rd * d0
        let dS = (GetDist p)
        if ((d0 > MAX_DIST) or (dS < SURF_DIST))
            return d0
        else
            d0 + dS

fn GetNormal (p)
    let d = (GetDist p)
    let _e = (vec2 .01 0)
    normalize
        d - (vec3 (GetDist (p - _e.xyy)) (GetDist (p - _e.yxy)) (GetDist (p - _e.yyx)))

fn GetLight (p)
    local light-pos = (vec3 0 5 6)
    light-pos.xz += (vec2 (sin iTime) (cos iTime)) * 2
    let l = (normalize (light-pos - p))
    let n = (GetNormal p)
    local dif = (clamp (dot n l) 0:f32 1:f32)
    let d = (RayMarch (p + n * SURF_DIST * 2) l)
    if (d < (length (light-pos - p)))
        dif *= .1
    dif

UV := (fragCoord - 0.5 * iResolution.xy) / iResolution.x
local col : vec3
let ro = (vec3 0 1 0)
let rd = (normalize (vec3 UV 1))
let d = (RayMarch ro rd)
p :=  ro + rd * d
let dif = (GetLight p)
col = (pow (vec3 dif) (vec3 .4545)) # gamma correction

fragColor = (vec4 col 1)
;