# Run once per fresh scene; pause again after every client restart.
sable paused true
setblock 332 191 2 minecraft:air
setblock 334 191 2 minecraft:air
setblock 332 191 2 create:copycat_panel[facing=south]{Material:{Name:"minecraft:glass"},Item:{id:"minecraft:glass",count:1}}
setblock 334 191 2 create:copycat_panel[facing=south]{Material:{Name:"minecraft:glass"},Item:{id:"minecraft:glass",count:1}}
setblock 336 191 2 minecraft:glass
setblock 338 191 2 minecraft:glass
sable assemble area 332 191 2 332 191 2
sable assemble area 336 191 2 336 191 2
tp @s 335 191 10 180 0
