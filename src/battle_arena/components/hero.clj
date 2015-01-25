(ns src.battle-arena.components.hero
  (:use arcadia.core
        src.battle-arena.vector3)
  (:import [UnityEngine
            Collision]))

(defcomponent Hero [^float movement-speed]
  (Awake [this])
  (Start [this])
  (Update [this])
  (OnCollisionEnter [this collision]))
