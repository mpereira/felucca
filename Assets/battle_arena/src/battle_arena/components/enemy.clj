(ns battle-arena.components.enemy
  (:use arcadia.core)
  (:require [battle-arena.vector3 :refer :all]
            [battle-arena.components.hero :as hero])
  (:import [UnityEngine Debug Color Camera GUI Rect Screen Vector3 Texture2D
            ScaleMode TextureFormat]))

(defn look-at-player-hero! [game-object]
  (let [enemy (state game-object :enemy)]
    (when (:player-hero enemy)
      (.. game-object transform (LookAt (.. (:player-hero enemy) transform position))))))

(defn update! [game-object role-key]
  (let [enemy (state game-object :enemy)]
    (when-not (hero/dead? game-object)
      (look-at-player-hero! game-object))))

(defmutable Enemy [^GameObject player-hero
                   ^float aggressiveness-radius])

(def hooks
  {:update #'update!})
