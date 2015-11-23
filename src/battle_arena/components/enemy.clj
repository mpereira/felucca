(ns src.battle-arena.components.enemy
  (:use arcadia.core)
  (:require [src.battle-arena.vector3 :refer :all]
            [src.battle-arena.components.hero :as hero])
  (:import [UnityEngine Debug Color Camera GUI Rect Screen Vector3 Texture2D
            ScaleMode TextureFormat]))

(defn look-at-player-hero! [this]
  (when (.player-hero this)
      (.. this transform (LookAt (.. this player-hero transform position)))))

(defn update! [this]
  (when-not (hero/dead? (hero/component this))
    (look-at-player-hero! this)))

(defcomponent Enemy [^GameObject player-hero
                     ^float aggressiveness-radius]
  (Update [this] (update! this)))

;; FIXME: trying to `import` types provided by `defcomponent` result in:
;;
;; System.NullReferenceException: Object reference not set to an instance of an
;; object
(def Component Enemy)
(defn component [that] (.GetComponent that Component))
