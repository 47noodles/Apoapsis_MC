# SPDX-FileCopyrightText: 2026 The Apoapsis contributors
# SPDX-License-Identifier: MIT
"""Compact-preset numbers for the Meridian system: LEO periods, SOIs, Hohmann transfers, ascent dv.
Run: python tools/calc/system.py"""
import math
G0=9.81
# Compact preset. Star GM chosen so the homeworld year is 4 h of real time.
T_home=4*3600
a_home=20_000e3
GMs=4*math.pi**2*a_home**3/T_home**2
print(f"Star GM = {GMs:.3e} m^3/s^2")
# name, R km, g (home g), a km (star) or parent+a, atm top km, surface atm, designed SOI km
P=[
 ("Cinder",   38, 0.55, 11_000, None, 0, 0.0, 260),
 ("Verdance", 64, 1.00, 20_000, None, 12, 1.0, 900),
 ("Ochre",    48, 0.42, 30_000, None, 7, 0.08, 520),
 ("Thalassa", 72, 1.20, 42_000, None, 18, 2.4, 900),
 ("Tempest", 420, 2.10, 80_000, None, 140, 0, 9000),
 ("Hollow",   30, 0.30, 125_000, None, 0, 0.0, 400),
]
M=[ # moons: name, R km, g, parent, orbit radius km, atm top, atm
 ("Pale",   20, 0.16, "Verdance", 320, 0, 0.0),
 ("Rook",    4, 0.012,"Ochre",    150, 0, 0.0),
 ("Pawn",  2.5, 0.008,"Ochre",    230, 0, 0.0),
 ("Glacis", 26, 0.13, "Tempest", 2400, 0, 0.0),
 ("Murk",   32, 0.14, "Tempest", 3600, 9, 1.5),
 ("Ember",  22, 0.18, "Tempest", 1500, 0, 0.0),
]
bodies={}
def gm(R,g): return g*G0*(R*1e3)**2
def fmt_t(s):
    if s<3600: return f"{s/60:.1f} min"
    if s<86400*2: return f"{s/3600:.1f} h"
    return f"{s/86400:.1f} d"
print("\n| Body | R (km) | g | GM | LEO alt (km) | v_LEO (m/s) | LEO period | v_esc (m/s) | year/period | SOI design (km) | SOI Laplace (km) |")
print("|---|---|---|---|---|---|---|---|---|---|---|")
for n,R,g,a,_,atm,p,soi in P:
    GM=gm(R,g); bodies[n]=(GM,R,a*1e3)
    leo_alt=max(atm*1.25, R*0.1) if n!="Tempest" else atm*1.15
    r=(R+leo_alt)*1e3
    v=math.sqrt(GM/r); T=2*math.pi*r/v; ve=math.sqrt(2*GM/(R*1e3))
    Ty=2*math.pi*math.sqrt((a*1e3)**3/GMs)
    lap=a*(GM/GMs)**0.4
    print(f"| {n} | {R} | {g} | {GM:.2e} | {leo_alt:.0f} | {v:.0f} | {fmt_t(T)} | {ve:.0f} | {fmt_t(Ty)} | {soi} | {lap:.0f} |")
for n,R,g,par,orb,atm,p in M:
    GM=gm(R,g); GMp,Rp,_=bodies[par]
    leo_alt=max(atm*1.25,R*0.15); r=(R+leo_alt)*1e3
    v=math.sqrt(GM/r); T=2*math.pi*r/v; ve=math.sqrt(2*GM/(R*1e3))
    To=2*math.pi*math.sqrt((orb*1e3)**3/GMp)
    lap=orb*(GM/GMp)**0.4
    ang=2*math.degrees(math.atan(R/(orb-Rp)))
    print(f"| {n} (moon of {par}) | {R} | {g} | {GM:.2e} | {leo_alt:.0f} | {v:.0f} | {fmt_t(T)} | {ve:.0f} | {fmt_t(To)} | - | {lap:.0f} | sky size from parent surface {ang:.1f} deg")

print("\nHohmann transfers between planets (from LEO, ignoring inclination):")
def leo(n):
    GM,R,a=bodies[n]
    alt={"Cinder":4,"Verdance":15,"Ochre":9,"Thalassa":22,"Tempest":161,"Hollow":3}[n]
    return GM,(R+alt)*1e3,a
print("| From -> To | transfer time | departure dv from LEO (m/s) | capture dv to LEO (m/s) | synodic period |")
print("|---|---|---|---|---|")
for f,t in [("Verdance","Cinder"),("Verdance","Ochre"),("Verdance","Thalassa"),("Ochre","Thalassa"),("Verdance","Tempest"),("Thalassa","Tempest"),("Tempest","Hollow"),("Verdance","Hollow")]:
    GM1,r1,a1=leo(f); GM2,r2,a2=leo(t)
    at=(a1+a2)/2
    tt=math.pi*math.sqrt(at**3/GMs)
    v1=math.sqrt(GMs/a1); v2=math.sqrt(GMs/a2)
    vp=math.sqrt(GMs*(2/a1-1/at)); va=math.sqrt(GMs*(2/a2-1/at))
    vinf1=abs(vp-v1); vinf2=abs(v2-va)
    dv1=math.sqrt(vinf1**2+2*GM1/r1)-math.sqrt(GM1/r1)
    dv2=math.sqrt(vinf2**2+2*GM2/r2)-math.sqrt(GM2/r2)
    T1=2*math.pi*math.sqrt(a1**3/GMs); T2=2*math.pi*math.sqrt(a2**3/GMs)
    syn=1/abs(1/T1-1/T2)
    print(f"| {f} -> {t} | {fmt_t(tt)} | {dv1:.0f} | {dv2:.0f} | {fmt_t(syn)} |")

