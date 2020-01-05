(ns battle-arena.components.hit-points-bar
  (:require [arcadia.core :refer [defmutable log update-state state log with-cmpt]]
            [battle-arena.vector3 :as v3]
            [battle-arena.components.creature :as creature])
  (:import [UnityEngine Debug Color Camera GUI Rect Screen Vector3 Texture2D
            Renderer ScaleMode TextureFormat]))

(defmutable HitPointsBar [^Texture2D hit-points-bar-texture-background
                          ^Texture2D hit-points-bar-texture])

(defn on-gui! [^GameObject this role-key]
  (let [hit-points-bar-state (state this :hit-points-bar)
        transform (.transform this)
        local-position (.localPosition transform)
        size (with-cmpt this [r Renderer]
               (.. r bounds size))
        view-position (.. Camera main (WorldToScreenPoint local-position))
        border 2
        background-width 80
        width (* background-width (creature/hit-points-percentage this))
        height 6
        x (- (.x view-position) (/ background-width 2))
        y (- (Screen/height) (.y view-position) (* 25 (.y size)))]
    (.. GUI (DrawTexture (Rect. (- x border)
                                (- y border)
                                (+ background-width (* 2 border))
                                (+ height (* 2 border)))
                         (:hit-points-bar-texture-background hit-points-bar-state)
                         (ScaleMode/StretchToFill)))
    (.. GUI (DrawTexture (Rect. x y width height)
                         (:hit-points-bar-texture hit-points-bar-state)
                         (ScaleMode/StretchToFill)))))

(def hooks
  {:on-gui #'on-gui!})
