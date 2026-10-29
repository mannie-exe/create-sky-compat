# Only for the disposable copy of Resource Review. Replaces this bounded test pad.
gamemode spectator @s
gamerule sendCommandFeedback true
gamerule doDaylightCycle false
gamerule doWeatherCycle false
weather clear
time set 3000
sable paused true
fill 288 180 -12 344 180 20 minecraft:smooth_stone
fill 298 181 -2 338 202 10 minecraft:air
fill 300 190 0 306 190 6 minecraft:oak_planks
fill 300 193 0 306 199 6 minecraft:white_wool hollow
fill 301 193 1 305 193 5 minecraft:air
setblock 303 191 3 aeronautics:adjustable_burner{ScrollValue:100}
setblock 303 190 3 minecraft:redstone_block
fill 300 191 0 300 192 0 minecraft:oak_fence
fill 306 191 6 306 192 6 minecraft:oak_fence
setblock 300 191 6 minecraft:gold_block
setblock 301 191 6 minecraft:iron_block
setblock 302 191 6 minecraft:copper_block
setblock 303 191 6 minecraft:emerald_block
setblock 304 191 6 minecraft:redstone_block
setblock 305 191 6 minecraft:glowstone
setblock 306 191 6 minecraft:glass
clone 300 190 0 306 199 6 316 190 0
setblock 332 191 2 create:copycat_panel[facing=south]{Material:{Name:"minecraft:gold_block"},Item:{id:"minecraft:gold_block",count:1}}
setblock 334 191 2 create:copycat_panel[facing=south]{Material:{Name:"minecraft:gold_block"},Item:{id:"minecraft:gold_block",count:1}}
tp @s 311 194 22 180 6
