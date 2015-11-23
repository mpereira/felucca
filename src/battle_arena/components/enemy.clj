(ns src.battle-arena.components.enemy
  (:use arcadia.core)
  (:require [src.battle-arena.vector3 :refer :all])
  (:import [UnityEngine Debug Color Camera GUI Rect Screen Vector3 Texture2D
            ScaleMode TextureFormat]))

(defcomponent Enemy [^GameObject player-hero
                     ^float aggressiveness-radius]
  (Awake [this])
  (Start [this])
  (Update [this]
    (when player-hero
      (.. this transform (LookAt (.. player-hero transform position))))))

;; FIXME: trying to `import` types provided by `defcomponent` result in:
;;
;; System.NullReferenceException: Object reference not set to an instance of an
;; object
(def Component Enemy)
