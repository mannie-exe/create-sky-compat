# Disposable fixture only: replaces the platform at y=100. Restart for startup-switch comparisons.
gamerule doDaylightCycle false
time set 9000
weather clear
gamemode spectator @s
forceload add -16 -16 16 16
fill -16 100 -16 16 100 16 minecraft:white_concrete
kill @e[tag=ui_shadow_fixture]
fill -1 101 -1 1 104 1 minecraft:stone
summon minecraft:text_display -7 104 0 {Tags:["ui_shadow_fixture"],text:'{"text":"TEXT DISPLAY","color":"white"}',background:-14671840,billboard:"center",line_width:120,brightness:{block:15,sky:15},transformation:{translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],scale:[5f,5f,5f]}}
summon minecraft:cow 6 101 0 {Tags:["ui_shadow_fixture"],NoAI:1b,Silent:1b,Invulnerable:1b,PersistenceRequired:1b,CustomName:'{"text":"COW NAME"}',CustomNameVisible:1b}
summon minecraft:block_display 0 103 6 {Tags:["ui_shadow_fixture"],block_state:{Name:"minecraft:gold_block"}}
summon minecraft:item_display 0 103 -6 {Tags:["ui_shadow_fixture"],item:{id:"minecraft:diamond",count:1},transformation:{translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],scale:[2f,2f,2f]}}
setblock -5 101 6 minecraft:oak_sign[rotation=8]{front_text:{messages:['{"text":"SIGN"}','""','""','""']}}
tp @s 12.0 106.0 16.0 143.0 17.0
