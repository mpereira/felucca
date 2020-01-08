(ns felucca.components.hit-points-bar
  (:require [arcadia.core :refer [defmutable log update-state state log with-cmpt]]
            [felucca.vector3 :as v3]
            [felucca.components.creature :as creature])
  (:import [UnityEngine Debug Color Camera GUI Rect Screen Vector3 Texture2D
            Renderer ScaleMode TextureFormat]))

(defmutable HitPointsBar [^Texture2D hit-points-bar-texture-background
                          ^Texture2D hit-points-bar-texture
                          ^Texture2D hit-points-bar-texture-border])

(defn start! [^GameObject this role-key]
  (let [hit-points-bar-texture (Texture2D. 1 1 (TextureFormat/RGB24) false)
        hit-points-bar-texture-background (Texture2D. 1 1 (TextureFormat/RGB24) false)
        hit-points-bar-texture-border (Texture2D. 1 1 (TextureFormat/RGB24) false)]
    (.SetPixel hit-points-bar-texture-background 0 0 (Color/red))
    (.Apply hit-points-bar-texture-background)
    (.SetPixel hit-points-bar-texture 0 0 (Color/blue))
    (.Apply hit-points-bar-texture)
    (.SetPixel hit-points-bar-texture-border 0 0 (Color/gray))
    (.Apply hit-points-bar-texture-border)
    (update-state this
                  :hit-points-bar
                  (fn [hit-points-bar-state]
                    (assoc hit-points-bar-state
                           :hit-points-bar-texture-background
                           hit-points-bar-texture-background
                           :hit-points-bar-texture
                           hit-points-bar-texture
                           :hit-points-bar-texture-border
                           hit-points-bar-texture-border)))))

(defn on-gui! [^GameObject this role-key]
  (let [hit-points-bar-state (state this :hit-points-bar)
        transform (.transform this)
        local-position (.localPosition transform)
        size (with-cmpt this [r Renderer]
               (.. r bounds size))
        view-position (.. Camera main (WorldToScreenPoint local-position))
        border 2
        width 80
        height 6
        hit-points-percentage-width (* width
                                       (creature/hit-points-percentage this))
        x (- (.x view-position) (/ width 2))
        y (- (Screen/height) (.y view-position) (* 25 (.y size)))]
    (.. GUI (DrawTexture (Rect. (- x border)
                                (- y border)
                                (+ width (* 2 border))
                                (+ height (* 2 border)))
                         (:hit-points-bar-texture-border hit-points-bar-state)
                         (ScaleMode/StretchToFill)))
    (.. GUI (DrawTexture (Rect. x y width height)
                         (:hit-points-bar-texture-background hit-points-bar-state)
                         (ScaleMode/StretchToFill)))
    (.. GUI (DrawTexture (Rect. x y hit-points-percentage-width height)
                         (:hit-points-bar-texture hit-points-bar-state)
                         (ScaleMode/StretchToFill)))))

(def hooks
  {:start #'start!
   :on-gui #'on-gui!})
