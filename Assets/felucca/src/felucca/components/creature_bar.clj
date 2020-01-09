(ns felucca.components.creature-bar
  (:require [arcadia.core :refer [defmutable log update-state state log with-cmpt]]
            [felucca.vector3 :as v3]
            [felucca.components.creature :as creature]
            [felucca.components.hit-points-bar :as hit-points-bar]
            [felucca.utils :as utils])
  (:import [UnityEngine Debug Color Camera GUI Rect Screen Vector3 Texture2D
            Renderer ScaleMode TextureFormat]
           [UnityEngine.UI Text]
           [UnityEngine.EventSystems EventSystem PointerEventData]))

(defmutable CreatureBar [^int height
                         ^int width
                         ^int border
                         ^bool visible?
                         ^Texture2D creature-bar-texture-border
                         ^Texture2D creature-bar-texture])

(defn start! [^GameObject this role-key]
  (let [creature-bar-color (Color/black)
        creature-bar-border-color (Color/gray)
        creature-bar-texture (Texture2D. 1 1 (TextureFormat/RGB24) false)
        creature-bar-texture-border (Texture2D. 1 1 (TextureFormat/RGB24) false)]
    (.SetPixel creature-bar-texture 0 0 creature-bar-color)
    (.Apply creature-bar-texture)
    (.SetPixel creature-bar-texture-border 0 0 creature-bar-border-color)
    (.Apply creature-bar-texture-border)
    (update-state this
                  :creature-bar
                  (fn [creature-bar-state]
                    (assoc creature-bar-state
                           :creature-bar-texture-border
                           creature-bar-texture-border
                           :creature-bar-texture
                           creature-bar-texture)))))

(defn draw-window! [a]
  (GUI/DragWindow (Rect. 0 0 100 100))
  (log (EventSystem/current))
  (log "drawing window" a))

(defn on-gui! [^GameObject this role-key]
  (let [creature-state (state this :creature)
        {:keys [height width border] :as creature-bar-state}
        (state this :creature-bar)
        hit-points-bar-state (state this :hit-points-bar)
        local-position (.. this transform localPosition)
        size (with-cmpt this [r Renderer]
               (.. r bounds size))
        view-position (.. Camera main (WorldToScreenPoint local-position))
        x-padding 8
        y-padding 2
        x (- (.x view-position)
             (/ width 2))
        y (- (Screen/height)
             (.y view-position))
        text-x (+ x x-padding)
        text-y (+ y y-padding)
        hit-points-bar-y (+ text-y 25)
        hit-points-bar-height 5
        hit-points-bar-width (* 0.8 width)
        hit-points-bar-border 2
        hit-points-bar-x  (+ x
                             (/ (- width hit-points-bar-width)
                                2))]
    (GUI/Window 0 (Rect. 0 0 100 100) draw-window! "My window")

    (.. GUI (DrawTexture (Rect. (- x border)
                                (- y border)
                                (+ width (* 2 border))
                                (+ height (* 2 border)))
                         (:creature-bar-texture-border creature-bar-state)
                         (ScaleMode/StretchToFill)))
    (.. GUI (DrawTexture (Rect. x y width height)
                         (:creature-bar-texture creature-bar-state)
                         (ScaleMode/StretchToFill)))
    (.. GUI (Label (Rect. text-x text-y width height)
                   (:name creature-state)))
    (hit-points-bar/draw! this
                          hit-points-bar-x
                          hit-points-bar-y
                          hit-points-bar-height
                          hit-points-bar-width
                          hit-points-bar-border
                          (:hit-points-bar-texture-border hit-points-bar-state)
                          (:hit-points-bar-texture-background hit-points-bar-state)
                          (:hit-points-bar-texture hit-points-bar-state))))

(defn on-mouse-drag! [^GameObject this role-key]
  (log "mouse drag"))

(def hooks
  {:start #'start!
   :on-mouse-drag #'on-mouse-drag!
   :on-gui #'on-gui!})