print("\nLocal transfers:")
GM,R,_=bodies["Verdance"]
r1=(64+15)*1e3; r2=320e3
at=(r1+r2)/2; tt=math.pi*math.sqrt(at**3/GM)
dv=math.sqrt(GM*(2/r1-1/at))-math.sqrt(GM/r1)
print(f"Verdance LEO -> Pale: {fmt_t(tt)}, TLI dv {dv:.0f} m/s")
# ascent dv estimate: v_LEO + gravity/drag losses (rule of thumb)
for n,R,g,a,_,atm,p,soi in P[:4]:
    GM=gm(R,g); r=(R+max(atm*1.25,R*0.1))*1e3
    v=math.sqrt(GM/r)
    loss = 1.0*g*G0*90 + (250 if p>0.5 else 60*p/0.08 if p>0 else 0)  # ~90 s gravity loss at TWR~1.6 plus drag
    print(f"{n}: ascent to LEO ~ {v+loss*0.6:.0f}-{v+loss:.0f} m/s")
# real-scale check
print("\nReal-scale cube face edge for Earth radius:", math.pi*6371e3/2/1e6, "million blocks")
print("Verdance cube face edge:", math.pi*64e3/2/1e3, "thousand blocks; surface area equiv square side:", math.sqrt(4*math.pi*(64e3)**2)/1e3, "km")

print("\nDerived figures quoted in design/07 and design/14:")
g = 9.81
GMv, Rv, av = bodies["Verdance"]
GMpale = gm(20, 0.16)
r_pale_orbit = 320e3
# Pale capture into a 3 km low orbit, arriving on the Hohmann transfer from Verdance LEO
r1 = (64 + 15) * 1e3
at = (r1 + r_pale_orbit) / 2
v_arr = math.sqrt(GMv * (2 / r_pale_orbit - 1 / at))
v_pale = math.sqrt(GMv / r_pale_orbit)
vinf = abs(v_pale - v_arr)
rl = (20 + 3) * 1e3
cap = math.sqrt(vinf**2 + 2 * GMpale / rl) - math.sqrt(GMpale / rl)
T_leo = 2 * math.pi * math.sqrt(r1**3 / GMv)
T_pale = 2 * math.pi * math.sqrt(r_pale_orbit**3 / GMv)
print(f"Pale capture to 3 km LEO: {cap:.0f} m/s; phasing window every {1/abs(1/T_leo-1/T_pale)/60:.1f} min")
# Tides on Verdance (20-min day) from Pale
T_rot = 20 * 60
lunar_day = 1 / (1 / T_rot - 1 / T_pale)
T_year = 2 * math.pi * math.sqrt(av**3 / GMs)
phase = 1 / (1 / T_pale - 1 / T_year)
print(f"Tides: lunar day {lunar_day/60:.1f} min, semidiurnal tide every {lunar_day/120:.1f} min, Pale phase cycle {phase/3600:.2f} h")
print(f"Verdance seen from Pale: {2*math.degrees(math.atan(64/(320-20))):.1f} deg; Pale from Verdance vs Earth's Moon (0.527 deg): x{8.9/0.527:.1f}")
# Hollow inner drum: 0.3 g at r = 28 km
r_drum = 28e3
print(f"Hollow drum period for 0.3 g at 28 km: {2*math.pi*math.sqrt(r_drum/(0.3*g))/60:.1f} min")
# Breakup periods (spin where equatorial centrifugal acceleration equals surface gravity)
for n, R, gg in [("Tempest", 420, 2.10), ("Verdance", 64, 1.0)]:
    print(f"{n} breakup spin period: {2*math.pi*math.sqrt(R*1e3/(gg*g))/60:.1f} min")
# Hill radii
for n, R, gg, a, *_ in P:
    GM = gm(R, gg)
    print(f"Hill radius {n}: {a*(GM/(3*GMs))**(1/3):.0f} km")
# Cinder solar intensity vs Verdance (inverse square)
print(f"Cinder solar intensity vs Verdance: x{(20000/11000)**2:.2f}")
# Wanderer: e = 0.8, period 2 days
Tw = 2 * 86400
aw = (GMs * Tw**2 / (4 * math.pi**2)) ** (1 / 3)
print(f"Wanderer: a = {aw/1e3:,.0f} km, perihelion = {aw*(1-0.8)/1e3:,.0f} km")
# Torch transfer Verdance -> Tempest at 0.2 g constant thrust, flip at midpoint (straight-line approximation)
d = (80000 - 20000) * 1e3
acc = 0.2 * g
print(f"Torch transfer Verdance->Tempest at 0.2 g: about {2*math.sqrt(d/acc)/3600:.1f} h (Hohmann 7.9 h)")
