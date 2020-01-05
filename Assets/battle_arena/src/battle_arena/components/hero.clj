(ns battle-arena.components.hero
  (:use arcadia.core)
  (:require [battle-arena.vector3 :refer :all])
  (:import [UnityEngine Debug Color Camera GUI Rect Screen Vector3 Texture2D
            Renderer ScaleMode TextureFormat]))

(defn strength [game-object]
  (let [hero (state game-object :hero)]
    (:strength hero)))

(defn hit-points [game-object]
  (let [hero (state game-object :hero)]
    (:hit-points hero)))

(defn attack-speed [game-object]
  (let [hero (state game-object :hero)]
    (:attack-speed hero)))

(defn attack-range [game-object]
  (let [hero (state game-object :hero)]
    (:attack-range hero)))

(defn movement-speed [game-object]
  (let [hero (state game-object :hero)]
    (:movement-speed hero)))

(defn rotation-speed [game-object]
  (let [hero (state game-object :hero)]
    (:rotation-speed hero)))

(defn max-hit-points [game-object]
  (float (strength game-object)))

(defn hit-points-percentage [game-object]
  (/ (hit-points game-object) (max-hit-points game-object)))

(defn dead? [game-object]
  (>= 0 (hit-points game-object)))

(def alive? (complement dead?))

(defn decrement-hit-points! [game-object value]
  (update-state game-object
                :hero
                (fn [{:keys [hit-points] :as hero}]
                  (assoc hero :hit-points (- hit-points (max 0.0 value))))))

(defn increment-hit-points! [game-object value]
  (update-state game-object
                :hero
                (fn [{:keys [hit-points] :as hero}]
                  (assoc hero
                         :hit-points
                         (+ hit-points (min (max-hit-points game-object)
                                            value))))))

(defn receive-hit! [game-object hit]
  (decrement-hit-points! game-object hit))

(defn awake! [game-object role-key]
  (log "hero awake!" (.name game-object)))

(defn start! [game-object role-key]
  (log "hero start!" (.name game-object))
  (update-state game-object
                :hero
                (fn [{:keys [hit-points] :as hero}]
                  (assoc hero :hit-points (strength game-object)))))

(defn on-gui! [game-object role-key]
  (let [hero (state game-object :hero)
        transform (.transform game-object)
        local-position (.localPosition transform)
        size (with-cmpt game-object [r Renderer]
               (.. r bounds size))
        view-position (.. Camera main (WorldToScreenPoint local-position))
        border 2
        background-width 80
        width (* background-width (hit-points-percentage game-object))
        height 6
        x (- (.x view-position) (/ background-width 2))
        y (- (Screen/height) (.y view-position) (* 25 (.y size)))]
    (.. GUI (DrawTexture (Rect. (- x border)
                                (- y border)
                                (+ background-width (* 2 border))
                                (+ height (* 2 border)))
                         (:hit-points-bar-texture-background hero)
                         (ScaleMode/StretchToFill)))
    (.. GUI (DrawTexture (Rect. x y width height)
                         (:hit-points-bar-texture hero)
                         (ScaleMode/StretchToFill)))))

(defmutable Hero [^int strength
                  ^double hit-points
                  ^double attack-speed
                  ^double attack-range
                  ^double movement-speed
                  ^double rotation-speed
                  ^Texture2D hit-points-bar-texture-background
                  ^Texture2D hit-points-bar-texture])

(def hooks
  {:awake #'awake!
   :start #'start!
   :on-gui #'on-gui!})
