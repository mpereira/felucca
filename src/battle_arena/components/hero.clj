(ns src.battle-arena.components.hero
  (:use arcadia.core)
  (:require [src.battle-arena.vector3 :refer :all])
  (:import [UnityEngine Debug Color Camera GUI Rect Screen Vector3 Texture2D
            ScaleMode TextureFormat]))

(defn strength [this] (.strength this))
(defn hit-points [this] (.hit-points this))
(defn attack-speed [this] (.attack-speed this))
(defn movement-speed [this] (.movement-speed this))
(defn rotation-speed [this] (.rotation-speed this))

(defn max-hit-points [this]
  (strength this))

(defn hit-points-percentage [this]
  (/ (hit-points this) (max-hit-points this)))

(defn dead? [this]
  (>= 0 (hit-points this)))

(def alive? (complement dead?))

(defn decrement-hit-points! [this value]
  (set! (.hit-points this) (float (- (hit-points this) (max 0 value)))))

(defn increment-hit-points! [this value]
  (set! (.hit-points this) (float (+ (hit-points this)
                                     (min (max-hit-points this) value)))))

(defn receive-hit! [this hit]
  (decrement-hit-points! this hit))

(defn awake! [this]
  (set! (.hit-points-bar-texture-background this)
        (Texture2D. 1 1 (TextureFormat/RGB24) false))
  (.SetPixel (.hit-points-bar-texture-background this) 0 0 (Color/black))
  (.Apply (.hit-points-bar-texture-background this))
  (set! (.hit-points-bar-texture this) (Texture2D. 1 1 (TextureFormat/RGB24) false))
  (.SetPixel (.hit-points-bar-texture this) 0 0 (Color/red))
  (.Apply (.hit-points-bar-texture this)))

(defn start! [this]
  (set! (.hit-points this) (strength this)))

(defn on-gui! [this]
  (let [transform (.transform this)
        local-position (.localPosition transform)
        size (.. transform renderer bounds size)
        view-position (.. Camera main (WorldToScreenPoint local-position))
        border 2
        background-width 80
        width (* background-width (hit-points-percentage this))
        height 6
        x (- (.x view-position) (/ background-width 2))
        y (- (Screen/height) (.y view-position) (* 25 (.y size)))]
    (.. GUI (DrawTexture (Rect. (- x border)
                                (- y border)
                                (+ background-width (* 2 border))
                                (+ height (* 2 border)))
                         (.hit-points-bar-texture-background this)
                         (ScaleMode/StretchToFill)))
    (.. GUI (DrawTexture (Rect. x y width height)
                         (.hit-points-bar-texture this)
                         (ScaleMode/StretchToFill)))))

(defcomponent Hero [^float strength
                    ^float hit-points
                    ^float attack-speed
                    ^float movement-speed
                    ^float rotation-speed
                    ^Texture2D hit-points-bar-texture-background
                    ^Texture2D hit-points-bar-texture]
  (Awake [this] (awake! this))
  (Start [this] (start! this))
  (OnGUI [this] (on-gui! this)))

;; FIXME: trying to `import` types provided by `defcomponent` result in:
;;
;; System.NullReferenceException: Object reference not set to an instance of an
;; object
(def Component Hero)
(defn component [that] (.GetComponent that Component))
