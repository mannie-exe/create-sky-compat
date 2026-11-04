# Marked disposable world only: owns x/z=-16..16, y=280..296.
gamemode spectator @s
gamerule sendCommandFeedback false
gamerule doDaylightCycle false
gamerule doWeatherCycle false
gamerule doMobSpawning false
gamerule doMobLoot false
gamerule doEntityDrops false
time set 3000
weather clear
forceload add -16 -16 16 16
kill @e[tag=world_text_review]
fill -16 280 -16 16 280 16 white_concrete
fill -16 281 -16 16 296 16 air
summon minecraft:text_display 0 284 0 {Tags:["world_text_review"],text:'[{"text":"RED\\n","color":"red"},{"text":"GREEN\\n","color":"green"},{"text":"BLUE\\n","color":"blue"},{"text":"WHITE","color":"white"}]',background:-16777216,billboard:"center",line_width:200,brightness:{block:15,sky:15},transformation:{translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],scale:[4f,4f,4f]}}
summon minecraft:text_display 0 284 -7 {Tags:["world_text_review"],text:'[{"text":"BLACK\\n","color":"black"},{"text":"RED\\n","color":"red"},{"text":"BLUE","color":"blue"}]',background:-1,billboard:"center",line_width:200,brightness:{block:15,sky:15},shadow:1b,transformation:{translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],scale:[4f,4f,4f]}}
summon minecraft:text_display 0 284 7 {Tags:["world_text_review"],text:'[{"text":"RED\\n","color":"red"},{"text":"GREEN\\n","color":"green"},{"text":"BLUE\\n","color":"blue"},{"text":"WHITE","color":"white"}]',background:1073741824,billboard:"center",line_width:200,transformation:{translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],scale:[4f,4f,4f]}}
summon minecraft:text_display 0 284 -14 {Tags:["world_text_review"],text:'[{"text":"THROUGH","color":"aqua"}]',background:1073741824,billboard:"center",line_width:200,brightness:{block:15,sky:15},see_through:1b,transformation:{translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],scale:[4f,4f,4f]}}
summon minecraft:cow 0 281 4 {Tags:["world_text_review"],NoAI:1b,Silent:1b,Invulnerable:1b,PersistenceRequired:1b,CustomName:'{"text":"RED NAME","color":"red"}',CustomNameVisible:1b}
setblock 0 281 -4 stone
setblock 0 281 10 minecraft:oak_sign[rotation=4]{front_text:{messages:['{"text":"SIGN","color":"red"}','""','""','""']}}
summon minecraft:block_display 0 282 -12 {Tags:["world_text_review"],block_state:{Name:"minecraft:gold_block"}}
summon minecraft:item_display 0 282 12 {Tags:["world_text_review"],item:{id:"minecraft:diamond",count:1}}
tp @s -10 284 0 -90 0
