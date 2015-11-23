(ns src.battle-arena.components.hero
  (:use arcadia.core)
  (:require [src.battle-arena.vector3 :refer :all])
  (:import [UnityEngine Debug Color Camera GUI Rect Screen Vector3 Texture2D
            ScaleMode TextureFormat]))

(defn start! [this]
  (set! (.. this hit-points)
        (.. this strength)))

(defn max-hit-points [this]
  (.strength this))

(defn hit-points-percentage [this]
  (/ (.-hit-points this) (max-hit-points this)))

(defcomponent Hero [^float strength
                    ^float hit-points
                    ^float attack-speed
                    ^float movement-speed
                    ^float rotation-speed
                    ^Texture2D hit-points-bar-texture-background
                    ^Texture2D hit-points-bar-texture]
  (Awake [this]
    (set! hit-points-bar-texture-background
          (Texture2D. 1 1 (TextureFormat/RGB24) false))
    (.SetPixel hit-points-bar-texture-background 0 0 (Color/black))
    (.Apply hit-points-bar-texture-background)
    (set! hit-points-bar-texture (Texture2D. 1 1 (TextureFormat/RGB24) false))
    (.SetPixel hit-points-bar-texture 0 0 (Color/red))
    (.Apply hit-points-bar-texture))
  (Start [this] (start! this))
  (Update [this])
  (OnGUI [this]
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
                           hit-points-bar-texture-background
                           (ScaleMode/StretchToFill)))
      (.. GUI (DrawTexture (Rect. x y width height)
                           hit-points-bar-texture
                           (ScaleMode/StretchToFill))))))

;; FIXME: trying to `import` types provided by `defcomponent` result in:
;;
;; System.NullReferenceException: Object reference not set to an instance of an
;; object
(def Component Hero)
