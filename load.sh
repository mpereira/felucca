#!/bin/bash

PROGRAM="$(cat \
  src/battle_arena/vector3.clj \
  src/battle_arena/components/hero.clj \
  src/battle_arena/components/enemy.clj \
  src/battle_arena/components/player_input.clj \
  src/battle_arena/components/rts_camera.clj \
  src/battle_arena/utils.clj \
  src/battle_arena/core.clj \
)"

ruby Assets/Arcadia/Editor/repl-client.rb <<< "$PROGRAM"
