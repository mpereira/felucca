(ns src.battle-arena.components.player-input
  (:use arcadia.core)
  (:require [src.battle-arena.vector3 :refer :all]
            [src.battle-arena.utils :refer :all]
            [src.battle-arena.components.hero :as hero])
  (:import [UnityEngine
            Application
            Camera
            CharacterController
            Debug
            KeyCode
            Mathf
            Physics
            Ray
            RaycastHit
            Time]))

(defn start! [this])

(defn hit-points [creature]
  (.hit-points (.GetComponent creature hero/Component)))

(defn attack-speed [creature]
  (.attack-speed (.GetComponent creature hero/Component)))

(defn alive? [creature]
  (< 0 (hit-points creature)))

(defn receive-hit! [creature hit]
  (let [previous-hit-points (hit-points creature)]
    (set! (.hit-points (.GetComponent creature hero/Component))
          (float (- previous-hit-points hit)))))

(defn recovered-from-previous-hit? [this]
  (or (not (.last-hit-attempted-at this))
      (> (- Time/time (.last-hit-attempted-at this)) (attack-speed this))))

(defn hit! [this creature]
  (set! (.last-hit-attempted-at this) Time/time)
  (receive-hit! creature 5))

(defn attempt-hit! [this attackee]
  (when (and (alive? attackee) (recovered-from-previous-hit? this))
    (hit! this attackee)))

(defn set-target-position! [this position]
  (set! (.target-position this)
        (v3 (.x position) (.. this transform localPosition y) (.z position))))

(defn look-towards-position! [this position]
  (let [current-position (.. this transform localPosition)]
    (if (> (vdistance current-position position) 1)
      (let [current-rotation (.. this transform localRotation)
            target-rotation  (Quaternion/LookRotation (v* (v3 1 0 1)
                                                          (v- position
                                                              current-position)))]
        (set! (.. this transform localRotation)
              (Quaternion/RotateTowards current-rotation
                                        target-rotation
                                        (* (.. this
                                               (GetComponent hero/Component)
                                               rotation-speed)
                                           Time/deltaTime)))))))

(defn move-towards-position! [this position]
  (let [current-position (.. this transform localPosition)
        controller       (.. this (GetComponent CharacterController))]
    (if (> (vdistance current-position position) 1)
      (.SimpleMove controller (v* (.. this transform forward)
                                  (.. this
                                      (GetComponent hero/Component)
                                      movement-speed))))))

(defn handle-terrain-click! [this raycast-hit]
  (set! (.attacking-attackee? this) false)
  (set! (.moving-towards-target? this) true)
  (set-target-position! this (.point raycast-hit)))

(defn handle-enemy-click! [this raycast-hit]
  (set! (.attackee this) (.. raycast-hit transform gameObject))
  (set! (.attacking-attackee? this) true)
  (set! (.. this moving-towards-target?) false))

(defn update! [this]
  (when (Input/GetKeyDown KeyCode/Mouse1)
    (when-let [raycast-hit (target-hit)]
      (cond (terrain? raycast-hit) (handle-terrain-click! this raycast-hit)
            (enemy-hero? raycast-hit) (handle-enemy-click! this raycast-hit))))
  (when (.moving-towards-target? this)
    (look-towards-position! this (.target-position this))
    (move-towards-position! this (.target-position this)))
  (when (.attacking-attackee? this)
    (let [attackee-position (.. this attackee transform localPosition)]
      (look-towards-position! this attackee-position)
     (move-towards-position! this attackee-position)
     (when (> 1.2 (vdistance (.. this transform localPosition) attackee-position))
       (Debug/Log "attempting hit")
       (attempt-hit! this (.attackee this))))))

(defcomponent PlayerInput [^bool moving-towards-target?
                           ^Vector3 target-position
                           ^GameObject attackee
                           ^bool attacking-attackee?
                           ^float last-hit-attempted-at]
  (Start  [this] (start!  this))
  (Update [this] (update! this)))

;; FIXME: trying to `import` types provided by `defcomponent` result in:
;;
;; System.NullReferenceException: Object reference not set to an instance of an
;; object
(def Component PlayerInput)
